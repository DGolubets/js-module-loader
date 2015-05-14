package ru.dgolubets.scripting.amd.internal

import ru.dgolubets.scripting.internal.Module

/**
 * Describes resolution process state.
 * It is used to detect circular dependencies.
 *
 * @param chain Chain of resolved modules
 */
private[amd] case class AmdResolutionContext(chain: List[Module[AmdModuleDefinition]] = Nil) {

  /**
   * The module being resolved.
   * @return
   */
  def module = chain.headOption
}
