package ru.dgolubets.scripting

import javax.script.ScriptContext

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Common script module loader interface.
 */
trait ScriptModuleLoader {

  /**
   * ScriptContext where require function is available.
   * @return
   */
  def context: ScriptContext
}
