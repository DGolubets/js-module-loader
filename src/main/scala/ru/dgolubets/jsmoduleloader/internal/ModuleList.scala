package ru.dgolubets.jsmoduleloader.internal

import scala.collection.concurrent

/**
 * Module list.
 */
private[jsmoduleloader] class ModuleList[Definition] {
  private val _modules = concurrent.TrieMap[String, Module[Definition]]()

  /**
   * Gets a module.
   * @param id Module id
   * @return
   */
  def get(id: String): Option[Module[Definition]] = _modules.get(id)

  /**
   * Gets or creates a module.
   * @param id Module id
   * @return
   */
  def apply(id: String): Module[Definition] = _modules.getOrElseUpdate(id, Module[Definition](id))
}
