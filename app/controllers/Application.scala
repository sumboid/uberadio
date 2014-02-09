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

  def upload = Action(parse.multipartFormData) { request =>
    request.body.file("track") map { track =>
      import java.io.File
      val filename = track.filename
      val contentType = track.contentType
      //track.ref.moveTo(new File(s"/tmp/picture/$filename"))
      println(filename + ": " + contentType)
      Ok("File uploaded")
    } getOrElse {
      Redirect(routes.Application.index).flashing(
        "error" -> "Missing file")
    }
  }
}