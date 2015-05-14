package ru.dgolubets.jsmoduleloader.internal

/**
 * Module instance.
 *
 * Since there is no other Nashorn common type but AnyRef,
 * it's convenient to have a wrapper for it.
 *
 * @param value Javascript value.
 */
private[jsmoduleloader] case class ModuleInstance(value: AnyRef)