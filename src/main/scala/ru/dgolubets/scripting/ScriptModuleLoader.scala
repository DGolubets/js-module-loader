package ru.dgolubets.scripting

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Common script module loader interface.
 */
trait ScriptModuleLoader {
  /**
   * Loads a module.
   * @param moduleId Module absolute id
   * @return Future module wrapper
   */
  def require(moduleId: String): Future[ScriptModule]

  /**
   * Loads few modules.
   * @param moduleIds Absolute ids of the modules to load
   * @return
   */
  def require(moduleIds: Seq[String]): Future[Seq[ScriptModule]] = Future.sequence(moduleIds.map(m => require(m)))
}
