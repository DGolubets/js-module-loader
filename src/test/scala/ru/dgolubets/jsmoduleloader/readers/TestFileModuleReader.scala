package ru.dgolubets.jsmoduleloader.readers

import java.io.File
import java.net.URI
import java.nio.charset.Charset

/**
 * File reader with shim config.
 */
class TestFileModuleReader(baseDir: File, shim: Map[String, String]) extends FileModuleReader(baseDir, charset = Charset.defaultCharset()) {
  override protected def getModuleFile(uri: URI): File = {
    val uriStr = uri.toString
    val absolute = !uriStr.startsWith(".")
    if(absolute && shim.contains(uriStr)){
      new File(shim(uriStr))
    }
    else super.getModuleFile(uri)
  }
}
