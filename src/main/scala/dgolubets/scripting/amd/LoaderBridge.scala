package dgolubets.scripting.amd

import dgolubets.Logging
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

/**
 * JS to JVM proxy.
 * It wraps a loader with a specified context.
 *
 * @param loader Script loader
 * @param context Loader context
 */
private class LoaderBridge(loader: AMDScriptLoader, context: LoaderContext) extends Logging {

  def require(moduleName: String): AnyRef = {
    log.trace(s"require('$moduleName')")
    val module = loader.require(moduleName)
    Await.result(module, 30.seconds).value
  }

  def require(moduleNames: Array[String], callback: ScriptObjectMirror): Unit = {
    log.trace(s"require([${moduleNames.map(d => s"'$d'").mkString(",")}], $callback)")
    loader.require(moduleNames).map(modules => callback.call(null, modules.map(_.value): _*))
  }

  def define(dependencies: Array[String], callback: ScriptObjectMirror): Unit  = {
    log.trace(s"define([${dependencies.map(d => s"'$d'").mkString(",")}], $callback)")
    loader.define(None, dependencies, callback)(context)
  }

  def define(moduleId: String, dependencies: Array[String], callback: ScriptObjectMirror) = {
    log.trace(s"define('$moduleId'), [${dependencies.map(d => s"'$d'").mkString(",")}], $callback)")
    loader.define(Some(moduleId), dependencies, callback)(context)
  }

}
