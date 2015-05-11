package ru.dgolubets.scripting.commonjs

import scala.concurrent._
import scala.reflect.ClassTag
import scala.util._

private object Module {

  /**
   * Base module state.
   */
  sealed trait State

  /**
   * No-error module state.
   */
  sealed trait NoErrorState extends State

  /**
   * Error state.
   * @param err Exception
   */
  sealed case class Error(err: Throwable) extends State

  /**
   * Empty module.
   * It's the first state.
   */
  sealed case class Empty() extends NoErrorState

  /**
   * Loading module definition.
   */
  sealed case class Loading() extends NoErrorState

  /**
   * Module definition has been loaded.
   * @param definition Module definition
   */
  sealed case class Loaded(definition: ModuleDefinition) extends NoErrorState

  /**
   * Module is being initialised.
   * @param instance Module instance
   */
  sealed case class Initializing(instance: ModuleInstance) extends NoErrorState

  /**
   * Module is initialized.
   * @param instance Module instance
   */
  sealed case class Initialized(instance: ModuleInstance) extends NoErrorState
}

/**
 * Module state machine.
 *
 * @param id Absolute module id
 */
private case class Module(id: String) {
  import Module._

  private object _lock
  private var _state: State = Empty()
  private var _stateChanged = Promise[State]()

  /**
   * Locks the module and executes the expression.
   * @param block Expression to execute
   * @tparam T Expression type
   * @return Result of the expression
   */
  def lock[T](block: => T): T ={
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
      }
    }
    false
  }

  /**
   * Locks the module if it has specified state type and executes the expression.
   * @param block Expression to execute
   * @tparam S State type
   * @return True if locked and executed, otherwise false
   */
  def lockInState[S <: State : ClassTag](block: S => Unit): Boolean = lockIf(_state match {
    case _: S => true
    case _ => false
  })(block(_state.asInstanceOf[S]))

  /**
   * Sets module state.
   *
   * @param newState A new state
   * @tparam To State type
   */
  private def setState[To <: State](newState: To): Unit = {
    lockIf(_state != newState){
      _state = newState

      // notify of the state change
      _stateChanged.success(_state)
      // and reset promise
      _stateChanged = Promise[State]()
    }
  }

  /**
   * Transitions module from one state to another.
   * It is thread safe.
   *
   * @param newState A new state
   * @tparam From State that the module should be in before transition
   * @tparam To State that the module should be in after transition
   * @return true if transition succeeded or false if it didn't match the module current state
   */
  private def tryTransition[From <: State : ClassTag, To <: State : ClassTag](newState: To): Boolean = {
    _state match {
      case _ : From =>
        _lock.synchronized {
          _state match {
            case _ : From =>
              setState(newState)
              true
            case _ => false
          }
        }
      case _ => false
    }
  }

  private def throwInvalidState(assert: Boolean): Unit = if(!assert) throw new IllegalStateException("Module is in invalid state")

  /**
   * Gets the module state.
   * @return
   */
  def state = _state

  /**
   * Gets module future state.
   * It can be either normal ste or an error.
   *
   * @tparam S State type
   * @return
   */
  def futureState[S <: State : ClassTag](implicit ec: ExecutionContext): Future[Either[Error, S]] = {
    def matchState(state: State): Future[Either[Error, S]] = {
      _lock.synchronized {
        state match {
          case state: S => Future.successful(Right(state))
          case error: Error => Future.successful(Left(error))
          case _ => _stateChanged.future.flatMap(s => matchState(s))
        }
      }
    }
    matchState(_state)
  }

  /**
   * Tries to set the module to an Error state.
   * @param err Exception
   * @return true if the state has been set.
   */
  def tryFail(err: Throwable): Boolean = tryTransition[NoErrorState, Error](Error(err))

  /**
   * Sets the module to an Error state.
   * @param err Exception
   */
  def fail(err: Throwable) = throwInvalidState(tryFail(err))

  /**
   * Tries to set the module to Loading state.
   * @return
   */
  def tryStartLoading(): Boolean = tryTransition[Empty, Loading](Loading())

  /**
   * Sets the module to an Loading state.
   */
  def startLoading() = throwInvalidState(tryStartLoading())

  /**
   * Tries to set the module to Loaded state.
   * @param definition Module definition
   * @return
   */
  def tryLoad(definition: ModuleDefinition): Boolean = tryTransition[Loading, Loaded](Loaded(definition))

  /**
   * Sets the module to an Loaded state.
   * @param definition Module definition
   */
  def load(definition: ModuleDefinition) = throwInvalidState(tryLoad(definition))

  /**
   * Tries to set the module to Initializing state.
   * @param instance Initial module instance
   * @return
   */
  def tryStartInitializing(instance: ModuleInstance): Boolean = tryTransition[Loaded, Initializing](Initializing(instance))

  /**
   * Sets the module to Initializing state.
   * @param instance Module definition
   */
  def startInitializing(instance: ModuleInstance) = throwInvalidState(tryStartInitializing(instance))

  /**
   * Tries to set the module to Initialized state.
   * @param instance Final module instance
   * @return
   */
  def tryInitialize(instance: ModuleInstance): Boolean = tryTransition[Initializing, Initialized](Initialized(instance))

  /**
   * Sets the module to Initialized state.
   * @param instance Final module instance
   * @return
   */
  def initialize(instance: ModuleInstance) = throwInvalidState(tryInitialize(instance))
}
