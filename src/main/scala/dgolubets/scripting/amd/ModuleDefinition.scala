package dgolubets.scripting.amd

import java.net.URI

/**
 * AMD module definition.
 * @param uri Module file URI
 * @param factory Module factory
 */
private case class ModuleDefinition(uri: URI, factory: ResolutionContext => ModuleInstance)
