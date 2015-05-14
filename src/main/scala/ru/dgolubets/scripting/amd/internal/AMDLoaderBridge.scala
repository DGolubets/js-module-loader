package ru.dgolubets.scripting.amd.internal

import jdk.nashorn.api.scripting.JSObject
import ru.dgolubets.internal.util.Logging
import ru.dgolubets.scripting.amd.AmdLoader

/**
 * JS to JVM proxy.
 * It wraps a loader with a specified context.
 *
 * @param loader Script loader
 * @param context Loader context
 */
private[amd] class AmdLoaderBridge(loader: AmdLoader, var context: AmdLoaderContext) extends Logging {
  import loader.executionContext

  def require(moduleName: String): AnyRef = {
    log.trace(s"require('$moduleName')")
    loader.requireLocal(moduleName)(context)
  }

  def require(moduleNames: Array[String], callback: JSObject): Unit = {
    log.trace(s"require([${moduleNames.map(d => s"'$d'").mkString(",")}], $callback)")
    loader.requireAsync(moduleNames).map(modules => callback.call(null, modules.map(_.value): _*))
  }

  def define(dependencies: Array[String], factory: JSObject): Unit  = {
    log.trace(s"define([${dependencies.map(d => s"'$d'").mkString(",")}], $factory)")
    loader.define(None, dependencies, factory)(context)
  }

  def define(moduleId: String, dependencies: Array[String], factory: JSObject): Unit = {
    log.trace(s"define('$moduleId'), [${dependencies.map(d => s"'$d'").mkString(",")}], $factory)")
    loader.define(Some(moduleId), dependencies, factory)(context)
  }

}
