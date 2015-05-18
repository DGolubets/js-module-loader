package ru.dgolubets.jsmoduleloader.api.commonjs

import ru.dgolubets.jsmoduleloader.internal.Logging

/**
 * JS to JVM proxy.
 * It wraps a loader with a specified context.
 *
 * @param loader Script loader
 * @param context Loader context
 */
private class CommonJsLoaderBridge(loader: CommonJsLoader, var context: CommonJsLoaderContext) extends Logging {

  def require(moduleName: String): AnyRef = {
    log.trace(s"require('$moduleName')")
    loader.requireLocal(moduleName)(context)
  }

}
