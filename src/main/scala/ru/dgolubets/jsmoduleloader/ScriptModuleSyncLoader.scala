package ru.dgolubets.jsmoduleloader

import scala.util.Try

/**
 * Synchronous script module loader interface.
 */
trait ScriptModuleSyncLoader extends ScriptModuleLoader {

  /**
   * Loads a module.
   * @param moduleId Module absolute id
   * @return Future module wrapper
   */
  def require(moduleId: String): Try[ScriptModule]

  /**
   * Loads few modules.
   * @param moduleIds Absolute ids of the modules to load
   * @return
   */
  def require(moduleIds: Seq[String]): Try[Seq[ScriptModule]] = Try(moduleIds).flatMap(m => require(m))
}
