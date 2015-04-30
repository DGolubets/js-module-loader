package dgolubets.scripting.amd

import java.io._
import javax.script._

import dgolubets.scripting.ScriptModuleException
import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.scalatest._
import org.scalatest.concurrent._

import scala.concurrent.Promise
import scala.reflect.ClassTag

// https://github.com/amdjs/amdjs-api/blob/master/AMD.md
class AMDScriptLoaderSpec extends WordSpec with Matchers with ScalaFutures {

  val engineManager = new ScriptEngineManager(null)

  trait CleanTest {
    val engine = engineManager.getEngineByName("nashorn")
    val loader = AMDScriptLoader(engine, new File("src/test/javascript/amd"))
  }

  trait BaseTest extends CleanTest {
    val globalsReader = new BufferedReader(new InputStreamReader(this.getClass.getResourceAsStream("/globals.js")))
    try {
      engine.eval(globalsReader)
    }
    finally{
      globalsReader.close()
    }
  }

  "AMDScriptLoader" when {

    "created" should {

      "expose define, require and nothing more" in new CleanTest {
        var bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE)
        assert(bindings.containsKey("define"))
        assert(bindings.containsKey("require"))

        assert(bindings.keySet.size() == 2)
      }
    }

    "loads modules" should {
      "expose engine scope to modules for read access" in new CleanTest {
        val module = loader.require("core/readEngineScope")

        engine.put("engineText", "some text")
        whenReady(module) { m =>
        }
      }

      "not expose engine scope to modules for write access" in new CleanTest {
        val module = loader.require("core/writeEngineScope")

        whenReady(module) { m =>
          engine.get("engineText") shouldBe null
        }
      }
    }

    "require in called in javascript" should {
      "load module" in new BaseTest {
        val promise = Promise[AnyRef]()
        engine.put("promise", promise)
        engine.eval("require(['definitions/object'], function(m){ promise.success(m); })")

        whenReady(promise.future) { m =>
          m shouldNot be (null)
          m shouldBe a [ScriptObjectMirror]
        }
      }
    }

    "require is called in scala" should {

      "return error for invalid file" in new BaseTest {
        val module = loader.require("aModuleThatDoesNotExist")

        whenReady(module.failed) { m =>
          m shouldBe a [ScriptModuleException]
        }
      }

      def loadModule[T: Manifest](format: String, file: String, check: T => Boolean = { _: T => true}) = {
        s"load a module in $format" in  new BaseTest {
          val module = loader.require(file)

          whenReady(module) { m =>
            m.value shouldBe a [T]
            assert(check(m.value.asInstanceOf[T]))
          }
        }
      }

      loadModule[ScriptObjectMirror]("simple object format", "definitions/object")

      loadModule[ScriptObjectMirror]("simple function format", "definitions/function")

      loadModule[ScriptObjectMirror]("function format with dependencies", "definitions/functionWithDependencies")

      loadModule[ScriptObjectMirror]("function format with dependencies that returns a function", "definitions/returnsFunction",
        m => m.isFunction)

      loadModule[ScriptObjectMirror]("function format with dependencies that returns an array", "definitions/returnsArray",
        m => m.isArray)

      loadModule[String]("function format with dependencies that returns a string", "definitions/returnsString")

      loadModule[Integer]("function format with dependencies that returns a number", "definitions/returnsNumber")


      "load simple circular dependant modules" in new BaseTest {
        val module = loader.require("dependencies/circular/simple/A")

        whenReady(module) { m =>
          println(m)
        }
      }

      "load circular dependant modules defined with 'exports'" in new BaseTest {
        val module = loader.require("dependencies/circular/exports/A")

        whenReady(module) { m =>
          println(m)
        }
      }

      "load modules with common dependency defined with 'exports'" in new BaseTest {
        val module = loader.require("dependencies/common/exports/A")

        whenReady(module) { m =>
          println(m)
        }
      }

      "load modules with common dependency" in new BaseTest {
        val module = loader.require("dependencies/common/simple/A")

        whenReady(module) { m =>
          println(m)
        }
      }
    }
  }
}