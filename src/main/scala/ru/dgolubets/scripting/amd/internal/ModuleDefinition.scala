package ru.dgolubets.scripting.amd.internal

import java.net.URI

/**
 * AMD module definition.
 * @param id Module id
 * @param uri Module file URI
 * @param factory Module factory
 */
private[amd] case class ModuleDefinition(id: String, uri: URI, factory: ResolutionContext => ModuleInstance)
