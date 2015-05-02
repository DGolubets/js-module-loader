package ru.dgolubets.scripting.impl

import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths, Files}

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}


class FileModuleReaderSpec extends WordSpec with Matchers with ScalaFutures {

  "FileModuleReader" should {
    "read a file" in {
      val expectedText = {
        val bytes = Files.readAllBytes(new File("src/test/javascript/amd/definitions/object.js").toPath)
        new String(bytes, StandardCharsets.UTF_8)
      }

      val reader = FileModuleReader("src/test/javascript")
      val readOperation = reader.read(new URI("amd/definitions/object"))
      whenReady(readOperation) { text =>
        text shouldBe expectedText
      }
    }
  }

}
