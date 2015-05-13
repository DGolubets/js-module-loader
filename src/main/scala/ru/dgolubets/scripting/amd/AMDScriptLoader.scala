package ru.dgolubets.scripting.amd

import java.net.URI
import java.util.concurrent.Executors
import javax.script.{Bindings, ScriptContext, ScriptEngine}

import jdk.nashorn.api.scripting.{NashornScriptEngineFactory, JSObject, NashornScriptEngine}
import ru.dgolubets.scripting._
import ru.dgolubets.scripting.internal.ScriptEngineExtensions._
import ru.dgolubets.scripting.readers.ScriptModuleReader
import ru.dgolubets.internal.util.{Logging, Resource}

import scala.beans.BeanProperty
import scala.concurrent._
import scala.util.Success

object AMDScriptLoader {
  /**
   * Creates an instance of AMD script loader on a new engine.
   * @param moduleReader Script reader
   */
  def apply(moduleReader: ScriptModuleReader) = {
    val factory = new NashornScriptEngineFactory
    val engine = factory.getScriptEngine.asInstanceOf[NashornScriptEngine]
    new AMDScriptLoader(engine, moduleReader)
  }

  /**
   * Creates an instance of AMD script loader on the engine.
   * @param engine Engine where a loader should be instantiated
   * @param moduleReader Script reader
   */
  def apply(engine: NashornScriptEngine, moduleReader: ScriptModuleReader) =
    new AMDScriptLoader(engine, moduleReader)
}

/**
 * AMD script loader.
 * https://github.com/amdjs/amdjs-api/blob/master/AMD.md
 *
 * @param scriptEngine Engine where a loader should be instantiated
 * @param moduleReader Script reader
 */
class AMDScriptLoader(scriptEngine: NashornScriptEngine, moduleReader: ScriptModuleReader)
  extends ScriptModuleAsyncLoader with Logging {

  /**
   * Execution context for module loading.
   *
   * ScriptEngine is NOT thread safe!
   * Therefore all asynchronous operations that work with ScriptEngine should be executed sequentially.
   * That's why SingleThreadExecutor is used here.
   */
  private[amd] implicit val executionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

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
   * Default resolution context.
   * Every explicit module resolution starts with it.
   * It's also prevents creation of unnecessary objects in memory
   * when module is already resolved and there is no real need of a resolution context.
   */
  private val defaultResolutionContext = ResolutionContext()

  /**
   * Default (top-level) loader context.
   */
  private val defaultLoaderContext = LoaderContext("", new URI(""), engine.getBindings(ScriptContext.ENGINE_SCOPE))

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
   * Binds a loader context and a script bindings.
   * I.e. creates require function there.
   *
   * @param bindings Script bindings
   * @param context Loader context
   */
  private def bind(bindings: Bindings, context: LoaderContext): Unit = {
    val initScript = Resource.readString("/amd/bind.js").get
    val initFunction = engine.eval(initScript).asInstanceOf[JSObject]

    initFunction.call(null, bindings, new LoaderBridge(this, context))
  }


  /**
   * Resolves relative module id.
   * @param moduleId Module id that can be relative to resolving module
   * @param context Resolution context
   * @return Absolute module id
   */
  private def resolveModuleId(moduleId: String)(implicit context: ResolutionContext): String = {
    if(moduleId.startsWith(".")){
      // A module identifier is "relative" if the first term is "." or ".."

      val currentURI = new URI(context.module.map(_.id).getOrElse(""))
      currentURI.resolve(moduleId).toString
    }
    else moduleId
  }


  /**
   * Loads the module script file.
   * @param module Module
   */
  private def loadModuleAsync(module: Module): Future[Unit] = {
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
      val loaderContext = new LoaderContext(module.id, moduleUri, moduleBindings)
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
      for(definition <- loaderContext.definitions){
        val definedModule = ensureModule(definition.id)
        if(!definedModule.definition.trySuccess(definition)){
          log.warn(s"Module '${definedModule.id}' has already been defined.")
        }
      }

      // check if the primary module has been defined
      // it should have been to the moment
      if(!module.definition.isCompleted){
        throw ScriptModuleException(s"Module definition has not been found: ${module.id}")
      }
    }

    // if load has failed - mark the primary module definition failed too
    loadOperation onFailure {
      case err =>
        log.error(s"Could not load module: ${module.id}", err)
        module.definition.tryFailure(ScriptModuleException(s"Could not load module: ${module.id}", err))
    }

    loadOperation
  }

  /**
   * Gets or adds an empty module.
   * @param id Absolute module id
   * @return
   */
  private def ensureModule(id: String) = _modulesLock.synchronized {
    // synchronization is required cos it can be called in futures from different threads
    modules.getOrElse(id, {
      val module = Module(id, Promise())
      modules += (id -> module)
      module
    })
  }

  /**
   * Resolves a module.
   * @param relativeId Module relative id
   * @return
   */
  private def resolveModule(relativeId: String)( implicit resolutionContext: ResolutionContext): Future[AnyRef] = {
    val moduleId = resolveModuleId(relativeId)
    log.debug(s"Resolving module: $moduleId")

    // if module doesn't yet exists
    val module = _modulesLock.synchronized {
      modules.getOrElse(moduleId, {
        // create it
        val newModule = ensureModule(moduleId)
        // start the loading process
        loadModuleAsync(newModule)
        newModule
      })
    }

    if(resolutionContext.chain.contains(module)){
      // this is a circular dependency
      // thus return uninitialized module object
      log.debug(s"Circular dependency detected: ${module.id}")
      module.instance.map(_.value)
    }
    else {
      // otherwise return future initialized value
      module.instance.flatMap(_.initialized).map(_.value)
    }
  }

  /**
   * Resolves a dependency.
   *
   * @param dependency Module dependency to resolve
   * @param resolutionContext Resolution context
   * @param loaderContext Loader context
   * @return Future value
   */
  private def resolveDependency(dependency: String)(implicit resolutionContext: ResolutionContext, loaderContext: LoaderContext): Future[AnyRef] = {
    // at least one module should be here
    val module = resolutionContext.module.get

    // there are few special names according to the AMD spec
    dependency match {
      case "exports" =>
        module.instance.map(_.value) // uninitialized module object
      case "module" =>
        case class ModuleInfo(@BeanProperty id: String, @BeanProperty uri: String)
        module.definition.future.map(definition => new ModuleInfo(module.id, definition.uri.toString))
      case "require" =>
        Future.successful(loaderContext.bindings.get("require"))
      case _ =>
        resolveModule(dependency)
    }
  }

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
   * Synchronously loads a module by id and relative to current module.
   * @param moduleId Module id
   * @param loaderContext Loader context
   * @return
   */
  private[amd] def requireLocal(moduleId: String)(implicit loaderContext: LoaderContext): AnyRef = {
    implicit val resolutionContext = ResolutionContext(modules.get(loaderContext.moduleId).toList)
    val absoluteModuleId = resolveModuleId(moduleId)

    // return initialized module value or none
    modules.get(absoluteModuleId).flatMap(module =>
      module.instance.value match {
      case Some(Success(instance)) =>
        Some(instance.value)
      case _ =>
        None
    }).getOrElse(Undefined)
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
  private[amd] def define(moduleId: Option[String], dependencies: Seq[String], moduleFactory: JSObject)(implicit loaderContext: LoaderContext): Unit = {
    // ensure the module using id from the loader when omitted
    val module = ensureModule(moduleId.getOrElse(loaderContext.moduleId))

    log.debug(s"Defined module: ${module.id}")

    // create module definition
    val definition = ModuleDefinition(
      module.id,
      loaderContext.file, // take URI from the loader context
      { context =>

        if(moduleFactory.isFunction){
          // when moduleFactory is a function - use it to construct a module

          // create new resolution context for this module
          implicit val newResolutionContext = new ResolutionContext(module :: context.chain)

          /*
          Create uninitialized module instance.
          It is used in case of circular dependencies.

          When module uses 'exports', we should create an empty object which will be extended by the module.
          This object can also be passed as a dependency to other modules before this module finishes.

          Otherwise just use undefined.
          */

          // if dependencies are omited - use default dependency list (as in AMD spec)
          val dependenciesOrDefault = if(dependencies.nonEmpty) dependencies else Seq("require", "exports", "module")

          val instanceRef =
            if(dependenciesOrDefault.contains("exports"))
              scriptEngine.eval("new Object()") // {}
            else
              Undefined

          val instanceReady = Promise[AnyRef]()
          val instance = ModuleInstance(instanceRef, instanceReady.future)

          val result = Future.sequence(dependenciesOrDefault.map(d => resolveDependency(d))).map { deps =>
            moduleFactory.call(null, deps: _*) match {
              case Undefined =>
                // when factory returned undefined - then it's likely it uses 'exports'
                instance.value
              case other =>
                other
            }
          }

          // mark instance as ready when result is created
          instanceReady.completeWith(result)
          instance
        }
        else {
          // if moduleFactory is not a function - just return whatever it is
          ModuleInstance(moduleFactory, Promise.successful(moduleFactory).future)
        }
      }
    )

    // do not publish module definition yet
    // only add definition to the context
    loaderContext.definitions += definition
  }
}
