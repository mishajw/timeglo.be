package controllers

import models.IndexContainer
import play.api.mvc._

import scala.io.Source

class Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def testGeocoder = Action { implicit request =>
    Source.fromURL("")

    Ok("")
  }

  def testIndexContainer = Action {
    val a = new IndexContainer

    Ok(a.getCompressedPosition("AbeL").toString)
  }

}
