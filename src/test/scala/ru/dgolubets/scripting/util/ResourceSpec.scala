package ru.dgolubets.scripting.util

import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import ru.dgolubets.util.Resource


class ResourceSpec extends WordSpec with Matchers{

  import org.scalatest.TryValues._

  "Resource" should {
    "read a string" in {
      val text = Resource.readString("/globals.js")

      assert(text.success.value.length > 0)
      assert(text.success.value.lines.length > 1, "there should be more than one line for sure")
    }
  }

}
