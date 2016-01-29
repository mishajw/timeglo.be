package backend.parser.location

import backend.retriever.wikipedia.APIArticleRetreiver
import backend.util.DB
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser
import org.eclipse.mylyn.wikitext.core.util.ServiceLocator

/**
  * Created by misha on 26/01/16.
  */
object LinkLocationExtractor {
  private val parser = new MarkupParser()

  private val linkRegex = "\\[\\[[^\\[\\]]*\\]\\]".r
  private val infoBoxStartRegex = "\\{\\{Infobox".r
  private val coordinateRegex = "\\| (coordinates|latitude|longitude) *=.*".r

  def run() = {
    // For every event...
    println(DB.getEvents
      .reverse
      .take(10)
      .map(e =>
        // Get every link...
        (linkRegex findAllIn e.description)
          // Format as a link...
          .map(s => s
            .substring(2, s.length - 2)
            .split("\\|").head
            .replace(" ", "_"))
          // Get the article...
          .map(APIArticleRetreiver.getTitle)
          .flatMap(a => {
            infoBoxStartRegex.findAllIn(a).matchData.foreach(md => {
              var bracketCount = 0
              val start = md.start
              var end = start

              do {
                a.substring(end, end + 2) match {
                  case "{{" => bracketCount += 1
                  case "}}" => bracketCount -= 1
                  case _ =>
                }

                end += 1[]
              } while (bracketCount > 0)
              end += 1

              println(a.substring(start, end))
            })

            ""
          })
      )
      .map(_.mkString(", "))
      .mkString("\n"))
  }
}
