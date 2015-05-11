package ru.dgolubets.scripting.commonjs

import java.net.URI
import javax.script.{ScriptContext, ScriptEngine}

import jdk.nashorn.api.scripting.ScriptObjectMirror
import ru.dgolubets.scripting.commonjs.exceptions._
import ru.dgolubets.scripting.internal._
import ru.dgolubets.scripting.readers.ScriptModuleReader
import ru.dgolubets.scripting.{ScriptModule, ScriptModuleException, ScriptModuleSyncLoader}
import ru.dgolubets.util.{Logging, Resource}

import scala.util._

object CommonJsLoader
{
  /**
   * Creates an instance of CommonJs script loader on the engine.
   * @param engine Engine where a loader should be instantiated
   * @param moduleReader Script reader
   */
  def apply(engine: ScriptEngine, moduleReader: ScriptModuleReader) =
    new CommonJsLoader(engine, moduleReader)
}


/**
 * CommonJs script loader.
 * http://wiki.commonjs.org/wiki/Modules/1.1.1
 *
 * @param engine Engine where a loader should be instantiated
 * @param moduleReader Script reader
 */
class CommonJsLoader(engine: ScriptEngine, moduleReader: ScriptModuleReader)
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
   * Default bridge (and the only one in the current implementation)
   */
  private val defaultBridge = new CommonJsLoaderBridge(this, CommonJsLoaderContext(defaultResolutionContext))

  /**
   * Default script context (and the only one in the current implementation)
   */
  private lazy val defaultScriptContext = {
    val scriptContext = new LoaderScriptContext(
      engine.getBindings(ScriptContext.GLOBAL_SCOPE),
      engine.getBindings(ScriptContext.ENGINE_SCOPE),
      engine.createBindings())

    bindScriptContext(scriptContext, defaultBridge)

    scriptContext
  }

  /**
   * ScriptContext where require function is available.
   * @return
   */
  override def context: ScriptContext = defaultScriptContext

  /**
   * Loads a module.
   * @param moduleId Module absolute id
   * @return Future module wrapper
   */
  override def require(moduleId: String): Try[ScriptModule] = resolveModule(moduleId)(defaultResolutionContext).map(m => ScriptModule(m))

  /**
   * Binds a script context to a loader bridge.
   * @param bridge Loader bridge
   */
  private def bindScriptContext(scriptContext: LoaderScriptContext, bridge: CommonJsLoaderBridge): ScriptContext = {
    val moduleBindings = scriptContext.getBindings(LoaderScriptContext.Scopes.Module.id)

    val initScript = Resource.readString("/commonjs.js").get
    val init = engine.eval(initScript).asInstanceOf[ScriptObjectMirror]

    init.call(null, moduleBindings, bridge)
    scriptContext
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
    val exports = engine.eval("new Object()", defaultScriptContext)
    val instance = ModuleInstance(exports)

    // first initialization workflow step
    module.startInitializing(instance)

    // save current context and bindings
    val prevLoaderContext = defaultBridge.context
    val prevModuleBindings = context.getBindings(LoaderScriptContext.Scopes.Module.id)

    // set new context
    defaultBridge.context = new CommonJsLoaderContext(ResolutionContext(module :: resolutionContext.chain))

    // set new bindings
    val newModuleBindings = engine.createBindings()
    defaultScriptContext.setBindings(newModuleBindings, LoaderScriptContext.Scopes.Module.id)
    // have to bind again to write to new module bindings
    bindScriptContext(defaultScriptContext, defaultBridge)
    newModuleBindings.put("exports", exports)

    try {
      // wrap script in anonymous function to make a private variable scope
      // it doesn't help with explicit globals though (e.g. "global1 = 'global text'")
      // nashorn writes them directly to global object and it seems impossible to intercept it
      // todo: find a way to forbid explicit global variables
      val wrappedScript = s"(function(){ var exports = this.exports;\n ${moduleDefinition.script}\n })()"
      engine.eval(wrappedScript, context)
    }
    catch {
      case err: Exception =>
        log.error(s"Error evaluating module: ${module.id}", err)
        throw new ScriptModuleException(s"Error evaluating module: ${module.id}", err)
    }
    finally {
      // restore module bindings
      defaultScriptContext.setBindings(prevModuleBindings, LoaderScriptContext.Scopes.Module.id)
      defaultBridge.context = prevLoaderContext
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
   * Loads a module by id and relative to current module.
   * @param moduleId Module id
   * @param loaderContext Loader context
   * @return
   */
  private[commonjs] def requireLocal(moduleId: String)(implicit loaderContext: CommonJsLoaderContext): AnyRef = {
    implicit val resolutionContext = loaderContext.resolutionContext
    resolveModule(moduleId).get
  }
}
