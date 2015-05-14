package ru.dgolubets.jsmoduleloader.internal

import java.net.URI

/**
 * Base module definition.
 */
private[jsmoduleloader] trait ModuleDefinition {
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
