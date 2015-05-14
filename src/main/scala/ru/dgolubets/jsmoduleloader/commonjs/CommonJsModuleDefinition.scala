package ru.dgolubets.jsmoduleloader.commonjs

import java.net.URI

import ru.dgolubets.jsmoduleloader.internal.ModuleDefinition

/**
 * AMD module definition.
 * @param id Module id
 * @param uri Module file URI
 * @param script Module script
 */
private case class CommonJsModuleDefinition(id: String, uri: Option[URI], script: String) extends ModuleDefinition
