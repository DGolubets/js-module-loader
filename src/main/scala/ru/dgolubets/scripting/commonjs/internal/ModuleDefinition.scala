package ru.dgolubets.scripting.commonjs.internal

import java.net.URI

/**
 * AMD module definition.
 * @param id Module id
 * @param uri Module file URI
 * @param script Module script
 */
private[commonjs] case class ModuleDefinition(id: String, uri: URI, script: String)
