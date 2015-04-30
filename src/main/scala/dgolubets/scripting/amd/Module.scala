package dgolubets.scripting.amd

import scala.concurrent._

/**
 * AMD module.
 * @param id Absolute module id
 * @param definition Module definition. It is added asynchronously during javascript code evaluation
 */
private case class Module(id: String, definition: Promise[ModuleDefinition]) {
  private object _lock
  private var _instance: Option[ModuleInstance] = None

  /**
   * Gets module instance.
   * @param context Resolution context that will be used during the module instantiation process.
   *                After that the value is ignored and cached result is returned.
   * @return Future module instance
   */
  def instance(implicit context: ResolutionContext, ec: ExecutionContext): Future[ModuleInstance] = definition.future.map { definition =>
    // module instance is cached once resolved
    // using double checked lock here for sync
    _instance.getOrElse {
      _lock.synchronized {
        _instance.getOrElse {
          _instance = Some(definition.factory(context))
          _instance.get
        }
      }
    }
  }
}
