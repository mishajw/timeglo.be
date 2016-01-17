# WikiMap

A map of events from Wikipedia, taken from pages like [this](en.wikipedia.org/wiki/January_1).

Using the [Wikipedia API](https://en.wikipedia.org/w/api.php), and mapping the event descriptions to locations using a (very) rich [collection of location names](http://download.geonames.org/export/dump/).

These are then mapped onto a globe using [D3](d3js.org) and [Topojson](https://github.com/mbostock/topojson) managed by [Play! Framework](https://www.playframework.com/), sized by events in that location and coloured by population size.

![Screenshot](res/images/screenshot.png?raw=true)
