package ru.dgolubets.scripting

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

/**
 * Asynchronous script module loader interface.
 */
trait ScriptModuleAsyncLoader extends ScriptModuleLoader {

  /**
   * Loads a module.
   * @param moduleId Module absolute id
   * @return Future module wrapper
   */
  def requireAsync(moduleId: String): Future[ScriptModule]

  /**
   * Loads few modules.
   * @param moduleIds Absolute ids of the modules to load
   * @return
   */
  def requireAsync(moduleIds: Seq[String]): Future[Seq[ScriptModule]] = Future.sequence(moduleIds.map(m => requireAsync(m)))
}
