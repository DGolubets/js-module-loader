package ru.dgolubets.jsmoduleloader.conversions

import java.net.URI

import ru.dgolubets.jsmoduleloader.conversions.JavaConversions._
import ru.dgolubets.jsmoduleloader.{api, japi}

import scala.concurrent.Future
import scala.util.Try

/**
 * Wrapper for Java ScriptModuleReader.
 */
class ScalaScriptModuleReaderWrapper(reader: japi.readers.ScriptModuleReader) extends api.readers.ScriptModuleReader {

  /**
   * Reads module text.
   * @param uri Module URI
   * @return
   */
  override def read(uri: URI): Try[String] = Try(reader.read(uri))

  /**
   * Reads module file text asynchronously.
   * @param uri Module URI
   * @return
   */
  override def readAsync(uri: URI): Future[String] = reader.readAsync(uri)

}
