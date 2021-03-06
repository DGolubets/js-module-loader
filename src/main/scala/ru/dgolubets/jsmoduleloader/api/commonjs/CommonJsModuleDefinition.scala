package ru.dgolubets.jsmoduleloader.api.commonjs

import java.net.URI

/**
 * AMD module definition.
 * @param id Module id
 * @param uri Module file URI
 * @param script Module script
 */
private case class CommonJsModuleDefinition(id: String, uri: Option[URI], script: String)
