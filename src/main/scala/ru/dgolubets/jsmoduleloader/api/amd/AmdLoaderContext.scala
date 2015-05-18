package ru.dgolubets.jsmoduleloader.api.amd

import java.net.URI
import javax.script.Bindings

import scala.collection.mutable.ListBuffer

/**
 * Describes loader state during evaluation of a module in a separate file.
 *
 * @param moduleId AMD module id to use when it is omitted in the definition
 * @param file File being evaluated
 * @param bindings Local bindings
 */
private case class AmdLoaderContext(moduleId: String, file: Option[URI], bindings: Bindings){

  /**
   * List of module definitions in the context.
   */
  val definitions = ListBuffer[AmdModuleDefinition]()

}
