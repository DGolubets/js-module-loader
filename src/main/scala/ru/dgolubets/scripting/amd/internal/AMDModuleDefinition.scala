package ru.dgolubets.scripting.amd.internal

import java.net.URI
import javax.script.Bindings

import jdk.nashorn.api.scripting.JSObject
import ru.dgolubets.scripting.internal.ModuleDefinition

/**
 * AMD module definition.
 * @param id Module id
 * @param uri Module file URI
 * @param dependencies Module dependencies list
 * @param moduleFactory Module factory js-object
 */
private[amd] case class AmdModuleDefinition(id: String, uri: Option[URI],
                                            dependencies: Seq[String], moduleFactory: JSObject,
                                            moduleBindings: Bindings) extends ModuleDefinition
