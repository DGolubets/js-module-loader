package ru.dgolubets

import grizzled.slf4j.Logger

/**
 * Logging mixin.
 */
trait Logging {

  protected lazy val log = Logger(this.getClass)

}
