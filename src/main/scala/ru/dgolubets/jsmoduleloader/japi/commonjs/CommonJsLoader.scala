package ru.dgolubets.jsmoduleloader.japi.commonjs

import java.util.concurrent.Callable
import javax.script.ScriptEngine

import jdk.nashorn.api.scripting.NashornScriptEngine
import ru.dgolubets.jsmoduleloader._
import ru.dgolubets.jsmoduleloader.conversions.JavaConversions
import ru.dgolubets.jsmoduleloader.conversions.JavaConversions._
import ru.dgolubets.jsmoduleloader.japi.ScriptModule
import ru.dgolubets.jsmoduleloader.japi.readers.ScriptModuleReader

/**
 * CommonJs script loader for Java.
 *
 * @param loader Scala loader
 */
class CommonJsLoader private(loader: api.commonjs.CommonJsLoader)
  extends japi.SyncScriptModuleLoader {

  def this(moduleReader: ScriptModuleReader) = this(api.commonjs.CommonJsLoader(moduleReader))

  def this(scriptEngine: NashornScriptEngine, moduleReader: ScriptModuleReader) = this(api.commonjs.CommonJsLoader(scriptEngine, moduleReader))

  override def require(moduleId: String): ScriptModule = loader.require(moduleId).get

  override def getEngine(): ScriptEngine = loader.engine

  override def lock(code: Runnable): Unit = loader.lock(code.run())
}
