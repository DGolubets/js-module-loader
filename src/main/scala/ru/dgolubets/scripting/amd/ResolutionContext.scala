package ru.dgolubets.scripting.amd

/**
 * Describes resolution process state.
 * It is used to detect circular dependencies.
 *
 * @param chain Chain of resolved modules
 */
private case class ResolutionContext(chain: List[Module] = Nil) {

  /**
   * The module being resolved.
   * @return
   */
  def module = chain.headOption
}
