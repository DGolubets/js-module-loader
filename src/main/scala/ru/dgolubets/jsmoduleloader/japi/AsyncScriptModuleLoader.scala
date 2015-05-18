package ru.dgolubets.jsmoduleloader.japi

import java.util.concurrent.CompletableFuture

/**
 * Asynchronous script module loader interface.
 * Java compatible  interface.
 */
trait AsyncScriptModuleLoader extends ScriptModuleLoader {

    /**
     * Loads a module.
     * @param moduleId Module absolute id
     * @return Future module wrapper
     */
    def requireAsync(moduleId: String): CompletableFuture[ScriptModule]
}
