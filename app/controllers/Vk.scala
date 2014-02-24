package controllers


import scala.io.Source

import sys.process._
import java.net.URL
import java.io.File

import play.api._
import play.api.mvc._

import play.api.libs.json._

import models._

object VKAPI extends Controller {
  val redirectURI = "http://uberadio.tk/vk-connect"
  val authorizeURL = "https://oauth.vk.com/authorize"
  val accessTokenURL = "https://oauth.vk.com/access_token"
  val apiURL = "https://api.vk.com/method"
  val version = "5.11"

  val clientID = ID
  val clientSecret = SECRET
  val accessLevel = 8

  def vkCheck = Action { implicit request => 
    request.cookies.get("token") match {
      case Some(tokenCookie) => {
        val token = tokenCookie.value
        val response = try {
          Some(Source.fromURL(s"$apiURL/audio.get?access_token=$token&count=1&offset=0"))
        } catch {
          case _ => None
        } 

        response match {
          case Some(x) => Ok(Json.obj("state" -> "connected"))
          case None => Ok(Json.obj("state" -> "disconnected"))
        }
      }
      case None => Ok(Json.obj("state" -> "disconnected"))
    }
  }

  def vkConnect(code: String) = Action { implicit request => 
    if(code == "") {
        Redirect(s"$authorizeURL?client_id=$clientID&scope=$accessLevel&redirect_uri=$redirectURI&response_type=code")
      }
      else {
          val response = try {
            Some(Source.fromURL(s"$accessTokenURL?client_id=$clientID&client_secret=$clientSecret&code=$code&redirect_uri=$redirectURI"))
            } catch {
              case _ => None
            }

          response match {
            case Some(x) => {
              val json = Json.parse(x.mkString)
              val id = (json \ "user_id").as[Int]
              val token = (json \ "access_token").as[String]
              val expires = (json \ "expires_in").as[Int]


              Ok(views.html.vkConnect()).withCookies(Cookie("token", token, Some(expires)))
            }
            case None => Ok("error")
          }
        }
  }

  def personalTracks(count: Int, offset: Int) = Action { implicit request =>
    request.cookies.get("token") match {
      case None => Ok(Json.obj("type" -> "token error"))

      case Some(x) => {
        val token = x.value
        val response = try {
          println(s"$apiURL/audio.get?access_token=$token&count=$count")
          Some(Source.fromURL(s"$apiURL/audio.get?access_token=$token&count=$count&offset=$offset&v=$version"))
          } catch {
            case _ => None
          }
        response match {
          case Some(x) => {
            val json = Json.parse(x.mkString) \ "response" \ "items"
            val tracks = Json.obj("type" -> "huge success", "tracks" -> json.as[Seq[JsValue]].filter(x => (x \ "duration").as[Int] < 600))
            Ok(tracks)
          }
          case None => Ok(Json.obj("type" -> "api error"))
        }
      }
    }
  }

  def addTrack(id: String) = Action { implicit request =>
    request.cookies.get("token") match {
      case None => Ok(Json.obj("type" -> "token error"))
      case Some(x) => {
        val token = x.value
        val response = try {
          Some(Source.fromURL(s"$apiURL/audio.getById?access_token=$token&audios=$id"))
        } catch {
          case _ => None
        }
        
        response match {
          case Some(x) => {
            val json = Json.parse(x.mkString)
            val url = ((json \ "response")(0) \ "url").as[String]
            val title = ((json \ "response")(0) \ "title").as[String]
            val artist = ((json \ "response")(0) \ "artist").as[String]
            val time = ((json \ "response")(0) \ "duration").as[Int]
            Player.add(url.split("\\?", 2)(0), artist, title, time)
            Ok("")
          }
          case None => Ok("")
        }
      }
    }
  }

  def search(count: Int, offset: Int, q: String) = Action { implicit request =>
    request.cookies.get("token") match {
      case None => Ok(Json.obj("type" -> "token error"))

      case Some(x) => {
        val token = x.value
        val response = try {
          Some(Source.fromURL(s"$apiURL/audio.search?access_token=$token&count=$count&q=$q&search_own=1&v=$version"))
          } catch {
            case _ => None
          }
        response match {
          case Some(x) => {
            val json = (Json.parse(x.mkString) \ "response" \ "items")
            val tracks = Json.obj("type" -> "huge success", "tracks" -> json.as[Seq[JsValue]].filter(x => (x \ "duration").as[Int] < 600))
            Ok(tracks)
          }
          case None => Ok(Json.obj("type" -> "api error"))
        }
      }
    }   
  }
}
