package ru.dgolubets.jsmoduleloader.commonjs

import ru.dgolubets.jsmoduleloader.internal.Module

/**
 * Describes resolution process state.
 * It is used to detect circular dependencies.
 *
 * @param chain Chain of resolved modules
 */
private case class CommonJsResolutionContext(chain: List[Module[CommonJsModuleDefinition]] = Nil) {

  /**
   * The module being resolved.
   * @return
   */
  def module = chain.headOption
}
