package ru.dgolubets.internal

import scala.reflect.ClassTag

private[dgolubets] abstract class StateMachine[State: ClassTag](initial: State) {
  private object _lock
  private var _state: State = initial

  /**
   * Locks the module and executes the expression.
   * @param block Expression to execute
   * @tparam U Expression type
   * @return Result of the expression
   */
  def lock[U](block: => U): U ={
    _lock.synchronized {
      block
    }
  }

  /**
   * Locks the module if the condition is met and executes the expression.
   * @param condition Condition to check before locking
   * @param block Expression to execute
   * @return True if locked and executed, otherwise false
   */
  def lockIf(condition: => Boolean)(block: => Unit): Boolean = {
    if(condition) {
      _lock.synchronized {
        if(condition) {
          block
          true
        }
        else false
      }
    }
    else false
  }

  /**
   * Locks the module if it has specified state type and executes the expression.
   * @param block Expression to execute
   * @tparam S State type
   * @return True if locked and executed, otherwise false
   */
  def lockIfState[S <: State : ClassTag](block: S => Unit): Boolean = lockIf(_state match {
    case _: S => true
    case _ => false
  })(block(_state.asInstanceOf[S]))


  /**
   * Tries transition module from one state to another.
   * It is thread safe.
   *
   * @param newState A new state
   * @tparam From State that the module should be in before transition
   * @tparam To State that the module should be in after transition
   * @return true if transition succeeded or false if it didn't match the module current state
   */
  protected def tryTransition[From <: State : ClassTag, To <: State : ClassTag](newState: To): Boolean = {
    _state match {
      case _ : From =>
        _lock.synchronized {
          _state match {
            case _ : From =>
              state = newState
              true
            case _ => false
          }
        }
      case _ => false
    }
  }

  /**
   * Transitions module from one state to another.
   * Throws if module is in invalid state.
   *
   * @param newState A new state
   * @tparam From State that the module should be in before transition
   * @tparam To State that the module should be in after transition
   */
  protected def transition[From <: State : ClassTag, To <: State : ClassTag](newState: To) = throwInvalidState(tryTransition[From, To](newState))

  /**
   * Throws IllegalStateException if condition is not met.
   * Useful to wrap tryTransition methods.
   *
   * @param assert Condition to check
   */
  protected def throwInvalidState(assert: Boolean): Unit = if(!assert) throw new IllegalStateException()

  /**
   * Gets the module state.
   * @return
   */
  def state = _state


  /**
   * Sets module state.
   *
   * @param newState A new state
   * @tparam To State type
   */
  protected def state_=[To <: State](newState: To): Unit = {
    lockIf(_state != newState){
      var oldState = _state
      _state = newState
      onStateChanged(_state, oldState)
    }
  }

  /**
   * Called in a lock, when the state has been changed.
   * Override to react.
   *
   * @param newState New state
   * @param oldState Previous state
   */
  protected def onStateChanged(newState: State, oldState: State): Unit = {}
}
