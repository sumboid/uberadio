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

package player {
  object Messages {
    case class Add(uri: String)
    case class AddExternal(uri: String, artist: String, title: String, time: Int)
    case class TrackID(id: Int)
    case object Updated
  }
}

import player.Messages

case class TrackInfo(artist: String, title: String, time: Int)

object Player {
  implicit val timeout = Timeout(5 second)

  lazy val player = {
    val playerActor = Akka.system.actorOf(Props[Player])
    playerActor
  }

  def add(uri: String) = player ! Messages.Add(uri)
  def add(uri: String, artist: String, title: String, time: Int) = (player ? Messages.AddExternal(uri, artist, title, time)) map {
    case Messages.TrackID(id) => Playlist.updateInfo(id, TrackInfo(artist, title, time))
  }
}

class Player extends Actor {  
  val mpd = SmartMPD("127.0.0.1", 6666)
  val idlempd = SmartMPD("127.0.0.1", 6666)

  var queue: List[String] = Nil
  var end = false

  def mpdListen: Unit = while(true) {
    val idlecmd = Idle(sub.Database() :: Nil)
    idlempd send idlecmd

    idlempd.response() match {
      case IdleResponse(x) => x match {
        case Nil => if(end) { println(end); println("Player: I'm dead"); end = false; return }
        case xs => xs foreach {
          case s: sub.Database => self ! Messages.Updated
        }
      }
      case _ => println("Player: error")
    }
  }

  var worker: Future[Unit] = Future(mpdListen)


  def add(uri: String): Boolean = {
    var state = false

    mpd send Add(uri)
    mpd response() match {
      case x: AddResponse => println("added"); state = true
      case x: ConnectionErrorResponse => println("connection error response");
      case x: ExternalErrorResponse => println("external error response");
      case ErrorResponse(x) => println(x)
      case x: UnknownResponse => println("unknown response")
      case _ => println("shit happend")
    }

    if(!state) false

    mpd send smpd.Status()
    mpd response() match {
      case StatusResponse(x) => x foreach { 
        case stat.State(x) => x match {
          case stat.Play => {}
          case _ => { mpd send smpd.Play(); mpd wresponse() }
        }
      } 
      case _ => {}
    }

    true
  }

  def receive = {  
    case Messages.Add(uri: String) => {
      queue = queue :+ uri
      mpd send Update()
      mpd wresponse()
    }

    case Messages.AddExternal(uri: String, artist: String, title: String, time: Int) => {
      if(add(uri)) {
        mpd send new PlaylistInfo
        mpd response() match {
          case PlaylistInfoResponse(tracks) => {
            val id = tracks.filter( _.file.split("://", 2)(0) == "http" ).last.id
            sender ! Messages.TrackID(id)
          }
        }
      }
    }

    case Messages.Updated => {
      while (queue != Nil && add(queue.head)) {
        queue = queue.tail
      }
    }
  }
  override def postStop {
    println("SHIT")
    end = true;
    idlempd.send(new NoIdle)
  }
}