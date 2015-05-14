package ru.dgolubets.jsmoduleloader.internal


import scala.concurrent._

private[jsmoduleloader] object Module {

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
  sealed case class Loaded[Definition](definition: Definition) extends NoErrorState

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
 * Generic module state machine.
 *
 * @param id Absolute module id
 * @tparam Definition Module definition type
 */
private[jsmoduleloader] case class Module[Definition](id: String) extends StateMachine[Module.State](Module.Empty()) {
  import Module._

  // promises for some module state changes that we need to observe in future
  private val _loaded = Promise[Unit]()
  private val _initializing = Promise[Unit]()
  private val _initialized = Promise[Unit]()


  /**
   * Called in a lock, when the state has been changed.
   * Override to react.
   *
   * @param newState New state
   * @param oldState Previous state
   */
  override protected def onStateChanged(newState: State, oldState: State): Unit = {
    super.onStateChanged(newState, oldState)

    newState match {
      case _: Loaded[Definition] => _loaded.success(())
      case _: Initializing => _initializing.success(())
      case _: Initialized => _initialized.success(())
      case err: Error =>
        _loaded.tryFailure(err.err)
        _initializing.tryFailure(err.err)
        _initialized.tryFailure(err.err)
      case _ =>
    }
  }

  /**
   * Future that completes after module is loaded or failed.
   * @return Future
   */
  def loaded = _loaded.future

  /**
   * Future that completes after module starts initializing or failed.
   * @return Future
   */
  def initializing = _initializing.future

  /**
   * Future that completes after module is initialized or failed.
   * @return Future
   */
  def initialized = _initialized.future

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
  def tryLoad(definition: Definition): Boolean = tryTransition[Loading, Loaded[Definition]](Loaded(definition))

  /**
   * Sets the module to an Loaded state.
   * @param definition Module definition
   */
  def load(definition: Definition) = throwInvalidState(tryLoad(definition))

  /**
   * Tries to set the module to Initializing state.
   * @param instance Initial module instance
   * @return
   */
  def tryStartInitializing(instance: ModuleInstance): Boolean = tryTransition[Loaded[Definition], Initializing](Initializing(instance))

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
