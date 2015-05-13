package ru.dgolubets.scripting.commonjs

import java.net.URI
import javax.script.{Bindings, ScriptContext, ScriptEngine}

import jdk.nashorn.api.scripting.{JSObject, NashornScriptEngine, NashornScriptEngineFactory}
import ru.dgolubets.scripting.commonjs.exceptions._
import ru.dgolubets.scripting.internal.ScriptEngineExtensions._
import ru.dgolubets.scripting.readers.ScriptModuleReader
import ru.dgolubets.scripting.{ScriptModule, ScriptModuleException, ScriptModuleSyncLoader}
import ru.dgolubets.internal.util.{Logging, Resource}

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
   * Synchronization object for modules list
   */
  private object _modulesLock

  /**
   * List of the modules.
   * Every module that was required explicitly or through dependency chain is added to the list.
   *
   * The key of a module is it's absolute module id.
   */
  private var modules = Map[String, Module]()

  /**
   * Top level module resolution context
   */
  private val defaultResolutionContext = ResolutionContext()

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
  private def resolveModuleId(moduleId: String)(implicit context: ResolutionContext): String = {
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
   * Gets or adds an empty module.
   * @param id Absolute module id
   * @return
   */
  private def ensureModule(id: String) = _modulesLock.synchronized {
    // synchronization is required cos it can be called in futures from different threads
    modules.getOrElse(id, {
      val module = Module(id)
      modules += (id -> module)
      module
    })
  }

  /**
   * Loads the module script file.
   * @param moduleId Module id
   */
  private def loadModule(moduleId: String): ModuleDefinition = {
    log.debug(s"Loading module: $moduleId")
    val moduleUri = URI.create(moduleId)

    val definition = moduleReader.read(moduleUri) map { moduleScript =>
      ModuleDefinition(moduleId, moduleUri, moduleScript)
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
  private def initializeModule(module: Module, moduleDefinition: ModuleDefinition)( implicit resolutionContext: ResolutionContext): Unit = {
    val exports = scriptEngine.eval("new Object()")
    val instance = ModuleInstance(exports)

    // first initialization workflow step
    module.startInitializing(instance)

    // prepare module bindings
    val moduleBindings = scriptEngine.createBindings()
    bind(moduleBindings, new CommonJsLoaderContext(ResolutionContext(module :: resolutionContext.chain)))
    moduleBindings.put("exports", exports)

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
    module.initialize(instance)
  }

  /**
   * Resolves a module.
   * @param relativeId Module relative id
   * @return
   */
  private def resolveModule(relativeId: String)( implicit resolutionContext: ResolutionContext): Try[AnyRef] = Try {
    val moduleId = resolveModuleId(relativeId)
    log.debug(s"Resolving module: $moduleId")

    val module = ensureModule(moduleId)

    module.lockInState[Module.Empty]{ _ =>
      module.startLoading()
      module.load(loadModule(moduleId))
    }

    module.lockInState[Module.Loaded]{ state =>
      initializeModule(module, state.definition)
    }

    if(resolutionContext.chain.contains(module)){
      // this is a circular dependency
      // thus return uninitialized module object
      log.debug(s"Circular dependency detected: ${module.id}")

      module.state match {
        case state: Module.Initializing => state.instance.value
        case other => throw UnexpectedModuleState(other)
      }
    }
    else {
      module.state match {
        case state: Module.Initialized => state.instance.value
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
  private[commonjs] def requireLocal(moduleId: String)(implicit loaderContext: CommonJsLoaderContext): AnyRef = {
    implicit val resolutionContext = loaderContext.resolutionContext
    resolveModule(moduleId).get
  }
}
