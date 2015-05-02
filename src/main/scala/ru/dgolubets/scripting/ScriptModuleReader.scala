package ru.dgolubets.scripting

import java.net.URI

import scala.concurrent.Future

/**
 * Base module reader interface.
 */
trait ScriptModuleReader {

  /**
   * Reads module file text.
   * @param uri Module uri
   * @return
   */
  def read(uri: URI): Future[String]
}
