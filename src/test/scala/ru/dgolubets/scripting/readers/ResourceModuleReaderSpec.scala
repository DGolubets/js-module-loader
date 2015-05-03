package ru.dgolubets.scripting.readers

import java.net.URI

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class ResourceModuleReaderSpec extends WordSpec with Matchers with ScalaFutures {

  import org.scalatest.TryValues._

  val baseUrl = "/"
  val moduleUrl = "globals"

  "UrlModuleReader" should {
    "read a file" in {

      val reader = ResourceModuleReader(baseUrl, this.getClass)
      val text = reader.read(URI.create(moduleUrl))
      assert(text.success.value.lines.length > 1, "jQuery is likely to have more than one line")
    }

    "read a file asynchronously" in {

      val reader = ResourceModuleReader(baseUrl, this.getClass)
      val readOperation = reader.readAsync(URI.create(moduleUrl))
      whenReady(readOperation) { text =>
        assert(text.lines.length > 1, "jQuery is likely to have more than one line")
      }
    }
  }

}
