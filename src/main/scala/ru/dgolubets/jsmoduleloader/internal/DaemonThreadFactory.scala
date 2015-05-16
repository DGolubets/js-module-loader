package ru.dgolubets.jsmoduleloader.internal

import java.util.concurrent.{Executors, ThreadFactory}

/**
 * Creates daemon threads.
 */
class DaemonThreadFactory extends ThreadFactory {
  override def newThread(r: Runnable): Thread = {
    val thread = Executors.defaultThreadFactory().newThread(r)
    thread.setDaemon(true)
    thread
  }
}
