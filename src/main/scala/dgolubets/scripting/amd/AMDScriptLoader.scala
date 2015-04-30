package dgolubets.scripting.amd

import java.io.{BufferedReader, File, FileReader, InputStreamReader}
import java.net.URI
import javax.script.{ScriptEngineManager, SimpleScriptContext, ScriptContext, ScriptEngine}

import dgolubets.Logging
import dgolubets.scripting._
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.beans.BeanProperty
import scala.concurrent._
import scala.concurrent.duration._

object AMDScriptLoader {
  /**
   * Creates an instance of AMD script loader on the engine.
   * @param engine Engine where a loader should be instantiated
   * @param baseDir Path to a base directory of all script modules
   * @param ec Execution context
   */
  def apply(engine: ScriptEngine, baseDir: File, ec: ExecutionContext = ExecutionContext.Implicits.global) =
    new AMDScriptLoader(engine, baseDir, ec)

  /**
   * Creates new script engine and an instance of AMD script loader.
   * @param baseDir Path to a base directory of all script modules
   */
  def apply(baseDir: File): AMDScriptLoader = {
    val engineManager = new ScriptEngineManager(null)
    val engine = engineManager.getEngineByName("nashorn")
    AMDScriptLoader(engine, baseDir)
  }
}

/**
 * AMD script loader.
 * https://github.com/amdjs/amdjs-api/blob/master/AMD.md
 *
 * @param engine Engine where a loader should be instantiated
 * @param baseDir Path to a base directory of all script modules
 */
class AMDScriptLoader(engine: ScriptEngine, baseDir: File, ec: ExecutionContext)
  extends ScriptModuleLoader with Logging {

  /**
   * Execution context for module loading operations.
   */
  protected[amd] implicit val executionContext = ec

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
   * Default loader context that is used for modules defined without their own file (i.e. via engine.eval).
   */
  private val defaultLoaderContext = LoaderContext("", new URI(""), engine.getContext)

  // set define and require on the engine
  expose(defaultLoaderContext)

  /**
   * Gets a file for specified module id.
   *
   * @param moduleId Absolute module id
   * @return Some(File) or None if file doesn't exist
   */
  private def getModuleFile(moduleId: String): Option[File] = {
    // try both files without and with .js extension
    val fileList = List(
      new File(baseDir, moduleId),
      new File(baseDir, moduleId + ".js"))

    // return only real existent files, not directories
    fileList.find(_.isFile).map(_.getCanonicalFile)
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
   * Puts require and define functions on the script context specified in the loader context.
   * @param loaderContext Loader context
   */
  private def expose(loaderContext: LoaderContext): Unit = {
    val scriptContext = loaderContext.scriptContext
    val localBindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE)

    // to prevent js namespace pollution, expose loader as 'require'
    // then the bridge.js script will take the value and replace it with a function
    localBindings.put("require", new LoaderBridge(this, loaderContext))

    val bridgeJsReader = new BufferedReader(new InputStreamReader(this.getClass.getResourceAsStream("/bridge.js")))
    try {
      engine.eval(bridgeJsReader, scriptContext)
    }
    finally{
      bridgeJsReader.close()
    }
  }

  /**
   * Loads the module
   * @param module Module to load
   */
  private def loadModuleFile(module: Module) = {
    val moduleFile = getModuleFile(module.id)
    log.debug(s"Loading module file: $moduleFile")

    if(moduleFile.isDefined){
      /*
        Modules should be able to see global variables defined on the engine.
        On the other hand a separate loader context is required for each module file, that should be in a private module scope.

        To achieve it without manual bindings processing I use new ScriptContext where I put engine bindings in GLOBAL_SCOPE
        and new bindings in ENGINE_SCOPE.
       */
      val moduleScriptContext = new SimpleScriptContext()
      moduleScriptContext.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE)
      moduleScriptContext.setBindings(engine.getBindings(ScriptContext.ENGINE_SCOPE), ScriptContext.GLOBAL_SCOPE)

      // construct new loader context with the script context and expose it to module
      expose(LoaderContext(module.id, moduleFile.get.toURI, moduleScriptContext))

      // next - read and evaluate module file in the script context
      // on error - finish module definition promise with failure
      try {
        val moduleReader = new FileReader(moduleFile.get)
        try {
          engine.eval(moduleReader, moduleScriptContext)
        }
        finally{
          moduleReader.close()
        }
      }
      catch {
        case err: Exception => module.definition.tryFailure(ScriptModuleException(cause = err))
      }
    }
    else {
      module.definition.tryFailure(ScriptModuleException(s"Couldn't find module file."))
    }
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
        loadModuleFile(newModule)
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
        Future.successful(engine.eval("require", loaderContext.scriptContext))
      case _ =>
        resolveModule(dependency)
    }
  }

  /**
   * Loads a module by id.
   * @param moduleId Module absolute id
   * @return
   */
  override def require(moduleId: String): Future[ScriptModule] = {
    resolveModule(moduleId)(defaultResolutionContext).map(value => new ScriptModule(value))
  }

  /**
   * Synchronously loads a module by id and relative to current module.
   * @param moduleId Module id
   * @param loaderContext Loader context
   * @return
   */
  private[amd] def requireLocal(moduleId: String)(implicit loaderContext: LoaderContext): ScriptModule = {
    val resolutionContext = ResolutionContext(modules.get(loaderContext.moduleId).toList)
    val module = resolveModule(moduleId)(resolutionContext).map(value => new ScriptModule(value))
    Await.result(module, 30.seconds)
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
  private[amd] def define(moduleId: Option[String], dependencies: Seq[String], moduleFactory: ScriptObjectMirror)(implicit loaderContext: LoaderContext): Unit = {
    // ensure the module using id from the loader when omitted
    val module = ensureModule(moduleId.getOrElse(loaderContext.moduleId))

    log.debug(s"Defined module: ${module.id}")

    // create module definition
    val definition = ModuleDefinition(
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
              engine.eval("new Object()") // {}
            else
              null

          val instanceReady = Promise[AnyRef]()
          val instance = ModuleInstance(instanceRef, instanceReady.future)

          val result = Future.sequence(dependenciesOrDefault.map(d => resolveDependency(d))).map { deps =>
            moduleFactory.call(null, deps: _*) match {
              case _: jdk.nashorn.internal.runtime.Undefined =>
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
    if(!module.definition.trySuccess(definition)){
      log.warn(s"Module '$moduleId' has already been defined.")
    }
  }
}
