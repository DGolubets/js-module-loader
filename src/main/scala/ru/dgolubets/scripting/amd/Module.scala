package ru.dgolubets.scripting.amd

import scala.concurrent._

/**
 * AMD module.
 * @param id Absolute module id
 * @param definition Module definition. It is added asynchronously during javascript code evaluation
 */
private case class Module(id: String, definition: Promise[ModuleDefinition]) {
  private object _lock
  private val _instancePromise = Promise[ModuleInstance]()

  /**
   * Gets module instance.
   * @param context Resolution context that will be used during the module instantiation process.
   *                After that the value is ignored and cached result is returned.
   * @return Future module instance
   */
  def instance(implicit context: ResolutionContext, ec: ExecutionContext): Future[ModuleInstance] = {
    if (!_instancePromise.isCompleted) {
      // nevertheless of Promise use
      // we need lock here to prevent more than one factory call
      _lock.synchronized {
        if (!_instancePromise.isCompleted) {
          _instancePromise.completeWith(definition.future.map(_.factory(context)))
        }
      }
    }
    _instancePromise.future
  }
}
