package ru.dgolubets.scripting.amd

import java.net.URI

/**
 * AMD module definition.
 * @param id Module id
 * @param uri Module file URI
 * @param factory Module factory
 */
private case class ModuleDefinition(id: String, uri: URI, factory: ResolutionContext => ModuleInstance)
