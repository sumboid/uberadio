package models

import smpd._
import scala.language.postfixOps

class Player {
  val mpd = SmartMPD("127.0.0.1", 6666)

  def update = {
    mpd send Update()
    mpd send Idle(sub.Update() :: Nil)

    mpd response() match {
      case x: ConnectionErrorResponse => {}
      case x: ExternalErrorResponse => {}
      case x: UpdateResponse => {}
    }

    mpd response() match {
      case x: ConnectionErrorResponse => {}
      case x: ExternalErrorResponse => {}
      case x: IdleResponse => {}
    }
  }

  def add(uri: String) = {
    update
    mpd send Add(uri)
    mpd response() match {
      case x: ConnectionErrorResponse => {}
      case x: ExternalErrorResponse => {}
      case x: AddResponse => {}
    }
  }
}

object Player {
  def add(uri: String) = (new Player).add(uri)
}
