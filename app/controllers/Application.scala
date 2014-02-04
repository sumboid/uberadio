package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._
import play.api.libs.iteratee._

import models._

import akka.actor._
import scala.concurrent.duration._

object Application extends Controller {
  def index = Action { implicit request =>
    val id = request.session.get("id").getOrElse("-1")
    if(id == "-1") {
      Ok(views.html.index("ppc")).withSession("id" -> System.nanoTime.toString) 
    } else {
      Ok(views.html.index("ppc"))
    }
  }

  def chatSocket = WebSocket.async[JsValue] { request  =>
    println(request.session.get("id").get)
    ChatRoom.getSocket(request.session.get("id").get)
  }

  def playlistSocket = WebSocket.async[JsValue] { request  =>
    Playlist.getSocket(request.session.get("id").get)
  }

  def chatRoomJs = Action { implicit request =>
    Ok(views.js.chat())
  }

  def playlistJs = Action { implicit request =>
    Ok(views.js.playlist())
  }
}