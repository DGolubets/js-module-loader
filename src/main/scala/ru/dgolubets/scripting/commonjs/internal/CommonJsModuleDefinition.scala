package ru.dgolubets.scripting.commonjs.internal

import java.net.URI

import ru.dgolubets.scripting.internal.ModuleDefinition

/**
 * AMD module definition.
 * @param id Module id
 * @param uri Module file URI
 * @param script Module script
 */
private[commonjs] case class CommonJsModuleDefinition(id: String, uri: Option[URI], script: String) extends ModuleDefinition
