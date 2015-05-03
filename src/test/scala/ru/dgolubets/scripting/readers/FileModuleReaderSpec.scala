package ru.dgolubets.scripting.readers

import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths, Files}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}


class FileModuleReaderSpec extends WordSpec with Matchers with ScalaFutures {

  import org.scalatest.TryValues._

  "FileModuleReader" should {
    "read a file" in {
      val expectedText = {
        val bytes = Files.readAllBytes(new File("src/test/javascript/amd/definitions/object.js").toPath)
        new String(bytes, StandardCharsets.UTF_8)
      }

      val reader = FileModuleReader("src/test/javascript")
      val text = reader.read(new URI("amd/definitions/object"))
      text.success.value shouldBe expectedText
    }

    "read a file asynchronously" in {
      val expectedText = {
        val bytes = Files.readAllBytes(new File("src/test/javascript/amd/definitions/object.js").toPath)
        new String(bytes, StandardCharsets.UTF_8)
      }

      val reader = FileModuleReader("src/test/javascript")
      val readOperation = reader.readAsync(new URI("amd/definitions/object"))
      whenReady(readOperation) { text =>
        text shouldBe expectedText
      }
    }
  }

}
