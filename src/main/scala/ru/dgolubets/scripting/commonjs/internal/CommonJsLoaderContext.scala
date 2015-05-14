package ru.dgolubets.scripting.commonjs.internal

/**
 * Describes loader state during evaluation of a module in a separate file.
 */
private[commonjs] case class CommonJsLoaderContext(resolutionContext: CommonJsResolutionContext)