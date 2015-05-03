package ru.dgolubets.util

import java.io.{InputStreamReader, BufferedReader}
import java.nio.CharBuffer

import scala.util.Try
import scala.collection.JavaConversions._

/**
 * Resource util.
 */
private[dgolubets] object Resource {
  /**
   * Reads resource string.
   * @param resourceName Name of the resource
   * @return
   */
  def readString(resourceName: String): Try[String] = Try {
      val reader = new BufferedReader(new InputStreamReader(this.getClass.getResourceAsStream(resourceName)))
      try {
        val stringBuilder = new StringBuilder()
        val buffer = CharBuffer.allocate(1024)
        while (reader.read(buffer) > 0){
          buffer.flip()
          stringBuilder.appendAll(buffer.array(), 0, buffer.remaining())
        }
        stringBuilder.result()
      }
      finally {
        reader.close()
      }
  }
}
