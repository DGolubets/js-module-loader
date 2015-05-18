package ru.dgolubets.jsmoduleloader.japi

import javax.script.ScriptEngine

/**
 * Common script module loader interface.
 * Java compatible  interface.
 */
trait ScriptModuleLoader {

    /**
     * Gets ScriptEngine used by loader.
     * @return
     */
    def getEngine(): ScriptEngine
}
