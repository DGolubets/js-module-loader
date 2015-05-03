package ru.dgolubets.scripting.readers

import java.net.URI

import scala.concurrent.Future
import scala.util.Try

/**
 * Base module reader interface.
 */
trait ScriptModuleReader {

  /**
   * Reads module text.
   * @param uri Module URI
   * @return
   */
  def read(uri: URI): Try[String]

  /**
   * Reads module file text asynchronously.
   * @param uri Module URI
   * @return
   */
  def readAsync(uri: URI): Future[String] = Future.fromTry(read(uri))
}
