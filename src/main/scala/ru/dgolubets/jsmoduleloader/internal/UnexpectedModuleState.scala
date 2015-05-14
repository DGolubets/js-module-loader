package ru.dgolubets.jsmoduleloader.internal


private[jsmoduleloader] class UnexpectedModuleState(state: Module.State) extends RuntimeException(s"Unexpected module state: $state")

private[jsmoduleloader] object UnexpectedModuleState {
  def apply(state: Module.State) = new UnexpectedModuleState(state)
}