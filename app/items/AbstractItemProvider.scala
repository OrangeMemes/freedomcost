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
import play.api.libs.json.{JsArray, Json}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait AbstractItemProvider {
  def GAME_NAME: String

  val isPf2 = GAME_NAME == "fc2"

  private val baseUrl = ConfigFactory.load("url").getString(GAME_NAME + ".baseUrl")
  private val itemsUrl = baseUrl + ConfigFactory.load("url").getString(GAME_NAME + ".items")

  var items: Seq[Item] = getUpdatedItems
  var rooms: Map[String, Seq[Room]] = Map.empty
  var modifiedTime: DateTime = _
  var updatedTime: DateTime = DateTime.now()
  var logUpdatedTime: Option[DateTime] = None

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

  def updateLogs(logdata: String): String = {
    logUpdatedTime = Some(DateTime.now)
    var updatedRooms: Seq[Room] = Seq.empty
    rooms = Map.empty

    logdata.split("\n").filter(_.contains("Получено сообщение: 2301"))
      .map(line => line.substring(line.indexOf("{"))).map(Json.parse).foreach { json =>
      val roomId = (json \ "id").as[Int].toString
      updatedRooms = updatedRooms :+ Room(roomId, Seq.empty, Rarity(0))
      (json \ "drop").as[JsArray].value.foreach { item =>
        val itemId = (item \ "item_id").as[Int].toString
        val rarity = (item \ "rarity").as[Int]
        val pm_map = (item \ "pm_map").asOpt[Int].map { pm_map =>
          var modesSeq: Seq[Int] = Seq.empty
          (0 to 31).foreach {i =>
            if ((pm_map & 1 << i) != 0)
              modesSeq = modesSeq :+ (i+1)
          }
          modesSeq
        }.getOrElse(Seq.empty)

        rooms = rooms.updated(itemId,
          rooms.getOrElse(itemId, Seq.empty) :+ Room(roomId, pm_map.map(Mode.apply), Rarity(rarity))
        )
      }
    }
    s"Обновлены комнаты ${updatedRooms.map(_.getText(isPf2)).mkString(", ")}"
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
        giftLevel = itemParams.get("gift_level"),
        rooms = () => {
          val roomsList = rooms.getOrElse(itemParams("id"), Seq.empty)
          roomsList.map { room =>
            s"${room.getText(isPf2)}" + {
              if (room.modes.nonEmpty)
                room.modes.map(_.getName).mkString(" (", ", ", ")")
              else
                ""
            } + {
              if (roomsList.map(_.rarity).distinct.length > 1) {
                " - " + room.rarity.getName
              } else ""
            }
          }.mkString(", ") + {
            if (roomsList.map(_.rarity).distinct.length == 1)
              s". ${roomsList.map(_.rarity).distinct.head.getName} предмет."
            else ""
          }
        }
      )
    } catch {
      case exception => Item("-1", exception.getMessage, "", "-1", Some(itemString), None, () => "")
    }
  }
}
