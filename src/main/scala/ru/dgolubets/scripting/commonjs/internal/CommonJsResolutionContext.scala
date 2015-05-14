package ru.dgolubets.scripting.commonjs.internal

import ru.dgolubets.scripting.internal.Module

/**
 * Describes resolution process state.
 * It is used to detect circular dependencies.
 *
 * @param chain Chain of resolved modules
 */
private[commonjs] case class CommonJsResolutionContext(chain: List[Module[CommonJsModuleDefinition]] = Nil) {

  /**
   * The module being resolved.
   * @return
   */
  def module = chain.headOption
}
