package wikimap.util

import com.mongodb.casbah.Imports._

/**
  * Created by misha on 28/12/15.
  */
class DB {
  private val connection = MongoConnection()
  private val db = connection("wikimap")

  val events = db("events")
  val locations = db("locations")
}
