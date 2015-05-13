package ru.dgolubets.scripting.amd

import javax.script.ScriptEngineManager

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import ru.dgolubets.scripting.readers.FileModuleReader
import ru.dgolubets.util.Resource

trait AMDScriptLoaderSpecBase extends WordSpec with Matchers with ScalaFutures  {

  val engineManager = new ScriptEngineManager(null)

  trait Test {
    val loader = AMDScriptLoader(FileModuleReader("src/test/javascript/amd"))
  }

  trait BaseTest extends Test {
    loader.engine.eval(Resource.readString("/globals.js").get)
  }
}
