package ru.dgolubets.scripting.commonjs.internal.exceptions

import ru.dgolubets.scripting.commonjs.internal.Module


class UnexpectedModuleState(state: Module.State) extends RuntimeException(s"Unexpected module state: $state")

object UnexpectedModuleState {
  def apply(state: Module.State) = new UnexpectedModuleState(state)
}