package controllers


import scala.io.Source

import sys.process._
import java.net.URL
import java.io.File

import play.api._
import play.api.mvc._

import play.api.libs.json._
import models._

object SoundCloudAPI extends Controller {
  val apiURL = "https://api.soundcloud.com"
  val clientID = ID

  def addTrack(id: String) = Action { implicit request =>
    val response = try {
      Some(Source.fromURL(s"$apiURL/tracks/$id.json?client_id=$clientID"))
    } catch {
      case _ => None
    }
        
    response match {
      case Some(x) => {
        val json = Json.parse(x.mkString)
        val url = (json \ "stream_url").as[String] + s"?client_id=$clientID"
        val title = (json \ "title").as[String]
        val artist = (json \ "user" \ "username").as[String]
        val time = (json \ "duration").as[Int] / 1000
        Player.add(url, artist, title, time)
        Ok("")
      }
      case None => Ok("")
    }
  }

  def search(count: Int, offset: Int, q: String) = Action { implicit request => Ok("")
    val response = try {
      println(s"$apiURL/tracks.json?client_id=$clientID&duration[from]=0&duration[to]=600000&filter=streamable&q=$q&limit=$count&offset=$offset")
      Some(Source.fromURL(s"$apiURL/tracks.json?client_id=$clientID&duration[from]=0&duration[to]=600000&filter=streamable&q=$q&limit=$count&offset=$offset"))
    } catch {
      case _ => None
    }
        
    response match {
      case Some(x) => {
        val json = Json.parse(x.mkString)
        Ok(x.mkString)
        val output = json.as[Seq[JsObject]]map { x =>
          Json.obj(
            "id" -> (x \ "id").as[Int],
            "title" -> (x \ "title").as[String],
            "artist" -> (x \ "user" \ "username").as[String],
            "duration" -> (x \ "duration").as[Int] / 1000
        )}
        Ok(Json.obj("type" -> "huge success", "tracks" -> output))
      }
      case None => Ok(Json.obj("type" -> "api error"))
    } 
  }
}
