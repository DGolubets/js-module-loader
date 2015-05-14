package ru.dgolubets.scripting.internal

/**
 * Created by Dima on 13.05.2015.
 */
trait Loader {
  type ModuleDefinitionType <: ModuleDefinition

  /**
   * Synchronization object for modules list
   */
  private object _modulesLock

  /**
   * List of the modules.
   * Every module that was required explicitly or through dependency chain is added to the list.
   *
   * The key of a module is it's absolute module id.
   */
  private var modules = Map[String, Module[ModuleDefinitionType]]()

  /**
   * Gets or adds an empty module.
   * @param id Absolute module id
   * @return
   */
  protected def ensureModule(id: String): Module[ModuleDefinitionType] = _modulesLock.synchronized {
    // synchronization is required cos it can be called in futures from different threads
    modules.getOrElse(id, {
      val module = Module[ModuleDefinitionType](id)
      modules += (id -> module)
      module
    })
  }
}
