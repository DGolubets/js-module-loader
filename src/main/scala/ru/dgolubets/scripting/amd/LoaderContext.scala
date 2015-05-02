package ru.dgolubets.scripting.amd

import java.net.URI

import scala.concurrent.Future

/**
 * Describes loader state during evaluation of a module in a separate file.
 *
 * @param moduleId AMD module id to use when it is omitted in the definition
 * @param file File being evaluated
 * @param scriptContext Current script context
 * @param fileLoaded signals that module file (bundle) processing has finished
 */
private case class LoaderContext(moduleId: String, file: URI, scriptContext: LoaderScriptContext, fileLoaded: Future[Unit])
