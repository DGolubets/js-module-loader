package ru.dgolubets.jsmoduleloader.japi.readers

import ru.dgolubets.jsmoduleloader.api
import ru.dgolubets.jsmoduleloader.conversions.JavaScriptModuleReaderWrapper

/**
 * Reads module files from an URL.
 * @param baseUrl Base URL
 */
class UrlModuleReader(baseUrl: String)
  extends JavaScriptModuleReaderWrapper(api.readers.UrlModuleReader(baseUrl))