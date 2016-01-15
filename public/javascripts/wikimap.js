
var width = 960,
    height = 500;

var projection = d3.geo.orthographic()
    .scale(250)
    .translate([width / 2, height / 2])
    .clipAngle(90);

var path = d3.geo.path()
    .projection(projection);

var λ = d3.scale.linear()
    .domain([0, width])
    .range([-180, 180]);

var φ = d3.scale.linear()
    .domain([0, height])
    .range([90, -90]);

var svg = d3.select("body").append("svg")
    .attr("width", width)
    .attr("height", height);

svg.on("mousemove", function() {
    var p = d3.mouse(this);
    projection.rotate([λ(p[0]), φ(p[1])]);
    svg.selectAll("path").attr("d", path);
});

d3.json("/assets/res/world-110m.json", function(error, world) {
    if (error) throw error;

    svg.append("path")
        .datum(topojson.feature(world, world.objects.land))
        .attr("class", "land")
        .attr("d", path);
});

$.ajax("/getEvents/23.04.1999/23.04.2000", {
    type: "GET",
    success: function(e) {
        handleEvents($.parseJSON(e));
    },
    error: function(e) {
        console.log("Couldn't get events.");
        console.log(e);
    }
});

function handleEvents(events) {
    console.log(events);
}