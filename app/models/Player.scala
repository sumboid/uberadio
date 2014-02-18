package models

import smpd._
import scala.language.postfixOps

class Player {
  val mpd = SmartMPD("127.0.0.1", 6666)

  def update = {
    mpd send Update()
    mpd send Idle(sub.Update() :: Nil)

    mpd wresponse()
    mpd response()
  }

  def add(uri: String) = {
    println("HERE")
    update
    mpd send Add(uri)
    mpd response() match {
      case x: AddResponse => println("added")
      case x: ConnectionErrorResponse => println("connection error response")
      case x: ExternalErrorResponse => println("external error response")
      case ErrorResponse(x) => println(x)
      case x: UnknownResponse => println("unknown response")
      case _ => println("shit happend")
    }

    mpd send Status()
    mpd response() match {
      case StatusResponse(x) => x foreach { 
        case stat.State(x) => x match {
          case stat.Play => {}
          case _ => { mpd send Play(); mpd wresponse() }
        }
      } 
      case _ => {}
    }
  }
}

object Player {
  def add(uri: String) = (new Player).add(uri)
}
