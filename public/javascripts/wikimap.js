$(function() {
    // Container for the visualisation
    var $container = $("#wikimap-d3");

    // D3 VARIABLES
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

    // JQUERY VARS
    var $svg = $container.find("svg");
    var $tooltip = $container.find("#tooltip");
    var $slider = $("#range-slider");

    // MOUSE VARS
    var isMouseDown = false;
    var tooltipHasMouse = false;
    var mouseDownLocation = {x: 0, y: 0};
    var mouseLocation = {x: 0, y: 0};

    // GLOBE VARS
    var globeRotation = {x: 670, y: 400};
    var globeRotIncrement = 30;

    // OTHER
    var startYear = 2012, endYear = 2014;

    updateRotation();
    $.ajax("/getDateRange", {
        success: function(e) {
            var json = $.parseJSON(e);
            setupSlider(json.startDate.year, json.endDate.year);
        }
    });

    // LOADING DATA
    d3.json("/assets/res/world-110m.json", function(error, world) {
        if (error) throw error;

        svg.append("path")
            .datum(topojson.feature(world, world.objects.land))
            .attr("class", "land")
            .attr("fill", "#333")
            .attr("d", path);
    });
    updateWithRange();
    $svg.fadeIn(1000);

    // MOUSE/KEYBOARD EVENTS
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

        svg.select("#" + d.pointID)
            .attr("stroke-width", "5px");
    }

    function eventMouseOut(d) {
        $tooltip.fadeOut();

        svg.select("#" + d.pointID)
            .attr("stroke-width", "0px");
    }

    $tooltip.on("mouseover", function() {
        tooltipHasMouse = true;
    });

    $tooltip.on("mouseout", function() {
        tooltipHasMouse = false;
    });

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
            case 27:
                getNextPoints();
                break;
            case 189:
                projection.scale(projection.scale() / 1.2);
                break;
            case 187:
                projection.scale(projection.scale() * 1.2);
                break;
            default:
                break;
        }

        updateRotation();
    });


    // OTHER FUNCTIONS
    function updateWithRange() {
        console.log("Getting points for " + parseInt(startYear) + " to " + parseInt(endYear));

        $.ajax("/getEvents/1.1." + parseInt(startYear) + "/1.1." + parseInt(endYear), {
            type: "GET",
            success: function(e) {
                handleEvents($.parseJSON(e));
            },
            error: function(e) {
                console.log("Couldn't get events.");
                console.log(e);
            }
        });
    }

    function handleEvents(events) {
        console.log("Handling events");

        svg.selectAll(".points").remove();

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

        events.forEach(function(e, i) {
            e.pointID = "point" + i;

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
                .attr("id", e.pointID)
                .attr("fill", "#99ccff")
                .attr("stroke", "white")
                .attr("opacity", 0.5)
                .on("mouseover", function(e) {
                    eventMouseOver(e.geometry.coordinates[0][2]);
                })
                .on("mouseout", function(e) {
                    eventMouseOut(e.geometry.coordinates[0][2]);
                })
                .attr("d", path);
        });
    }

    function setupSlider(min, max) {
        function updateLabel() {
            $("#range-label").text(
                $slider.slider("values", 0) + " to " +
                $slider.slider("values", 1));
        }

        $slider.slider({
            range: true,
            min: min,
            max: max,
            values: [max - 1, max],
            slide: function(event, ui) {
                updateLabel();
                startYear = ui.values[0];
                endYear = ui.values[1];
            }
        });

        updateLabel();

        $("#range-button").click(updateWithRange);
    }

    function updateRotation() {
        if (globeRotation.y > 800) globeRotation.y = 800;
        if (globeRotation.y < 0)   globeRotation.y = 0;

        projection.rotate([λ(globeRotation.x), φ(globeRotation.y)]);
        svg.selectAll("path").attr("d", path);
    }

    function getText(e) {
        var date =
            "<div class='event-date'>" +
                e.date +
            "</div>";

        var location =
            "<div class='event-location'>" +
            e.location.name +
            "</div>";

        var desc =
            "<div class='event-desc'>" +
                formatDescription(e.desc, e.location.matchedName) +
            "</div>";

        return location + date + desc;
    }

    function formatDescription(desc, matched) {
        var linkRegex = /\[\[([^\[\|\]]*)\]\]/g;
        var linkWithBarRegex = /\[\[([^\[\|\]]*)\|([^\[\|\]]*)\]\]/g;

        var matchedFormatted = "";
        for (var i = 0; i < matched.length; i++) {
            matchedFormatted += matched.charAt(i) + (i == matched.length - 1 ? "" : "\\W?");
        }
        var matchedRegex = new RegExp("(" + matchedFormatted + ")", "i");

        return desc
            .replace(matchedRegex, "<b>$1</b>")
            .replace(linkRegex, "<a href='http://en.wikipedia.org/wiki/$1'>$1</a>")
            .replace(linkWithBarRegex, "<a href='http://en.wikipedia.org/wiki/$1'>$2</a>");
    }
});