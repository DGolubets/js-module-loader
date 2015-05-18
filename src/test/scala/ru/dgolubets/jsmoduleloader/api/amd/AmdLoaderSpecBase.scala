package ru.dgolubets.jsmoduleloader.api.amd

import javax.script.ScriptEngineManager

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import ru.dgolubets.jsmoduleloader.api.readers.FileModuleReader
import ru.dgolubets.jsmoduleloader.internal.Resource

trait AmdLoaderSpecBase extends WordSpec with Matchers with ScalaFutures  {

  val engineManager = new ScriptEngineManager(null)

  trait Test {
    val loader = AmdLoader(FileModuleReader("src/test/javascript/amd"))
  }

  trait BaseTest extends Test {
    loader.engine.eval(Resource.readString("/globals.js").get)
  }
}
