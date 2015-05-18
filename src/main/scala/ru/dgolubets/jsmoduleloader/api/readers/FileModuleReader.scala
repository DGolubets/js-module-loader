package ru.dgolubets.jsmoduleloader.api.readers

import java.io.{File, FileInputStream}
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.charset.Charset

import scala.concurrent._
import scala.util.Try

object FileModuleReader {
  def apply(baseDir: File, charset: Charset = Charset.defaultCharset()) = new FileModuleReader(baseDir, charset)

  def apply(baseDir: String): FileModuleReader = FileModuleReader(new File(baseDir))

  def apply(baseDir: String, charset: Charset): FileModuleReader = FileModuleReader(new File(baseDir), charset)
}

/**
 * Reads module files from a file system.
 * @param baseDir Base directory
 * @param charset Files character encoding
 */
class FileModuleReader(baseDir: File, charset: Charset) extends ScriptModuleReader {

  private val baseUri = baseDir.toURI

  protected def getModuleFile(uri: URI): File = {
    var fileUri = baseUri.resolve(uri).toString
    if(!fileUri.endsWith(".js")){
      fileUri += ".js"
    }
    new File(URI.create(fileUri))
  }

  /**
   * Reads module text.
   * @param uri Module URI
   * @return
   */
  override def read(uri: URI): Try[String] = Try {
    val stringBuilder = new StringBuilder
    val fin = new FileInputStream(getModuleFile(uri))
    try {
      val fc = fin.getChannel
      val buffer = ByteBuffer.allocate(1024)
      while (fc.read(buffer) > 0) {
        buffer.flip()
        val charBuffer = charset.decode(buffer)
        stringBuilder.appendAll(charBuffer.array(), 0, charBuffer.remaining())
        buffer.clear()
      }
    }
    finally {
      fin.close()
    }
    stringBuilder.result()
  }

  /**
   * Reads module file text asynchronously.
   * @param uri Module URI
   * @return
   */
  override def readAsync(uri: URI): Future[String] = {
    val readPromise = Promise[String]()
    val stringBuilder = new StringBuilder

    try {
      val path = getModuleFile(uri).toPath
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
              val charBuffer = charset.decode(buffer)
              stringBuilder.appendAll(charBuffer.array(), 0, charBuffer.remaining())
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
