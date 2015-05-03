package ru.dgolubets.scripting.readers

import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}


class UrlModuleReaderSpec extends WordSpec with Matchers with ScalaFutures {

  import org.scalatest.TryValues._

  val baseUrl = "https://cdnjs.cloudflare.com"
  val moduleUrl = "ajax/libs/jquery/2.1.4/jquery"

  "UrlModuleReader" should {
    "read a file" in {

      val reader = UrlModuleReader(baseUrl)
      val text = reader.read(URI.create(moduleUrl))
      assert(text.success.value.lines.length > 1, "jQuery is likely to have more than one line")
    }

    "read a file when base url ends with slash" in {

      val reader = UrlModuleReader(baseUrl + '/')
      val text = reader.read(URI.create(moduleUrl))
      assert(text.success.value.lines.length > 1, "jQuery is likely to have more than one line")
    }

    "read a file asynchronously" in {

      val reader = UrlModuleReader(baseUrl)
      val readOperation = reader.readAsync(URI.create(moduleUrl))
      whenReady(readOperation) { text =>
        assert(text.lines.length > 1, "jQuery is likely to have more than one line")
      }
    }
  }

}
