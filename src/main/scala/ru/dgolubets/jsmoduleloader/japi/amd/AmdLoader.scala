package ru.dgolubets.jsmoduleloader.japi.amd

import java.util.concurrent.CompletableFuture
import javax.script.ScriptEngine

import jdk.nashorn.api.scripting.NashornScriptEngine
import ru.dgolubets.jsmoduleloader._
import ru.dgolubets.jsmoduleloader.conversions.JavaConversions._
import ru.dgolubets.jsmoduleloader.japi.ScriptModule
import ru.dgolubets.jsmoduleloader.japi.readers.ScriptModuleReader

/**
 * AMD script loader for Java.
 *
 * @param loader Scala loader
 */
class AmdLoader private(loader: api.amd.AmdLoader)
  extends japi.AsyncScriptModuleLoader {

  import scala.concurrent.ExecutionContext.Implicits.global

  def this(moduleReader: ScriptModuleReader) = this(api.amd.AmdLoader(moduleReader))

  def this(scriptEngine: NashornScriptEngine, moduleReader: ScriptModuleReader) = this(api.amd.AmdLoader(scriptEngine, moduleReader))

  def requireAsync(moduleId: String): CompletableFuture[ScriptModule] = loader.requireAsync(moduleId).map(m => asJavaScriptModule(m))

  override def getEngine(): ScriptEngine = loader.engine
}
