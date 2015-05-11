package ru.dgolubets.scripting.commonjs

import java.net.URI

/**
 * AMD module definition.
 * @param id Module id
 * @param uri Module file URI
 * @param script Module script
 */
private case class ModuleDefinition(id: String, uri: URI, script: String)
