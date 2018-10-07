package items

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait AbstractItemProvider {
  def GAME_NAME: String

  private val baseUrl = ConfigFactory.load("url").getString(GAME_NAME + ".baseUrl")
  private val itemsUrl = baseUrl + ConfigFactory.load("url").getString(GAME_NAME + ".items")

  var items: Seq[Item] = getUpdatedItems
  var modifiedTime: DateTime = _
  var updatedTime: DateTime = DateTime.now()

  def updateItems(): Unit = {
    items = getUpdatedItems
    updatedTime = DateTime.now()
  }

  private def getUpdatedItems = {

    implicit val system: ActorSystem = ActorSystem("actors")
    implicit val materializer = ActorMaterializer()

    val requestFuture = Http().singleRequest(HttpRequest(uri = itemsUrl))
    val zipFuture = Await.result(requestFuture, Duration(60, TimeUnit.SECONDS)).entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    val zipByteArray = Await.result(zipFuture, Duration(60, TimeUnit.SECONDS)).toArray
    val zis = new ZipInputStream(new ByteArrayInputStream(zipByteArray))
    val entry = zis.getNextEntry
    modifiedTime = new DateTime(entry.getLastModifiedTime.toMillis)
    val fout = new ByteArrayOutputStream()
    val buffer = new Array[Byte](1024)
    Stream.continually(zis.read(buffer)).takeWhile(_ != -1).foreach(fout.write(buffer, 0, _))

    system.terminate()

    fout.toString.split("\r\n\r\n").drop(1).map(parseItem).filter(_.id != "-1")

  }

  private def parseItem(itemString: String): Item = {
    val itemParams = mutable.HashMap[String, String]()
    try {
      itemString.split("\r\n").foreach { field =>
        if (field.startsWith("#"))
          itemParams("id") = field.tail
        else {
          val (name, value) = field.span(_ != ':')
          itemParams(name) = value.tail
        }
      }
      Item(
        id = itemParams("id"),
        title = itemParams("title"),
        picture = baseUrl + itemParams("picture"),
        amountLimit = itemParams("amount_limit"),
        description = itemParams.get("description"),
        giftLevel = itemParams.get("gift_level")
      )
    } catch {
      case exception => Item("-1", exception.getMessage, "", "-1", Some(itemString), None)
    }
  }
}
