package ru.dgolubets.jsmoduleloader.japi

/**
 * Synchronous script module loader interface.
 * Java compatible  interface.
 */
trait SyncScriptModuleLoader extends ScriptModuleLoader {

    /**
     * Loads a module.
     * @param moduleId Module absolute id
     * @return
     */
    def require(moduleId: String): ScriptModule
}
