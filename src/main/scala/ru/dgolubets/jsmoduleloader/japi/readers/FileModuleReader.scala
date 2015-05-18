package ru.dgolubets.jsmoduleloader.japi.readers

import java.io.File
import java.nio.charset.Charset

import ru.dgolubets.jsmoduleloader.api
import ru.dgolubets.jsmoduleloader.conversions.JavaScriptModuleReaderWrapper


/**
 * Reads module files from a file system.
 * @param baseDir Base directory
 * @param charset Files character encoding
 */
class FileModuleReader(baseDir: File, charset: Charset)
  extends JavaScriptModuleReaderWrapper(api.readers.FileModuleReader(baseDir, charset))