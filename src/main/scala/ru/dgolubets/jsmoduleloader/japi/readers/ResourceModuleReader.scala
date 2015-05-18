package ru.dgolubets.jsmoduleloader.japi.readers

import ru.dgolubets.jsmoduleloader.api
import ru.dgolubets.jsmoduleloader.conversions.JavaScriptModuleReaderWrapper

/**
 * Reads module files from resources.
 * @param baseName Base resource name
 * @param resourceClass Class to use for loading resources
 */
class ResourceModuleReader(baseName: String, resourceClass: Class[_])
  extends JavaScriptModuleReaderWrapper(api.readers.ResourceModuleReader(baseName, resourceClass))