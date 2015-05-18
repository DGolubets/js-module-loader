package ru.dgolubets.jsmoduleloader.api.readers

import java.net.URI

import ru.dgolubets.jsmoduleloader.internal.Resource

import scala.util.Try

object ResourceModuleReader {

  /**
   * Creates new ResourceModuleReader.
   * @param baseName Base resource name
   * @param resourceClass Class to use for loading resources
   * @return
   */
  def apply(baseName: String, resourceClass: Class[_]) = new ResourceModuleReader(baseName, resourceClass)
}

/**
 * Reads module files from resources.
 * @param baseName Base resource name
 * @param resourceClass Class to use for loading resources
 */
class ResourceModuleReader(baseName: String, resourceClass: Class[_]) extends ScriptModuleReader {

  private def getResourceName(moduleUri: URI): String = {
    var moduleId = moduleUri.toString
    if(!moduleId.endsWith(".js")){
      moduleId += ".js"
    }
    baseName + moduleId
  }

  /**
   * Reads module text.
   * @param uri Module URI
   * @return
   */
  override def read(uri: URI): Try[String] = Resource.readString(getResourceName(uri), resourceClass)
}


