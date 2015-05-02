package ru.dgolubets.scripting.amd

import java.io.{InputStreamReader, BufferedReader, File}
import javax.script.ScriptEngineManager

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import ru.dgolubets.scripting.impl.FileModuleReader

trait AMDScriptLoaderSpecBase extends WordSpec with Matchers with ScalaFutures  {

  val engineManager = new ScriptEngineManager(null)

  trait Test {
    val engine = engineManager.getEngineByName("nashorn")
    val loader = AMDScriptLoader(engine, FileModuleReader("src/test/javascript/amd"))
  }

  trait BaseTest extends Test {
    val globalsReader = new BufferedReader(new InputStreamReader(this.getClass.getResourceAsStream("/globals.js")))
    try {
      engine.eval(globalsReader)
    }
    finally {
      globalsReader.close()
    }
  }
}
