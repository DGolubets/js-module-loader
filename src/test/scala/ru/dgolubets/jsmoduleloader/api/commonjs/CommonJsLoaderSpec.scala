package ru.dgolubets.jsmoduleloader.api.commonjs

import java.io.File

import jdk.nashorn.api.scripting.JSObject
import org.scalatest.FreeSpec
import ru.dgolubets.jsmoduleloader.api.ScriptModuleException

class CommonJsLoaderSpec extends FreeSpec with CommonJsLoaderSpecBase {

  import org.scalatest.TryValues._

  "CommonJsLoader" - {

    "created with it's own engine" - {
      "should set globals" in new Test {
        loader.engine.eval("typeof global === 'object'") shouldBe true
        loader.engine.eval("typeof console === 'object'") shouldBe true
        loader.engine.eval("typeof console.log === 'function'") shouldBe true
        loader.engine.eval("typeof console.debug === 'function'") shouldBe true
        loader.engine.eval("typeof console.warn === 'function'") shouldBe true
        loader.engine.eval("typeof console.error === 'function'") shouldBe true
      }
    }

    "when created" - {

      "should create 'require' function on the engine" in new Test {
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