package ru.dgolubets.scripting.amd

import java.io._
import java.util
import javax.script.{ScriptEngine, Bindings, ScriptContext}
import scala.collection.JavaConversions._

private object LoaderScriptContext {

  /**
   * Possible scope values.
   */
  object Scopes extends scala.Enumeration {
    type Scope = Value

    val Global = Value(ScriptContext.GLOBAL_SCOPE)
    val Engine = Value(ScriptContext.ENGINE_SCOPE)
    val Module = Value(50)
  }

  private val scopeList: util.List[Integer] = Scopes.values.map(_.id: Integer).toList
}

/**
 * Script context with an additional scope for module.
 * Define and require methods specific to the module are stored in that scope.
 *
 * @param globalScope Global scope bindings
 * @param engineScope Engine scope bindings
 * @param moduleScope Module scope bindings
 */
private class LoaderScriptContext(private var globalScope: Bindings,
                                  private var engineScope: Bindings,
                                  private var moduleScope: Bindings) extends ScriptContext {
  import LoaderScriptContext._

  /**
   * Creates a context with new module bindings and other bindings copied from the engine context.
   * @param engine Script engine
   * @return
   */
  def this(engine: ScriptEngine) =
    this(
      engine.getBindings(ScriptContext.GLOBAL_SCOPE),
      engine.getBindings(ScriptContext.ENGINE_SCOPE),
      engine.createBindings())

  private var reader: Reader = new InputStreamReader(System.in)

  private var writer: Writer = new PrintWriter(System.out, true)

  private var errorWriter: Writer = new PrintWriter(System.err, true)

  override def setReader(reader: Reader): Unit = this.reader = reader

  override def setWriter(writer: Writer): Unit = this.writer = writer

  override def setErrorWriter(writer: Writer): Unit = errorWriter = writer

  override def getWriter: Writer = writer

  override def getReader: Reader = reader

  override def getErrorWriter: Writer = errorWriter

  override def getScopes: util.List[Integer] = scopeList

  override def getAttributesScope(name: String): Int = {
    if (moduleScope.containsKey(name))
      Scopes.Module.id
    else if (engineScope.containsKey(name))
      Scopes.Engine.id
    else if (globalScope != null && globalScope.containsKey(name))
      Scopes.Global.id
    else -1
  }

  override def getBindings(scope: Int): Bindings = Scopes(scope) match {
    case Scopes.Module => moduleScope
    case Scopes.Engine => engineScope
    case Scopes.Global => globalScope
  }

  override def setBindings(bindings: Bindings, scope: Int): Unit =  Scopes(scope) match {
    case Scopes.Module =>
      if (bindings == null)
        throw new NullPointerException("Module scope cannot be null.")
      moduleScope = bindings
    case Scopes.Engine =>
      if (bindings == null)
        throw new NullPointerException("Engine scope cannot be null.")
      engineScope = bindings
    case Scopes.Global =>
      globalScope = bindings
  }

  override def getAttribute(name: String): AnyRef = getAttributesScope(name) match {
    case scope if scope > 0 => getAttribute(name, scope)
    case _ => null
  }

  override def getAttribute(name: String, scope: Int): AnyRef =
    getBindings(scope).get(name)

  override def setAttribute(name: String, value: scala.Any, scope: Int): Unit =
    getBindings(scope).put(name, value)

  override def removeAttribute(name: String, scope: Int): AnyRef = getBindings(scope).remove(name)
}
