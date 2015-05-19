package ru.dgolubets.jsmoduleloader.japi

import java.util.concurrent.Callable
import javax.script.ScriptEngine

/**
 * Common script module loader interface.
 * Java compatible interface.
 */
trait ScriptModuleLoader {

    /**
     * Gets ScriptEngine used by loader.
     * @return
     */
    def getEngine(): ScriptEngine

    /**
     * Executes a code block that uses the script engine, synchronized with the loader.
     * Every action against the ScriptEngine or produced native script objects needs to be synchronized.
     *
     * @param code Synchronized code
     */
    def lock(code: Runnable)
}
