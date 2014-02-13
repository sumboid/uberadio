package models

import smpd._
import akka.actor._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Future

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

package playlist {
  object Messages {
    case class Init(id: String)
    case class Connected(enumerator:Enumerator[JsValue])
    case class Start
    case class Stop
    case class UpdatePlaylist
    case class UpdateCurrentSong
    case class Quit(id: String)
  }
}

import playlist.Messages._

case class TrackWrapper(track: Track) {
  def toJsObject = {
    JsObject (Seq ("position" -> JsNumber(track.pos),
                   "artist" -> JsString(track.artist), 
                   "title" -> JsString(track.title),
                   "time" -> JsNumber(track.time)))
  }
}

object Playlist {
  implicit val timeout = Timeout(5 second)

  lazy val default = {
    val playlistActor = Akka.system.actorOf(Props[Playlist])
    playlistActor
  }

  def removeShit(raw: String) = {
    raw.replaceAll("&", "&#38;").replaceAll("<", "&#60;").replaceAll(">", "&#62;")
  }

  def getSocket(id: String): scala.concurrent.Future[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    default ! UpdatePlaylist
    (default ? Init(id)) map { 
    case Connected(enumerator) => 
      val iteratee = Iteratee.foreach[JsValue] ( 
        event => ((event \ "type").as[String]) match {
          case "ping" => {}
          case _ => {}
        }).map(_ => default ! Quit(id))

        (iteratee, enumerator)
    }
  }
}

class Playlist extends Actor {
  val (enumerator, channel) = Concurrent.broadcast[JsValue]

  val mpd = SmartMPD("127.0.0.1", 6666)
  val idlempd = SmartMPD("127.0.0.1", 6666)

  def mpdListen: Unit = while(true) {
    val idlecmd = Idle(sub.Playlist() :: sub.Player() :: Nil)
    idlempd send idlecmd

    idlempd.response() match {
      case x: ConnectionErrorResponse => {}
      case x: ExternalErrorResponse => {}
      case IdleResponse(x) => x match {
        case Nil => println("I'm dead"); return
        case xs => xs foreach {
          case s: sub.Player => println("Update current song!"); self ! UpdateCurrentSong
          case s: sub.Playlist => println("Update playlist!"); self ! UpdatePlaylist
        }
      }
    }
  }

  var worker: Future[Unit] = Future()

  var members = 0

  def receive = {  
    case Init(id) => {
      val playlistCmd = new PlaylistInfo
      mpd.send(playlistCmd)

      val playlist =  mpd.response() match {
        case x: ConnectionErrorResponse => Nil
        case x: ExternalErrorResponse => Nil
        case x: PlaylistInfoResponse => x.playlist
      }

      val jsPlaylist = playlist map (TrackWrapper(_).toJsObject)
      val playlistEnumerator = Enumerator (JsObject(Seq("type" -> JsString("playlist"), "playlist" -> JsArray(jsPlaylist))))

      val cmd = new CurrentSong
      mpd.send(cmd)
      val currentSong = mpd.response() match {
        case x: ConnectionErrorResponse => JsObject(Seq("type" -> JsString("error")))
        case x: ExternalErrorResponse => JsObject(Seq("type" -> JsString("error")))
        case x: CurrentSongResponse => JsObject(Seq("type" -> JsString("currentsong"),
                                                    "track" -> TrackWrapper(x.track).toJsObject))
      }

      val currentSongEnumerator = Enumerator(currentSong)
      sender ! Connected(playlistEnumerator >- currentSongEnumerator >- enumerator)
      if(members == 0) worker = Future(mpdListen)
      members += 1
    }

    case Quit(id) => {
      println(members + "members")
      members -= 1
    }

    case UpdateCurrentSong => {
      val cmd = new CurrentSong
      mpd.send(cmd)
      mpd.response() match {
        case x: ConnectionErrorResponse => {}
        case x: ExternalErrorResponse => {}
        case x: CurrentSongResponse => channel.push(JsObject(Seq("type" -> JsString("currentsong"),
                                                                 "track" -> TrackWrapper(x.track).toJsObject)))
      }
    }

    case UpdatePlaylist => { 
      val cmd = new PlaylistInfo
      mpd.send(cmd)
      mpd.response() match {
        case x: PlaylistInfoResponse => {
          val jsPlaylistData = x.playlist map (TrackWrapper(_).toJsObject)
          val jsPlaylist = JsObject(Seq("type" -> JsString("playlist"), "playlist" -> JsArray(jsPlaylistData)))
          channel.push(jsPlaylist)
          self ! UpdateCurrentSong
        }
        case x: ConnectionErrorResponse => {}
        case x: ExternalErrorResponse => {}
      }      
    }

    case _ => {}
  }

  override def postStop {
    idlempd.send(new NoIdle)
  }
}