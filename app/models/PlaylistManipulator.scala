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

package playlistManipulator {
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

import playlistManipulator._

