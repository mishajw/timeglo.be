package wikimap.parser.simple

/**
  * Created by misha on 26/12/15.
  */
object SimpleLocationExtractor {

  def extractLocationMultiple(events: Seq[SimpleEvent]) =
    events.map(extractLocation)

  def extractLocation(event: SimpleEvent): Location = {



    LocationName("")
  }

  private def extractNounsFromText(eventDesc: String): Seq[String] = {
    stripLinkDetails(eventDesc)
      .split("\\W")
      .filter(_.startsWith("[A-Z]"))
      .toSeq
  }

  private def stripLinkDetails(eventDesc: String) =
    eventDesc.filterNot("[]".toSet)
}
