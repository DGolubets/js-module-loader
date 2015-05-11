package ru.dgolubets.scripting.commonjs

import java.io.File
import javax.script._

import jdk.nashorn.api.scripting.{JSObject, ScriptObjectMirror}
import org.scalatest.FreeSpec
import ru.dgolubets.scripting.ScriptModuleException

class CommonJsLoaderSpec extends FreeSpec with CommonJsLoaderSpecBase {

  import org.scalatest.TryValues._

  "CommonJsLoader" - {

    "when created" - {

      "expose require" in new Test {
        engine.eval("require", loader.context) shouldBe a [ScriptObjectMirror]
      }

      "leave engine scope clean" in new Test {
        val bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE)
        assert(bindings.keySet.size() == 0)
      }

      "leave global scope clean" in new Test {
        val bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE)
        if(bindings != null) {
          assert(bindings.keySet.size() == 0)
        }
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