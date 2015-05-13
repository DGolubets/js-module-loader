package ru.dgolubets.scripting.amd.internal

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

/**
 * Module instance.
 *
 * It allows synchronous access to uninitialized module value,
 * which is required in case of circular dependencies.
 *
 * After instance is initialized, both values are the same.
 * Indeed when module uses 'exports', they are the same from the start.
 *
 * @param raw Uninitialized javascript value.
 *            It can be either {} if module uses exports or undefined.
 * @param ready Future initialized javascript value
 */
private[amd] class ModuleInstance(raw: AnyRef, ready: Future[AnyRef]){

  // I guess it's enough to use volatile here
  @volatile private var _value = raw

  def value = _value
  val initialized = ready.map{ ref =>
    this._value = ref
    this
  }
}

private[amd] object ModuleInstance{
  def apply(raw: AnyRef, ready: Future[AnyRef]) = new ModuleInstance(raw, ready)
}