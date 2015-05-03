package ru.dgolubets.scripting.amd

import javax.script._

import ru.dgolubets.scripting.ScriptModuleException
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.concurrent.Promise

// https://github.com/amdjs/amdjs-api/blob/master/AMD.md
class AMDScriptLoaderSpec extends AMDScriptLoaderSpecBase {

  "AMDScriptLoader" when {

    "created" should {

      "expose define and require" in new Test {
        engine.eval("define", loader.context) shouldBe a [ScriptObjectMirror]
        engine.eval("require", loader.context) shouldBe a [ScriptObjectMirror]
      }

      "set define.amd property" in new Test {
        engine.eval("typeof define.amd === 'object'", loader.context) shouldBe true
      }

      "leave engine scope clean" in new Test {
        var bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE)
        assert(bindings.keySet.size() == 0)
      }

      "leave global scope clean" in new Test {
        var bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE)
        assert(bindings.keySet.size() == 0)
      }
    }

    "loads modules" should {
      "expose engine scope to modules for read access" in new Test {
        val module = loader.requireAsync("core/readEngineScope")

        engine.put("engineText", "some text")
        whenReady(module) { m =>
        }
      }
    }

    "require is called in javascript" should {
      "load module" in new BaseTest {
        val promise = Promise[AnyRef]()
        engine.put("promise", promise)
        engine.eval("require(['definitions/object'], function(m){ promise.success(m); })", loader.context)

        whenReady(promise.future) { m =>
          m shouldNot be (null)
          m shouldBe a [ScriptObjectMirror]
        }
      }
    }

    "require is called in scala" should {

      "return error for invalid file" in new BaseTest {
        val module = loader.requireAsync("aModuleThatDoesNotExist")

        whenReady(module.failed) { m =>
          m shouldBe a [ScriptModuleException]
        }
      }

      def load[T: Manifest](message: String, file: String, check: T => Boolean = { _: T => true}) = {
        s"load $message" in  new BaseTest {
          val module = loader.requireAsync(file)

          whenReady(module) { m =>
            m.value shouldBe a [T]
            assert(check(m.value.asInstanceOf[T]))
          }
        }
      }

      load[ScriptObjectMirror]("a module in simple object format", "definitions/object")

      load[ScriptObjectMirror]("a module in simple function format", "definitions/function")

      load[ScriptObjectMirror]("a module in simple function format with default arguments", "definitions/functionWithDefaultArgs")

      load[ScriptObjectMirror]("a module in function format with dependencies", "definitions/functionWithDependencies")

      load[ScriptObjectMirror]("a module in function format with dependencies that returns a function", "definitions/returnsFunction",
        m => m.isFunction)

      load[ScriptObjectMirror]("a module in function format with dependencies that returns an array", "definitions/returnsArray",
        m => m.isArray)

      load[String]("a module in function format with dependencies that returns a string", "definitions/returnsString")

      load[Integer]("a module in function format with dependencies that returns a number", "definitions/returnsNumber")



      load("simple circular dependant modules", "dependencies/circular/simple/A")

      load("circular dependant modules defined with 'exports'", "dependencies/circular/exports/A")

      load("modules with common dependency", "dependencies/common/simple/A")

      load("modules with common dependency defined with 'exports'", "dependencies/common/exports/A")

      load("module using local require", "dependencies/local/A")


      load("modules bundle", "bundles/ABC")

    }
  }
}