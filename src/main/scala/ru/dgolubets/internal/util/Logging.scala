package ru.dgolubets.internal.util

import grizzled.slf4j.Logger

/**
 * Logging mixin.
 */
private[dgolubets] trait Logging {

  protected lazy val log = Logger(this.getClass)

}