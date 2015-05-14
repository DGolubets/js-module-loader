package ru.dgolubets.jsmoduleloader.amd

import javax.script.ScriptEngineManager

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import ru.dgolubets.jsmoduleloader.internal.Resource
import ru.dgolubets.jsmoduleloader.readers.FileModuleReader

trait AmdLoaderSpecBase extends WordSpec with Matchers with ScalaFutures  {

  val engineManager = new ScriptEngineManager(null)

  trait Test {
    val loader = AmdLoader(FileModuleReader("src/test/javascript/amd"))
  }

  trait BaseTest extends Test {
    loader.engine.eval(Resource.readString("/globals.js").get)
  }
}
