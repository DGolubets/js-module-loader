package ru.dgolubets.scripting.amd.internal

/**
 * Describes resolution process state.
 * It is used to detect circular dependencies.
 *
 * @param chain Chain of resolved modules
 */
private[amd] case class ResolutionContext(chain: List[Module] = Nil) {

  /**
   * The module being resolved.
   * @return
   */
  def module = chain.headOption
}
