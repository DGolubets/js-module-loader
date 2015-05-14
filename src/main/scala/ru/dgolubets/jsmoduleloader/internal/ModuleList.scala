package ru.dgolubets.jsmoduleloader.internal

import scala.collection.concurrent

/**
 * Module list.
 */
private[jsmoduleloader] class ModuleList[T <: ModuleDefinition] {
  private val _modules = concurrent.TrieMap[String, Module[T]]()

  /**
   * Gets a module.
   * @param id Module id
   * @return
   */
  def get(id: String): Option[Module[T]] = _modules.get(id)

  /**
   * Gets or creates a module.
   * @param id Module id
   * @return
   */
  def apply(id: String): Module[T] = _modules.getOrElseUpdate(id, Module[T](id))
}
