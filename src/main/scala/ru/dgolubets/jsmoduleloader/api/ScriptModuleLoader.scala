package ru.dgolubets.jsmoduleloader.api

import javax.script.ScriptEngine

/**
 * Common script module loader interface.
 */
trait ScriptModuleLoader {

  /**
   * ScriptEngine being used to load modules.
   * @return
   */
  def engine: ScriptEngine

  /**
   * Executes a code block that uses the script engine, synchronized with the loader.
   * Every action against the ScriptEngine or produced native script objects needs to be synchronized.
   *
   * @param code Synchronized code
   * @tparam T Type of the code block expression
   */
  def lock[T](code: => T) = engine.synchronized {
    code
  }
}
