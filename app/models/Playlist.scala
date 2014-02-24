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
    case class UpdateInfo(id: Int, info: TrackInfo)
    case class Quit(id: String)
  }
}

import playlist.Messages._

case class TrackWrapper(track: Track) {
  def toJson = {
    Json.obj ("id"     -> track.id,
              "artist" -> track.artist, 
              "title"  -> track.title,
              "time"   -> track.time)
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

  def updateInfo(id: Int, info: TrackInfo) = default ! UpdateInfo(id, info)
}

class Playlist extends Actor {
  class PlaylistData(_playlist: List[Track] = Nil) {
    abstract class Action(action: String, track: Track) {
      def toJson = Json.obj("type"  -> action,
                            "track" -> TrackWrapper(track).toJson)
    }
    case class Remove(track: Track) extends Action("remove", track)
    case class Append(track: Track) extends Action("append", track)
    case class Update(track: Track) extends Action("update", track)

    case class Patch(commands: List[Action]) {
      //def toJson = Json.arr(commands)
    }

    var playlist: List[Track] = _playlist

    def update(_playlist: List[Track]) = {
      playlist = _playlist map (x => playlist find (_.id == x.id) match {
          case None => x
          case Some(y) => y 
      })
    }

    def updateTrack(id: Int, info: TrackInfo) = {
      playlist.zipWithIndex.find(x => x._1.id == id) match {
        case None => {}
        case Some(x) => {
          val track = x._1
          val updatedTrack = Track(track.id, track.pos, track.track, info.artist, info.title, track.genre, track.time.toString, info.time, track.file)
          playlist = playlist.updated(x._2, updatedTrack)
        }
      }
    }

    def toJson = {
      Json.obj( "type" -> "playlist",
                "playlist" -> (playlist map (TrackWrapper(_).toJson)))
    }
  }

  class CurrentSongData {
    var pos: Int = 0
    def update(i: Int) = {
      pos = i;
    }
    def toJson = {
      Json.obj("type" -> "currentsong",
               "id" -> pos)
    }
  }

  val (enumerator, channel) = Concurrent.broadcast[JsValue]

  lazy val mpd = SmartMPD("127.0.0.1", 6666)
  lazy val idlempd = SmartMPD("127.0.0.1", 6666)
  var end = false; // legshot

  def mpdListen: Unit = while(true) {
    val idlecmd = Idle(sub.Playlist() :: sub.Player() :: Nil)
    idlempd send idlecmd

    idlempd.response() match {
      case IdleResponse(x) => x match {
        case Nil => if(end) { println(end); println("I'm dead"); end = false; return }
        case xs => xs foreach {
          case s: sub.Player => println("current song"); self ! UpdateCurrentSong
          case s: sub.Playlist => println("playlist"); self ! UpdatePlaylist
        }
      }
      case _ => { println("error") }
    }
  }

  var worker: Future[Unit] = Future(mpdListen)

  val playlistData = new PlaylistData

  mpd.send (new PlaylistInfo)
  mpd.response() match {
    case PlaylistInfoResponse(x) => playlistData.update(x)
    case _ => Nil
  }

  val currentSongData = new CurrentSongData
  mpd.send(new CurrentSong)
  mpd.response() match {
    case CurrentSongResponse(x) => currentSongData.update(x.id)
    case _ => {}
  }

  def receive = {  
    case Init(id) => {
      sender ! Connected(Enumerator (playlistData.toJson) >- Enumerator(currentSongData.toJson) >- enumerator)
    }

    case Quit(id) => {
    }

    case UpdateCurrentSong => {
      val cmd = new CurrentSong
      mpd.send(cmd)
      mpd.response() match {
        case CurrentSongResponse(x) => {
          currentSongData.update(x.pos)
          channel.push(currentSongData.toJson)
        }
        case _ => {}
      }
    }

    case UpdatePlaylist => { 
      val cmd = new PlaylistInfo
      mpd.send(cmd)
      mpd.response() match {
        case PlaylistInfoResponse(x) => {
          playlistData.update(x)
          println(playlistData.playlist)
          channel.push(playlistData.toJson)
        }
        case _ => {println("playlist error")}
      }      
    }

    case UpdateInfo(id, info) => {
      playlistData.updateTrack(id, info)
      channel.push(playlistData.toJson)
    }
  }

  override def postStop {
    println("----- STOPPING -----")
    end = true
    idlempd.send(new NoIdle)
  }
}