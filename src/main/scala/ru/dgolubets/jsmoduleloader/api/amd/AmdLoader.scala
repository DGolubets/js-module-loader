package ru.dgolubets.jsmoduleloader.api.amd

import java.net.URI
import java.util.concurrent.Executors
import javax.script.{Bindings, ScriptContext, ScriptEngine}

import jdk.nashorn.api.scripting.{JSObject, NashornScriptEngine, NashornScriptEngineFactory}
import ru.dgolubets.jsmoduleloader.api.readers.ScriptModuleReader
import ru.dgolubets.jsmoduleloader.api.{AsyncScriptModuleLoader, ScriptModule, ScriptModuleException}
import ru.dgolubets.jsmoduleloader.internal.ScriptEngineExtensions._
import ru.dgolubets.jsmoduleloader.internal.{Resource, _}

import scala.concurrent._

object AmdLoader {
  /**
   * Creates an instance of AMD script loader on a new engine.
   * @param moduleReader Script reader
   */
  def apply(moduleReader: ScriptModuleReader) = {
    val factory = new NashornScriptEngineFactory
    val engine = factory.getScriptEngine.asInstanceOf[NashornScriptEngine]

    // set engine polyfill when it's created by the loader
    engine.eval(Resource.readString("/polyfill.js").get)

    new AmdLoader(engine, moduleReader)
  }

  /**
   * Creates an instance of AMD script loader on the engine.
   * @param engine Engine where a loader should be instantiated
   * @param moduleReader Script reader
   */
  def apply(engine: NashornScriptEngine, moduleReader: ScriptModuleReader) =
    new AmdLoader(engine, moduleReader)
}

/**
 * AMD script loader.
 * https://github.com/amdjs/amdjs-api/blob/master/AMD.md
 *
 * @param scriptEngine Engine where a loader should be instantiated
 * @param moduleReader Script reader
 */
class AmdLoader(scriptEngine: NashornScriptEngine, moduleReader: ScriptModuleReader)
  extends AsyncScriptModuleLoader with Logging {

  /**
   * Execution context for module loading.
   *
   * ScriptEngine is NOT thread safe!
   * Therefore all asynchronous operations that work with ScriptEngine should be executed sequentially.
   * That's why SingleThreadExecutor is used here.
   *
   * Loader should not block JVM from exiting. So the thread should be set to daemon.
   */
  private[jsmoduleloader] implicit val executionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor(new DaemonThreadFactory()))

  /**
   * List of the modules.
   * Every module that was required explicitly or through dependency chain is added to the list.
   *
   * The key of a module is it's absolute module id.
   */
  private val modules = new ModuleList[AmdModuleDefinition]

  /**
   * Default resolution context.
   * Every explicit module resolution starts with it.
   * It's also prevents creation of unnecessary objects in memory
   * when module is already resolved and there is no real need of a resolution context.
   */
  private val defaultResolutionContext = AmdResolutionContext()

  /**
   * Default (top-level) loader context.
   */
  private val defaultLoaderContext = AmdLoaderContext("", None, engine.getBindings(ScriptContext.ENGINE_SCOPE))

  /**
   * Undefined value in JavaScript
   */
  private lazy val Undefined = {
    val global = scriptEngine.eval("this").asInstanceOf[JSObject]
    global.getMember("undefined")
  }

  // set define and require on the engine
  bind(engine.getBindings(ScriptContext.ENGINE_SCOPE), defaultLoaderContext)

  /**
   * Loads a module by id.
   * @param moduleId Module absolute id
   * @return
   */
  override def requireAsync(moduleId: String): Future[ScriptModule] = {
    resolveModule(moduleId)(defaultResolutionContext).map(value => ScriptModule(value))
  }

  /**
   * ScriptEngine being used to load modules.
   * @return
   */
  override def engine: ScriptEngine = scriptEngine

  /**
   * Binds a loader context and a script bindings.
   * I.e. creates require function there.
   *
   * @param bindings Script bindings
   * @param context Loader context
   */
  private def bind(bindings: Bindings, context: AmdLoaderContext): Unit = {
    val initScript = Resource.readString("/amd/bind.js").get
    val initFunction = engine.eval(initScript).asInstanceOf[JSObject]

    initFunction.call(null, bindings, new AmdLoaderBridge(this, context))
  }

  /**
   * Resolves relative module id.
   * @param moduleId Module id that can be relative to resolving module
   * @param context Resolution context
   * @return Absolute module id
   */
  private def resolveModuleId(moduleId: String)(implicit context: AmdResolutionContext): String = {
    if(moduleId.startsWith(".")){
      // A module identifier is "relative" if the first term is "." or ".."

      val currentURI = new URI(context.module.map(_.id).getOrElse(""))
      currentURI.resolve(moduleId).toString
    }
    else moduleId
  }

  /**
   * Synchronously loads a module by id and relative to current module.
   * @param moduleId Module id
   * @param loaderContext Loader context
   * @return
   */
  private[jsmoduleloader] def requireLocal(moduleId: String)(implicit loaderContext: AmdLoaderContext): AnyRef = {
    implicit val resolutionContext = AmdResolutionContext(modules.get(loaderContext.moduleId).toList)
    val absoluteModuleId = resolveModuleId(moduleId)

    // return initialized module value or none
    modules.get(absoluteModuleId).map(module =>
      module.state match {
        case Module.Initialized(instance) => instance.value
        case other => Undefined
      }).getOrElse(Undefined)
  }

  /**
   * Resolves a module.
   * @param relativeId Module relative id
   * @return
   */
  private def resolveModule(relativeId: String)(implicit resolutionContext: AmdResolutionContext): Future[AnyRef] = {
    val moduleId = resolveModuleId(relativeId)
    log.debug(s"Resolving module: $moduleId")

    // if module doesn't yet exists
    val module = modules(moduleId)

    module.lockIfState[Module.Empty]{ _ =>
      module.startLoading()
      loadModuleAsync(module)
    }

    module.loaded.map { _ =>
      module.lockIfState[Module.Loaded[AmdModuleDefinition]] { state =>
        initializeModule(module, state.definition)(resolutionContext)
      }
    }

    if(resolutionContext.chain.contains(module)){
      // this is a circular dependency
      // thus return uninitialized module object
      log.debug(s"Circular dependency detected: ${module.id}")

      module.initializing.map { _ =>
        module.state match {
          case Module.Initializing(instance) => instance.value
          case Module.Error(err) => err
          case other => throw UnexpectedModuleState(other)
        }
      }
    }
    else {
      module.initialized.map { _ =>
        module.state match {
          case Module.Initialized(instance) => instance.value
          case Module.Error(err) => err
          case other => throw UnexpectedModuleState(other)
        }
      }
    }
  }

  /**
   * Defines an AMD module.
   *
   * @param moduleId Optional absolute module id
   *                 Determined from the file name when omitted
   * @param dependencies A list of the module dependencies
   * @param moduleFactory JavaScript module factory function or an object, see AMD spec
   * @param loaderContext Loader context
   * @return
   */
  private[jsmoduleloader] def define(moduleId: Option[String], dependencies: Seq[String], moduleFactory: JSObject)(implicit loaderContext: AmdLoaderContext): Unit = {
    // ensure the module using id from the loader when omitted
    val module = modules(moduleId.getOrElse(loaderContext.moduleId))

    log.debug(s"Defined module: ${module.id}")

    loaderContext.definitions += AmdModuleDefinition(module.id, loaderContext.file, dependencies, moduleFactory, loaderContext.bindings)
  }

  /**
   * Loads the module script file.
   * @param module Module
   */
  private def loadModuleAsync(module: Module[AmdModuleDefinition]): Unit = {
    log.debug(s"Loading module: ${module.id}")

    val moduleUri = new URI(module.id)
    val loadOperation = moduleReader.readAsync(moduleUri) map { moduleScript =>
      /*
        Modules should be able to see global and engine variables.
        On the other hand a separate loader context is required for each module file, that should be in a private module scope.
        For that reason there is a special ScriptContext subclass with an additional scope.
       */

      // prepare module private bindings
      val moduleBindings = scriptEngine.createBindings()
      val loaderContext = new AmdLoaderContext(module.id, Some(moduleUri), moduleBindings)
      bind(moduleBindings, loaderContext)

      // next - evaluate module script in the script context
      // on error - finish module definition promise with failure
      try {
        scriptEngine.execute(moduleScript, moduleBindings)
      }
      catch {
        case err: Exception =>
          log.error(s"Error evaluating module: ${module.id}", err)
          throw ScriptModuleException(s"Error evaluating module: ${module.id}", cause = err)
      }

      // publish collected module definitions
      var currentModuleDefinition: Option[AmdModuleDefinition] = None

      for(definition <- loaderContext.definitions){
        val definedModule = modules(definition.id)

        if(definedModule == module){
          currentModuleDefinition = Some(definition)
          definedModule.load(definition)
        }
        else {
          definedModule.lockIfState[Module.Empty]{ _ =>
            definedModule.startLoading()
            definedModule.load(definition)
          }
        }
      }

      // check if the primary module has been defined
      // it should have been to the moment
      if(currentModuleDefinition.isEmpty){
        throw ScriptModuleException(s"Module definition has not been found: ${module.id}")
      }
    }

    // if load has failed - mark the primary module definition failed too
    loadOperation onFailure {
      case err: Throwable =>
        log.error(s"Could not load module: ${module.id}", err)
        module.fail(ScriptModuleException(s"Could not load module: ${module.id}", err))
    }
  }

  private def initializeModule(module: Module[AmdModuleDefinition], moduleDefinition: AmdModuleDefinition)
                              (resolutionContext: AmdResolutionContext): Unit = {
    // if dependencies are omited - use default dependency list (as in AMD spec)
    val dependenciesOrDefault =
      if(moduleDefinition.dependencies.nonEmpty) moduleDefinition.dependencies
      else Seq("require", "exports", "module")

    if(moduleDefinition.moduleFactory.isFunction){
      // when moduleFactory is a function - use it to construct a module

      // create new resolution context for this module
      implicit val newResolutionContext = new AmdResolutionContext(module :: resolutionContext.chain)

      /*
      Create uninitialized module instance.
      It is used in case of circular dependencies.

      When module uses 'exports', we should create an empty object which will be extended by the module.
      This object can also be passed as a dependency to other modules before this module finishes.

      Otherwise just use undefined.
      */
      val initialValue =
        if(dependenciesOrDefault.contains("exports"))
          scriptEngine.eval("new Object()") // {}
        else
          Undefined

      module.startInitializing(ModuleInstance(initialValue))

      def resolveDependency(dependency: String): Future[AnyRef] = {
        // there are few special names according to the AMD spec
        dependency match {
          case "exports" =>
            Future.successful(initialValue)
          case "module" =>
            val moduleObject = engine.createBindings()
            moduleObject.put("id", module.id)
            moduleDefinition.uri.map { uri =>
              moduleObject.put("uri", uri.toString)
            }
            Future.successful(moduleObject)
          case "require" =>
            Future.successful(moduleDefinition.moduleBindings.get("require"))
          case _ =>
            resolveModule(dependency)
        }
      }

      val result = Future.sequence(dependenciesOrDefault.map(d => resolveDependency(d))).map { deps =>
        moduleDefinition.moduleFactory.call(null, deps: _*) match {
          case Undefined =>
            // when factory returned undefined - then it's likely it uses 'exports'
            initialValue
          case other =>
            other
        }
      }

      result.map { value =>
        module.initialize(ModuleInstance(value))
      } recover {
        case err =>
          module.fail(err)
      }
    }
    else {
      // if moduleFactory is not a function - just return whatever it is
      module.startInitializing(ModuleInstance(moduleDefinition.moduleFactory))
      module.initialize(ModuleInstance(moduleDefinition.moduleFactory))
    }
  }

}
