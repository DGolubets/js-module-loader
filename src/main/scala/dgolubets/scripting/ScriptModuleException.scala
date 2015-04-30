package dgolubets.scripting

/**
 * Base exception type for script loaders.
 * @param msg Message text
 * @param cause Inner exception
 */
case class ScriptModuleException(msg: String = null, cause: Throwable = null) extends Exception(msg, cause)
