package dgolubets.scripting.amd

import java.io._
import java.util.concurrent.Executors
import javax.script._

import dgolubets.scripting.ScriptModuleException
import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.scalatest._
import org.scalatest.concurrent._

import scala.concurrent.{ExecutionContext, Promise}
import scala.reflect.ClassTag

// https://github.com/amdjs/amdjs-api/blob/master/AMD.md
class AMDScriptLoaderSpec extends WordSpec with Matchers with ScalaFutures {

  val engineManager = new ScriptEngineManager(null)

  trait Test {
    lazy val executionContext = ExecutionContext.Implicits.global
    val engine = engineManager.getEngineByName("nashorn")
    val loader = AMDScriptLoader(engine, new File("src/test/javascript/amd"), executionContext)
  }

  trait BaseTest extends Test {
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

      "expose define, require and nothing more" in new Test {
        var bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE)
        assert(bindings.containsKey("define"))
        assert(bindings.containsKey("require"))

        assert(bindings.keySet.size() == 2)
      }

      "set define.amd property" in new Test {
        engine.eval("typeof define.amd === 'object'") shouldBe true
      }
    }

    "loads modules" should {
      "expose engine scope to modules for read access" in new Test {
        val module = loader.require("core/readEngineScope")

        engine.put("engineText", "some text")
        whenReady(module) { m =>
        }
      }

      "not expose engine scope to modules for write access" in new Test {
        val module = loader.require("core/writeEngineScope")

        whenReady(module) { m =>
          engine.get("engineText") shouldBe null
        }
      }
    }

    "uses execution context" should {
      "load modules bundle in custom execution context" in new BaseTest {
        override lazy val executionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
        val module = loader.require("bundles/ABC")

        whenReady(module) { m =>
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

      def load[T: Manifest](message: String, file: String, check: T => Boolean = { _: T => true}) = {
        s"load $message" in  new BaseTest {
          val module = loader.require(file)

          whenReady(module) { m =>
            m.value shouldBe a [T]
            assert(check(m.value.asInstanceOf[T]))
          }
        }
      }

      load[ScriptObjectMirror]("a module in simple object format", "definitions/object")

      load[ScriptObjectMirror]("a module in simple function format", "definitions/function")

      load[ScriptObjectMirror]("a module in function format with dependencies", "definitions/functionWithDependencies")

      load[ScriptObjectMirror]("a module in function format with dependencies that returns a function", "definitions/returnsFunction",
        m => m.isFunction)

      load[ScriptObjectMirror]("a module in function format with dependencies that returns an array", "definitions/returnsArray",
        m => m.isArray)

      load[String]("a module in function format with dependencies that returns a string", "definitions/returnsString")

      load[Integer]("a module in function format with dependencies that returns a number", "definitions/returnsNumber")



      load("simple circular dependant modules", "dependencies/circular/simple/A")

      load("circular dependant modules defined with 'exports'", "dependencies/circular/exports/A")

      load("load modules with common dependency", "dependencies/common/simple/A")

      load("modules with common dependency defined with 'exports'", "dependencies/common/exports/A")


      load("load modules bundle", "bundles/ABC")

    }
  }
}