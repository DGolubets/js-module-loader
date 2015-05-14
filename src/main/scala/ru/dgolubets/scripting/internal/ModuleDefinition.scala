package ru.dgolubets.scripting.internal

import java.net.URI

/**
 * Base module definition.
 */
trait ModuleDefinition {
  /**
   * Absolute module id.
   * @return
   */
  def id: String

  /**
   * Absolute module file URI.
   * @return
   */
  def uri: Option[URI]
}
