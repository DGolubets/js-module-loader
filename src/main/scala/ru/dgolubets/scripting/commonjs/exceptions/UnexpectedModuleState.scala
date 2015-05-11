package ru.dgolubets.scripting.commonjs.exceptions

import ru.dgolubets.scripting.commonjs.Module


class UnexpectedModuleState(state: Module.State) extends RuntimeException(s"Unexpected module state: $state")

object UnexpectedModuleState {
  def apply(state: Module.State) = new UnexpectedModuleState(state)
}