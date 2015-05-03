package ru.dgolubets.util

import java.io.{InputStreamReader, BufferedReader}
import java.nio.CharBuffer

import scala.util.{Failure, Try}

/**
 * Resource util.
 */
private[dgolubets] object Resource {
  /**
   * Reads resource string.
   * @param resourceName Name of the resource
   * @return
   */
  def readString(resourceName: String, resourceClass: Class[_] = this.getClass): Try[String] = Try {
      val resource = resourceClass.getResourceAsStream(resourceName)
      if(resource == null)
        throw new Exception(s"Resource was not found: $resourceName")

      val reader = new BufferedReader(new InputStreamReader(resource))
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
