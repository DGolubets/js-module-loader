package ru.dgolubets.jsmoduleloader.commonjs

import java.io.File

import jdk.nashorn.api.scripting.JSObject
import org.scalatest.FreeSpec
import ru.dgolubets.jsmoduleloader.ScriptModuleException

class CommonJsLoaderSpec extends FreeSpec with CommonJsLoaderSpecBase {

  import org.scalatest.TryValues._

  "CommonJsLoader" - {

    "when created" - {

      "create 'require' function on the engine" in new Test {
        loader.engine.eval("typeof require == 'function'") shouldBe true
      }

    }
  }

  "when require is called in scala" - {

    "should return failure for non existing module" in new Test {
      loader.require("aModuleThatDoesNotExist").failure.exception shouldBe a[ScriptModuleException]
    }

    "should return script object for existing module" in new Test {
      loader.require("custom/simple/a").success.value.value shouldBe a[JSObject]
    }
  }

  "Standard CommonJS tests" -  {
    runJsTests(new File(testsDir, "standard"))
  }

  "Custom tests" -  {
    runJsTests(new File(testsDir, "custom"))
  }
}