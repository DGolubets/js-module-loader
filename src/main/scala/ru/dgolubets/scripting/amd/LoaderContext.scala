package ru.dgolubets.scripting.amd

import java.net.URI

/**
 * Describes loader state during evaluation of a module in a separate file.
 *
 * @param moduleId AMD module id to use when it is omitted in the definition
 * @param file File being evaluated
 * @param scriptContext Current script context
 */
private case class LoaderContext(moduleId: String, file: URI, scriptContext: LoaderScriptContext)
