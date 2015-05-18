package ru.dgolubets.jsmoduleloader.japi.readers

import java.net.URI
import java.util.concurrent.CompletableFuture

/**
 * Base module reader class.
 * Java compatible.
 */
abstract class ScriptModuleReader {

  /**
   * Reads module text.
   * @param uri Module URI
   * @return
   */
  def read(uri: URI): String

  /**
   * Reads module file text asynchronously.
   * @param uri Module URI
   * @return
   */
  def readAsync(uri: URI): CompletableFuture[String] = {
    val javaFuture = new CompletableFuture[String]
    try {
      javaFuture.complete(read(uri))
    }
    catch {
      case err: Throwable => javaFuture.completeExceptionally(err)
    }
    javaFuture
  }
}
