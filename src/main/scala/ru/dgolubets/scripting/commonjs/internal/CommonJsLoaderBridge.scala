package ru.dgolubets.scripting.commonjs.internal

import ru.dgolubets.internal.util.Logging
import ru.dgolubets.scripting.commonjs.CommonJsLoader

/**
 * JS to JVM proxy.
 * It wraps a loader with a specified context.
 *
 * @param loader Script loader
 * @param context Loader context
 */
private[commonjs] class CommonJsLoaderBridge(loader: CommonJsLoader, var context: CommonJsLoaderContext) extends Logging {

  def require(moduleName: String): AnyRef = {
    log.trace(s"require('$moduleName')")
    loader.requireLocal(moduleName)(context)
  }

}
