package ru.dgolubets.jsmoduleloader.readers

import java.io.BufferedReader
import java.net.URI
import java.nio.CharBuffer

import jdk.nashorn.api.scripting.URLReader

import scala.util.Try

object UrlModuleReader {
  /**
   * Create new UrlModuleReader
   * @param baseUrl Base URL
   * @return
   */
  def apply(baseUrl: String) = new UrlModuleReader(baseUrl)
}


/**
 * Reads module files from an URL.
 * @param baseUrl Base URL
 */
class UrlModuleReader(baseUrl: String) extends ScriptModuleReader {

  private val baseUri = URI.create(
    if(baseUrl.endsWith("/")) baseUrl
    else baseUrl + "/")

  private def getAbsoluteURI(uri: URI): URI = {
    var fileUri = baseUri.resolve(uri).toString
    if(!fileUri.endsWith(".js")){
      fileUri += ".js"
    }
    URI.create(fileUri)
  }

  /**
   * Reads module text.
   * @param uri Module URI
   * @return
   */
  override def read(uri: URI): Try[String] = Try {
    val stringBuilder = new StringBuilder
    val reader = new BufferedReader(new URLReader(getAbsoluteURI(uri).toURL))
    try {
      val buffer = CharBuffer.allocate(1024)
      while (reader.read(buffer) > 0) {
        buffer.flip()
        stringBuilder.appendAll(buffer.array(), 0, buffer.remaining())
        buffer.clear()
      }
    }
    finally {
      reader.close()
    }
    stringBuilder.result()
  }
}
