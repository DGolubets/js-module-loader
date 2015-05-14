package ru.dgolubets.jsmoduleloader.commonjs

import java.net.URI
import javax.script.{Bindings, ScriptContext, ScriptEngine}

import jdk.nashorn.api.scripting.{JSObject, NashornScriptEngine, NashornScriptEngineFactory}
import ru.dgolubets.jsmoduleloader.internal.ScriptEngineExtensions._
import ru.dgolubets.jsmoduleloader.internal.{Resource, _}
import ru.dgolubets.jsmoduleloader.readers.ScriptModuleReader
import ru.dgolubets.jsmoduleloader.{ScriptModule, ScriptModuleException, ScriptModuleSyncLoader}

import scala.util._

object CommonJsLoader
{
  /**
   * Creates an instance of CommonJs script loader on a new engine.
   * @param moduleReader Script reader
   */
  def apply(moduleReader: ScriptModuleReader) = {
    val factory = new NashornScriptEngineFactory
    val engine = factory.getScriptEngine.asInstanceOf[NashornScriptEngine]
    new CommonJsLoader(engine, moduleReader)
  }

  /**
   * Creates an instance of CommonJs script loader on the engine.
   * @param engine Engine where a loader should be instantiated
   * @param moduleReader Script reader
   */
  def apply(engine: NashornScriptEngine, moduleReader: ScriptModuleReader) =
    new CommonJsLoader(engine, moduleReader)
}


/**
 * CommonJs script loader.
 * http://wiki.commonjs.org/wiki/Modules/1.1.1
 *
 * @param scriptEngine Engine where a loader should be instantiated
 * @param moduleReader Script reader
 */
class CommonJsLoader(scriptEngine: NashornScriptEngine, moduleReader: ScriptModuleReader)
  extends ScriptModuleSyncLoader with Logging {

  /**
   * List of the modules.
   * Every module that was required explicitly or through dependency chain is added to the list.
   *
   * The key of a module is it's absolute module id.
   */
  private val modules = new ModuleList[CommonJsModuleDefinition]

  /**
   * Top level module resolution context
   */
  private val defaultResolutionContext = CommonJsResolutionContext()

  /**
   * Top level module loader context
   */
  private val defaultLoaderContext = CommonJsLoaderContext(defaultResolutionContext)

  // set require on the default engine context
  bind(engine.getBindings(ScriptContext.ENGINE_SCOPE), defaultLoaderContext)
  
  /**
   * ScriptEngine being used to load modules.
   * @return
   */
  override def engine: ScriptEngine = scriptEngine

  /**
   * Loads a module.
   * @param moduleId Module absolute id
   * @return Future module wrapper
   */
  override def require(moduleId: String): Try[ScriptModule] = resolveModule(moduleId)(defaultResolutionContext).map(m => ScriptModule(m))

  /**
   * Creates 'require' function on the bindings
   * which is bound to the specified loader context.
   *
   * @param bindings Script bindings
   * @param context Loader context
   */
  private def bind(bindings: Bindings, context: CommonJsLoaderContext): Unit = {
    val initScript = Resource.readString("/commonjs/bind.js").get
    val initFunction = engine.eval(initScript).asInstanceOf[JSObject]

    initFunction.call(null, bindings, new CommonJsLoaderBridge(this, context))
  }

  /**
   * Resolves relative module id.
   * @param moduleId Module id that can be relative to resolving module
   * @param context Resolution context
   * @return Absolute module id
   */
  private def resolveModuleId(moduleId: String)(implicit context: CommonJsResolutionContext): String = {
    val absoluteId = if (moduleId.startsWith(".")) {
      // A module identifier is "relative" if the first term is "." or ".."

      val currentURI = new URI(context.module.map(_.id).getOrElse(""))
      currentURI.resolve(moduleId).toString
    }
    else moduleId

    // remove extension
    if(absoluteId.endsWith(".js")) absoluteId.replace(".js", "") else absoluteId
  }

  /**
   * Loads the module script file.
   * @param moduleId Module id
   */
  private def loadModule(moduleId: String): CommonJsModuleDefinition = {
    log.debug(s"Loading module: $moduleId")
    val moduleUri = URI.create(moduleId)

    val definition = moduleReader.read(moduleUri) map { moduleScript =>
      CommonJsModuleDefinition(moduleId, Some(moduleUri), moduleScript)
    } recover {
      case err =>
        log.error(s"Error loading module: $moduleId", err)
        throw new ScriptModuleException(s"Error loading module: $moduleId", err)
    }

    definition.get
  }

  /**
   * Initializes module with supplied definition.
   *
   * @param module Module
   * @param moduleDefinition Module definition
   * @param resolutionContext Resolution context
   * @return
   */
  private def initializeModule(module: Module[CommonJsModuleDefinition], moduleDefinition: CommonJsModuleDefinition)( implicit resolutionContext: CommonJsResolutionContext): Unit = {
    val exports = scriptEngine.eval("new Object()")

    // first initialization workflow step
    module.startInitializing(ModuleInstance(exports))

    // prepare module bindings
    val moduleBindings = scriptEngine.createBindings()
    bind(moduleBindings, new CommonJsLoaderContext(CommonJsResolutionContext(module :: resolutionContext.chain)))

    // module should have exports variable
    moduleBindings.put("exports", exports)

    // module should have module variable
    val moduleObject = scriptEngine.createBindings()
    moduleObject.put("id", module.id)
    moduleObject.put("uri", moduleDefinition.uri.map(_.toString))
    // 'module.exports' seems to be de facto standard
    // it allows a module to return a single export by assigning to it
    moduleObject.put("exports", exports)

    moduleBindings.put("module", moduleObject)

    try {
      // wrap script in anonymous function to make a private variable scope
      // it doesn't help with explicit globals though (e.g. "global1 = 'global text'")
      // nashorn writes them directly to global object and it seems impossible to intercept it
      // todo: find a way to forbid explicit global variables
      scriptEngine.execute(moduleDefinition.script, moduleBindings)
    }
    catch {
      case err: Exception =>
        log.error(s"Error evaluating module: ${module.id}", err)
        throw new ScriptModuleException(s"Error evaluating module: ${module.id}", err)
    }

    // last initialization workflow step
    module.initialize(ModuleInstance(moduleObject.get("exports")))
  }

  /**
   * Resolves a module.
   * @param relativeId Module relative id
   * @return
   */
  private def resolveModule(relativeId: String)( implicit resolutionContext: CommonJsResolutionContext): Try[AnyRef] = Try {
    val moduleId = resolveModuleId(relativeId)
    log.debug(s"Resolving module: $moduleId")

    val module = modules(moduleId)

    try {
      module.lockIfState[Module.Empty] { _ =>
        module.startLoading()
        module.load(loadModule(moduleId))
      }

      module.lockIfState[Module.Loaded[CommonJsModuleDefinition]] { state =>
        initializeModule(module, state.definition)
      }
    }
    catch {
      case err: Throwable => module.fail(err)
    }

    if(resolutionContext.chain.contains(module)){
      // this is a circular dependency
      // thus return uninitialized module object
      log.debug(s"Circular dependency detected: ${module.id}")

      module.state match {
        case Module.Initializing(instance) => instance.value
        case Module.Error(err) => throw err
        case other => throw UnexpectedModuleState(other)
      }
    }
    else {
      module.state match {
        case Module.Initialized(instance) => instance.value
        case Module.Error(err) => throw err
        case other => throw UnexpectedModuleState(other)
      }
    }
  }

  /**
   * Loads a module by id relative to current module.
   * @param moduleId Module id
   * @param loaderContext Loader context
   * @return
   */
  private[jsmoduleloader] def requireLocal(moduleId: String)(implicit loaderContext: CommonJsLoaderContext): AnyRef = {
    implicit val resolutionContext = loaderContext.resolutionContext
    resolveModule(moduleId).get
  }
}
