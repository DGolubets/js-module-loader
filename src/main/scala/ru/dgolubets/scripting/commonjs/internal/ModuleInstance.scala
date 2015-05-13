package ru.dgolubets.scripting.commonjs.internal

/**
 * Module instance.
 *
 * Since there is no other Nashorn common type but AnyRef,
 * it's convenient to have a wrapper for it.
 *
 * @param value Javascript value.
 */
private[commonjs] case class ModuleInstance(value: AnyRef)