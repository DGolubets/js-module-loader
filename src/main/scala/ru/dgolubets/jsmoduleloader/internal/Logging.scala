package ru.dgolubets.jsmoduleloader.internal

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory


/**
 * Logging mixin.
 */
private[dgolubets] trait Logging {

  @transient
  protected lazy val log = Logger(LoggerFactory.getLogger(this.getClass))

}
