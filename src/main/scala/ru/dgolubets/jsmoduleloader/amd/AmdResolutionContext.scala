package ru.dgolubets.jsmoduleloader.amd

import ru.dgolubets.jsmoduleloader.internal.Module

/**
 * Describes resolution process state.
 * It is used to detect circular dependencies.
 *
 * @param chain Chain of resolved modules
 */
private case class AmdResolutionContext(chain: List[Module[AmdModuleDefinition]] = Nil) {

  /**
   * The module being resolved.
   * @return
   */
  def module = chain.headOption
}
