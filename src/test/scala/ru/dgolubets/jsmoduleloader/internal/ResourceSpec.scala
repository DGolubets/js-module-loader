package ru.dgolubets.jsmoduleloader.internal

import org.scalatest.{Matchers, WordSpec}


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
