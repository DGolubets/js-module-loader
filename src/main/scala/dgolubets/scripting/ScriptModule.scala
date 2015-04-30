package dgolubets.scripting

/**
 * Script module wrapper.
 * @param value Javascript reference to the module.
 *              It can be casted either to ScriptObjectMirror or java boxed primitives
 *              depending on the module result
 */
class ScriptModule(val value: AnyRef)