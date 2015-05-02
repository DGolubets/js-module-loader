package ru.dgolubets.scripting.impl

import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.{CompletionHandler, AsynchronousFileChannel}
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import ru.dgolubets.scripting.ScriptModuleReader

import scala.concurrent._

object FileModuleReader {
  def apply(baseDir: File) = new FileModuleReader(baseDir)

  def apply(baseDir: String) = new FileModuleReader(new File(baseDir))
}

class FileModuleReader(baseDir: File) extends ScriptModuleReader {

  private val baseUri = baseDir.toURI

  /**
   * Reads module file text.
   * @param uri Module uri
   * @return
   */
  override def read(uri: URI): Future[String] = {
    val readPromise = Promise[String]()
    val stringBuilder = new StringBuilder()

    try {
      val fileUri = baseUri.resolve(uri)
      var path = Paths.get(fileUri)
      if(!path.endsWith(".js")){
        path = Paths.get(path.toString + ".js")
      }
      val channel = AsynchronousFileChannel.open(path)
      val buffer = ByteBuffer.allocate(1024)
      channel.read(buffer, 0, 0, new CompletionHandler[Integer, Int] {
        override def completed(result: Integer, totalRead: Int): Unit = {
          try {
            if (result == -1) {
              // finished
              channel.close()
              readPromise.success(stringBuilder.result())
            }
            else {
              buffer.flip()
              stringBuilder.append(new String(buffer.array(), 0, buffer.remaining(), StandardCharsets.UTF_8))
              buffer.clear()
              channel.read(buffer, totalRead + result, totalRead + result, this)
            }
          }
          catch {
            case err: Exception => readPromise.failure(err)
          }
        }

        override def failed(exc: Throwable, totalRead: Int): Unit = {
          readPromise.failure(exc)
        }
      })
    }
    catch {
      case err: Exception => readPromise.failure(err)
    }

    readPromise.future
  }
}
