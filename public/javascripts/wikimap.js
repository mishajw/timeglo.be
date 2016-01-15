
/* D3 VARIABLES */
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

var svg = d3.select("#wikimap-d3").append("svg")
    .attr("width", width)
    .attr("height", height);


/* OTHER GLOBALS */
var $svg = $("#wikimap-d3");
var isMouseDown = false;
var mouseDownLocation = {x: 0, y: 0};
var globeRotation = {x: 0, y: 0};


/* LOADING DATA */
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


/* MOUSE EVENTS */
$svg.on("mousemove", function(e) {
    if (!isMouseDown) return;

    globeRotation.x += e.clientX - mouseDownLocation.x;
    globeRotation.y += e.clientY - mouseDownLocation.y;

    mouseDownLocation.x = e.clientX;
    mouseDownLocation.y = e.clientY;

    updateRotation();
});

$svg.on("mousedown", function(e) {
    isMouseDown = true;

    mouseDownLocation.x = e.clientX;
    mouseDownLocation.y = e.clientY;
});

$svg.on("mouseup", function(e) {
    isMouseDown = false;
});

function eventMouseOver(d) {
    console.log(d);
}


/* OTHER FUNCTIONS */
function handleEvents(events) {
    console.log(events);

    var topojsonObject = {
        type: "Topology",
        objects: {
            events: {
                type: "MultiPoint",
                coordinates: []
            }
        },
        arcs: [],
        transform: {
            scale: [1, 1],
            translate: [0, 0]
        }
    };

    events.forEach(function(e) {
        // Set the topojson object to have details for this event
        topojsonObject.objects.events.coordinates = [[
            e.location.long,
            e.location.lat,
            // Inject into coordinates so we can get the data back later
            e
        ]];

        svg.append("path")
            .datum(topojson.feature(topojsonObject, topojsonObject.objects.events))
            .attr("class", "points")
            .attr("fill", "red")
            .on("mouseover", function(e) {
                // Take out the original information
                eventMouseOver(e.geometry.coordinates[0][2]);
            })
            .attr("d", path);
    });
}

function updateRotation() {
    projection.rotate([λ(globeRotation.x), φ(globeRotation.y)]);
    svg.selectAll("path").attr("d", path);
}
