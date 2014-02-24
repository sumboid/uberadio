package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._

import models._

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

  def playerJs = Action { implicit request =>
    Ok(views.js.player())
  }

  def upload = Action(parse.multipartFormData) { request =>
    request.body.file("track") map { track =>
      import java.io.File
      //val filename = track.filename
      val filename = System.nanoTime.toString
      val contentType = track.contentType.getOrElse("none/none")

      val AllowTypes = """(audio/\*|video/ogg|application/ogg)""".r
      contentType.split("/") match {
        case Array("audio", x) => track.ref.moveTo(new File(s"/mnt/radio/music/$filename." + x)); Player.add(filename + "." + x)
        case Array("video", "ogg") => track.ref.moveTo(new File(s"/mnt/radio/music/$filename.ogg")); Player.add(filename + ".ogg")
        case Array("application", "ogg") => track.ref.moveTo(new File(s"/mnt/radio/music/$filename.ogg")); Player.add(filename + ".ogg")
        case _ => println("eat"); Redirect(routes.Application.index).flashing("error" -> "Missing file")
      }
      
      println(filename + ": " + contentType)
      Ok("File uploaded")
    } getOrElse {
      Redirect(routes.Application.index).flashing(
        "error" -> "Missing file")
    }
  }
}