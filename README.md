# [timeglo.be](https://timeglo.be)

A map of events from Wikipedia, taken from pages like [this](https://en.wikipedia.org/wiki/January_1) and [DBpedia](http://wiki.dbpedia.org/).

Using the [Wikipedia API](https://en.wikipedia.org/w/api.php), and mapping the event descriptions to locations using WikiMedia's [Geographical Coordinates](https://en.wikipedia.org/wiki/Wikipedia:WikiProject_Geographical_coordinates).

These are then mapped onto a globe using [D3](d3js.org) and [Topojson](https://github.com/mbostock/topojson) managed by [Play! Framework](https://www.playframework.com/), sized by events in that location and coloured by how recent the events are.

![Screenshot](public/images/screenshot.png?raw=true)

Developed by [Misha Wagner](https://github.com/mishajw), with the help of [Joe Groocock](https://github.com/frebib). Idea from Hoagy Cunningham.

