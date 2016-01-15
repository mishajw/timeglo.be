
// Container for the visualisation
var $container = $("#wikimap-d3");

/* D3 VARIABLES */
var width = $container.width(),
    height = $container.height();

var projection = d3.geo.orthographic()
    .scale(height / 2)
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
var $svg = $container.find("svg");
var $tooltip = $container.find("#tooltip");
var isMouseDown = false;
var mouseDownLocation = {x: 0, y: 0};
var mouseLocation = {x: 0, y: 0};
var globeRotation = {x: 670, y: 400};
var globeRotIncrement = 30;
updateRotation();

/* LOADING DATA */
d3.json("/assets/res/world-110m.json", function(error, world) {
    if (error) throw error;

    svg.append("path")
        .datum(topojson.feature(world, world.objects.land))
        .attr("class", "land")
        .attr("d", path);
});

$.ajax("/getEvents/23.04.2014/23.04.2015", {
    type: "GET",
    success: function(e) {
        handleEvents($.parseJSON(e));
    },
    error: function(e) {
        console.log("Couldn't get events.");
        console.log(e);
    }
});


/* MOUSE/KEYBOARD EVENTS */
$svg.on("mousemove", function(e) {
    mouseLocation.x = e.clientX;
    mouseLocation.y = e.clientY;

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
    $tooltip.html(getText(d));
    $tooltip.show();
    $tooltip.css({
        left: mouseLocation.x - ($tooltip.width() / 2),
        top: mouseLocation.y
    });
}

function eventMouseOut(d) {
    $tooltip.hide();
    $.text("");
}

$(document).keydown(function(e) {
    switch (e.which) {
        case 37:
            globeRotation.x += globeRotIncrement;
            break;
        case 38:
            globeRotation.y += globeRotIncrement;
            break;
        case 39:
            globeRotation.x -= globeRotIncrement;
            break;
        case 40:
            globeRotation.y -= globeRotIncrement;
            break;
        default:
            break;
    }

    updateRotation();
});


/* OTHER FUNCTIONS */
function handleEvents(events) {
    console.log("Handling events");

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
                eventMouseOver(e.geometry.coordinates[0][2]);
            })
            .on("mouseout", function(e) {
                eventMouseOut(e.geometry.coordinates[0][2]);
            })
            .attr("d", path);
    });
}

function updateRotation() {
    projection.rotate([λ(globeRotation.x), φ(globeRotation.y)]);
    svg.selectAll("path").attr("d", path);
}

function getText(e) {
    var date =
        "<div class='event-date'>" +
            e.date +
        "</div>";

    var desc =
        "<div class='event-desc'>" +
            e.desc +
        "</div>";

    return date + desc;
}