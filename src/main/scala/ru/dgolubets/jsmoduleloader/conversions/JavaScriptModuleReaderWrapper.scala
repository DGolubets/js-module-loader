package ru.dgolubets.jsmoduleloader.conversions

import java.net.URI
import java.util.concurrent.CompletableFuture

import ru.dgolubets.jsmoduleloader.api
import ru.dgolubets.jsmoduleloader.conversions.JavaConversions._
import ru.dgolubets.jsmoduleloader.japi.readers.ScriptModuleReader


/**
 * Wrapper for scala ScriptModuleReader.
 * @param reader Scala ScriptModuleReader
 */
class JavaScriptModuleReaderWrapper(reader: api.readers.ScriptModuleReader) extends ScriptModuleReader {

  /**
   * Reads module text.
   * @param uri Module URI
   * @return
   */
  override def read(uri: URI): String = reader.read(uri).get

  /**
   * Reads module file text asynchronously.
   * @param uri Module URI
   * @return
   */
  override def readAsync(uri: URI): CompletableFuture[String] = reader.readAsync(uri)
}
