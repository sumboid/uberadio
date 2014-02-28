package models

import akka.actor._
import scala.concurrent.duration._
import scala.language.postfixOps

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

import java.util.Calendar
import java.text.SimpleDateFormat

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

package chatroom {
  object Messages {
    case class Join
    case class Quit
    case class Talk(id: String, text: String)

    case class Connected(enumerator:Enumerator[JsValue])
    case class CannotConnect(msg: String)
  }
}

import chatroom.Messages._

case class Message(name: String, text: String, time: String) {
  def replaceSpecSymbols(raw: String) = {
    raw.replaceAll("&", "&#38;").replaceAll("<", "&#60;").replaceAll(">", "&#62;")
  }

  def replaceMark(s: String) = {
    val bold = """(\*\*(\p{L}|\W|\d|\D)+\*\*|__(\p{L}|\W|\d|\D)+__)""".r
    val italic = """(\*(\p{L}|\W|\d|\D)+\*|_(\p{L}|\W|\d|\D)+_)""".r
    val spoiler = """(%%(\p{L}|\W|\d|\D)+%%|%%(\p{L}|\W|\d|\D)+%%)""".r

    var result = s
    result = bold.replaceAllIn(result, (m: Match) => m.matched.dropRight(2).drop(2).trim match {
      case "" => ""
      case x: String  => "<strong>" + x + "</strong>"
    })
    result = italic.replaceAllIn(result, (m: Match) => m.matched.dropRight(1).drop(1).trim match {
      case "" => ""
      case x: String  => "<em>" + x + "</em>"
    })
    result = spoiler.replaceAllIn(result, (m: Match) => m.matched.dropRight(2).drop(2).trim match {
      case "" => ""
      case x: String  => "<span class='spoiler'>" + x + "</span>"
    })

    result
  }

  val _name = replaceSpecSymbols(name).trim match { 
    case "" => "Anonymous"
    case x: String => x
  }

  val _text = replaceSpecSymbols(text).trim

  def get = JsObject(
                  Seq(
                    "user" -> JsString(_name),
                    "message" -> JsString(_text),
                    "time" -> JsString(time)
                  ))

  def empty = _text == ""
}

object History {
  val size = 10
  var history = List[Message]()

  def add(msg: Message) {
    println("Add: " + msg)
    history = history :+ msg
    println("Message added")
    if(history.size >= 10) {
      history = history.tail
    }
  }
}

object ChatRoom {
  implicit val timeout = Timeout(1 second)
  
  lazy val default = {
    val chatActor = Akka.system.actorOf(Props[ChatRoom])
    chatActor
  }

  def getSocket(id: String): scala.concurrent.Future[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    (default ? Join).map {   
      case Connected(enumerator) => 
        val iteratee = Iteratee.foreach[JsValue] ( 
          event => ((event \ "type").as[String]) match {
            case "message" => default ! Talk((event \ "name").as[String], 
                                             (event \ "text").as[String]) 
            case _ => {}
          }).map(_ => default ! Quit)

        val h = History.history map (x => Enumerator(x.get))
        (iteratee, (h :\ enumerator)((x, y) => x >- y))
    }
  }
}

class ChatRoom extends Actor {  
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  def receive = {  
    case Join => sender ! Connected(chatEnumerator)
    case Talk(id, text) => { notifyAll(id, text); println("talk: " + text) }
    case Quit => {}
  }
  
  def notifyAll(id: String, text: String) {
    println("Start notify")
    val today = Calendar.getInstance.getTime
    val curTimeFormat = new SimpleDateFormat("HH:mm:ss")
    val time = curTimeFormat.format(today).toString
    println("Creating message")
    val msg = Message(id, text, time)
    println("Message created")
    if(!msg.empty) {
      println("Pusing to channel: " + text)
      chatChannel.push(msg.get)
      History.add(msg)
    }
  }
}