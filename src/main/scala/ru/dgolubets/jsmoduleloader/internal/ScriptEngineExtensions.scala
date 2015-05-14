package ru.dgolubets.jsmoduleloader.internal

import javax.script.{Bindings, ScriptEngine}

import jdk.nashorn.api.scripting.JSObject

import scala.collection.JavaConversions._

/**
 * Created by Dima on 12.05.2015.
 */
private[jsmoduleloader] object ScriptEngineExtensions {

  implicit class ScriptEngineOps(val engine: ScriptEngine) extends AnyVal {

    /**
     * Executes the specific script with local variables in scope.
     * @param script Script to execute
     * @param bindings Bindings with variables
     * @return
     */
    def execute(script: String, bindings: Bindings): Unit = {
      val argListString = bindings.keySet().mkString(", ")
      val valueList = bindings.values().toList

      val wrappedScript = s"(function($argListString){ $script\n })"
      val func = engine.eval(wrappedScript).asInstanceOf[JSObject]
      func.call(null, valueList: _*)
    }
  }

}
