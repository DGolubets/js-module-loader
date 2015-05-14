package ru.dgolubets.jsmoduleloader

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
}
