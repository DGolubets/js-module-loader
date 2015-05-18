package ru.dgolubets.jsmoduleloader.conversions

import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer

import ru.dgolubets.jsmoduleloader.japi.ScriptModule
import ru.dgolubets.jsmoduleloader.{api, japi}

import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions

/**
 * Implicit conversions to and from Java.
 */
object JavaConversions {

  implicit def asJavaFuture[T](future: Future[T]): CompletableFuture[T] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val javaFuture = new CompletableFuture[T]
    future.map { module =>
      javaFuture.complete(module)
    } recover {
      case err => javaFuture.completeExceptionally(err)
    }
    javaFuture
  }

  implicit def asScalaFuture[T](future: CompletableFuture[T]): Future[T] = {
    val promise = Promise[T]()

    future.whenComplete(new BiConsumer[T, Throwable] {
      override def accept(t: T, u: Throwable): Unit =
        if(u != null) promise.failure(u)
        else promise.success(t)
    })

    promise.future
  }

  implicit def asScalaScriptModuleReader(reader: japi.readers.ScriptModuleReader): api.readers.ScriptModuleReader =
    new ScalaScriptModuleReaderWrapper(reader)

  implicit def asJavaScriptModuleReader(reader: api.readers.ScriptModuleReader): japi.readers.ScriptModuleReader =
    new JavaScriptModuleReaderWrapper(reader)

  implicit def asJavaScriptModule(module: api.ScriptModule): japi.ScriptModule =
    new ScriptModule(module.value)

}
