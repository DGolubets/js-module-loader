package ru.dgolubets.jsmoduleloader.api.amd

import javax.script.ScriptEngineManager
import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.scalatest.{AsyncWordSpec, Matchers}
import ru.dgolubets.jsmoduleloader.api.ScriptModuleException
import ru.dgolubets.jsmoduleloader.api.readers.FileModuleReader
import ru.dgolubets.jsmoduleloader.internal.Resource

import scala.concurrent.Promise

// https://github.com/amdjs/amdjs-api/blob/master/AMD.md
class AmdLoaderSpec extends AsyncWordSpec with Matchers {

  val engineManager = new ScriptEngineManager(null)

  def createLoader = AmdLoader(FileModuleReader("src/test/javascript/amd"))

  def createLoaderWithGlobals = {
    val loader = AmdLoader(FileModuleReader("src/test/javascript/amd"))
    loader.engine.eval(Resource.readString("/globals.js").get)
    loader
  }

  "AmdLoader" when {

    "created with it's own engine" should {
      "set globals" in {
        val loader = createLoader
        loader.engine.eval("typeof global === 'object'") shouldBe true
        loader.engine.eval("typeof console === 'object'") shouldBe true
        loader.engine.eval("typeof console.log === 'function'") shouldBe true
        loader.engine.eval("typeof console.debug === 'function'") shouldBe true
        loader.engine.eval("typeof console.warn === 'function'") shouldBe true
        loader.engine.eval("typeof console.error === 'function'") shouldBe true
      }
    }

    "created" should {

      "create 'require' function on the engine" in {
        val loader = createLoader
        loader.engine.eval("typeof require == 'function'") shouldBe true
      }

      "create 'define' function on the engine" in {
        val loader = createLoader
        loader.engine.eval("typeof define == 'function'") shouldBe true
      }

      "set define.amd property" in {
        val loader = createLoader
        loader.engine.eval("typeof define.amd === 'object'") shouldBe true
      }

    }

    "loads modules" should {
      "expose engine scope to modules for read access" in {
        val loader = createLoader
        val module = loader.requireAsync("core/readEngineScope")

        loader.engine.put("engineText", "some text")

        module.map(_ => succeed)
      }
    }

    "require is called in javascript" should {
      "load module" in {
        val loader = createLoaderWithGlobals
        val promise = Promise[AnyRef]()
        loader.engine.put("promise", promise)
        loader.engine.eval("require(['definitions/object'], function(m){ promise.success(m); })")

        promise.future.map { m =>
          m shouldNot be(null)
          m shouldBe a[ScriptObjectMirror]
        }
      }
    }

    "require is called in scala" should {

      "return error for invalid file" in {
        val loader = createLoaderWithGlobals
        val module = loader.requireAsync("aModuleThatDoesNotExist")

        module.failed.map { m =>
          m shouldBe a[ScriptModuleException]
        }
      }

      def load[T: Manifest](message: String, file: String, check: T => Boolean = { _: T => true }) = {
        s"load $message" in {
          val loader = createLoaderWithGlobals
          val module = loader.requireAsync(file)

          module.map { m =>
            m.value shouldBe a[T]
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