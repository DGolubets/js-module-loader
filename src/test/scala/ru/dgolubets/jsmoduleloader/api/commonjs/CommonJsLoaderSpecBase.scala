package ru.dgolubets.jsmoduleloader.api.commonjs

import java.io.{File, _}

import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.scalatest._
import ru.dgolubets.jsmoduleloader.api.readers.TestFileModuleReader


trait CommonJsLoaderSpecBase extends TestRegistration with Matchers {
  this: Suite =>

  import org.scalatest.TryValues._

  val testsDir = new File("src/test/javascript/commonjs")

  trait Test {
    val loader = CommonJsLoader(testReader(testsDir))
  }

  /**
   * Reader with shim config for "test" module.
   * @param baseDir
   * @return
   */
  def testReader(baseDir: File) = new TestFileModuleReader(baseDir, Map("test" -> "src/test/javascript/commonjs/util/test.js"))

  /**
   * Runs JavaScript test file.
   * @param file Test file
   */
  def runJsTestFile(file: File) = {
    val testDir = file.getParentFile
    val module = testDir.toURI.relativize(file.toURI).toString
    registerTest(file.toString) {
      val loader = CommonJsLoader(testReader(testDir))
      loader.require(module).success.value.value shouldBe a[ScriptObjectMirror]
    }
  }

  /**
   * Runs JavaScript test files in the specified directory.
   *
   * @param dir Directory with tests
   * @param testFileName Test file name
   *                     Default = "program.js"
   */
  def runJsTests(dir: File = testsDir, testFileName: String = "program.js") = {
    def findTests(dir: File): Seq[File] ={
      val testFiles = dir.listFiles(new FileFilter {
        override def accept(pathname: File): Boolean = pathname.isDirectory || pathname.getName == testFileName
      })

      testFiles.flatMap { f =>
        if(f.isFile) Seq(f)
        else findTests(f)
      }
    }

    for(testFile <- findTests(dir)) {
      runJsTestFile(testFile)
    }
  }
}
