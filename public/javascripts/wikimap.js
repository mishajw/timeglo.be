$(function() {
    // Container for the visualisation
    var $container = $("#wikimap-d3");

    // D3 VARIABLES
    var width = $container.width(),
        height = $container.height();

    // GLOBE VARS
    var globeRotIncrement = 30;
    var globeRotation = {x: 670, y: 400};
    var desGlobeRotation = {x: globeRotation.x, y: globeRotation.y};
    var globeRotCatchUp = 0.3;

    var globeZoomIncrement = 1.2;
    var globeZoom = 1;
    var desGlobeZoom = globeZoom;
    var globeZoomCatchUp = 0.2;
    var globeZoomMax = 6000 / 400, globeZoomMin = 300 / 400;

    var globeMaxEvents = 0;
    var globeMaxPopulation = 0;
    var globeMaxPointSize = 50;
    var globeMinPointSize = 2;

    var projection = d3.geo.orthographic()
        .scale(globeZoom)
        .translate([width * 0.65, height / 2])
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
    var $infobox = $("#infobox");
    var $tooltip = $("#tooltip");
    var $startDate = $("input[name=start-date]");
    var $endDate = $("input[name=end-date]");

    // MOUSE VARS
    var isMouseDown = false;
    var mouseDownLocation = {x: 0, y: 0};
    var mouseLocation = {x: 0, y: 0};

    // OTHER
    var defaultYears = [2010, 2015];

    // Setup
    $startDate.val(defaultYears[0]);
    $endDate.val(defaultYears[1]);
    updateRotation();
    $.ajax("/getDateRange", {
        success: function(e) {
            var json = $.parseJSON(e);
            setupSlider(json.startDate.year, json.endDate.year);
        }
    });
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

        $tooltip.css({
            top: mouseLocation.y,
            left: mouseLocation.x
        });

        if (!isMouseDown) return;

        globeRotation.x += (e.clientX - mouseDownLocation.x) / (globeZoom);
        globeRotation.y += (e.clientY - mouseDownLocation.y) / (globeZoom);

        desGlobeRotation = {x: globeRotation.x, y: globeRotation.y};

        mouseDownLocation.x = e.clientX;
        mouseDownLocation.y = e.clientY;

        updateRotation();
    });

    $svg.on("mousedown", function(e) {
        isMouseDown = true;

        mouseDownLocation.x = e.clientX;
        mouseDownLocation.y = e.clientY;

        e.preventDefault();
        return false;
    });

    $svg.on("mouseup", function(e) {
        isMouseDown = false;

        e.preventDefault();
        return false;
    });

    $svg.on("mousedrag", function(e) {
        e.preventDefault();
        return false;
    });

    function eventMouseClick(d) {
        $infobox.html(getText(d));
    }

    function eventMouseOver(d) {
        var location = getLocationForEvents(d);
        $tooltip.html(location.name);
        $tooltip.fadeIn();

        svg.select("#" + d.pointID)
            .attr("stroke-width", "5px");
    }

    function eventMouseOut(d) {
        $tooltip.html("");
        $tooltip.hide();

        svg.select("#" + d.pointID)
            .attr("stroke-width", "1px");
    }

    $(document).keydown(function(e) {
        switch (e.which) {
            case 37:
                desGlobeRotation.x += globeRotIncrement / globeZoom;
                break;
            case 38:
                desGlobeRotation.y += globeRotIncrement / globeZoom;
                break;
            case 39:
                desGlobeRotation.x -= globeRotIncrement / globeZoom;
                break;
            case 40:
                desGlobeRotation.y -= globeRotIncrement / globeZoom;
                break;
            case 189:
                desGlobeZoom /= globeZoomIncrement;
                break;
            case 187:
                desGlobeZoom *= globeZoomIncrement;
                break;
            default:
                break;
        }

        updateTransformations();
    });


    // OTHER FUNCTIONS
    function updateWithRange() {
        var years = getScaledYears();

        $.ajax("/getEvents/1.1." + years[0] + "/1.1." + years[1], {
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

        var groupedEvents = {};
        events.forEach(function(e) {
            // Put it int a group
            var coords = [e.location.long, e.location.lat];
            if (groupedEvents[coords]) {
                groupedEvents[coords].push(e);
            } else {
                groupedEvents[coords] = [e];
            }

            // Format the date
            var split = e.date.split("-");
            e.date = new Date(parseInt(split[2]), parseInt(split[1]) - 1, parseInt(split[0]));
        });

        // Get the max population / amount of events
        globeMaxEvents = 0;
        for (var coord in groupedEvents) {
            if (groupedEvents[coord].length > globeMaxEvents) {
                globeMaxEvents = groupedEvents[coord].length;
            }
        }

        // Get the max population
        globeMaxPopulation = 0;
        events.forEach(function(e) {
            if (e.location.population > globeMaxPopulation) {
                globeMaxPopulation = e.location.population;
            }
        });

        var index = 0;
        for (var coord in groupedEvents) {
            var parsedCoords = coord.split(",");
            var eventObject = {
                events: groupedEvents[coord],
                pointID: "point" + (index ++)
            };

            // Set the topojson object to have details for this event
            topojsonObject.objects.events.coordinates = [[
                parseInt(parsedCoords[0]),
                parseInt(parsedCoords[1]),
                // Inject into coordinates so we can get the data back later
                eventObject
            ]];

            svg.append("path")
                .datum(topojson.feature(topojsonObject, topojsonObject.objects.events))
                .attr("class", "points")
                .attr("id", eventObject.pointID)
                .attr("fill", function(eo) {
                    try {
                        var population = eo.geometry.coordinates[0][2].events[0].location.population;
                        var value = (Math.sqrt(population) / Math.sqrt(globeMaxPopulation)) * 255;
                        return d3.rgb(255 - value, value, 0);
                    } catch (err) {
                        console.log(err);
                    }
                })
                .attr("stroke", "white")
                .attr("opacity", 0.9)
                .on("click", function(e) {
                    eventMouseClick(e.geometry.coordinates[0][2]);
                })
                .on("mouseover", function(e) {
                    eventMouseOver(e.geometry.coordinates[0][2]);
                })
                .on("mouseout", function(e) {
                    eventMouseOut(e.geometry.coordinates[0][2]);
                })
                .attr("d", path.pointRadius(function(d) {
                    try {
                        return ((d.geometry.coordinates[0][2].events.length / globeMaxEvents) *
                                (globeMaxPointSize - globeMinPointSize) + globeMinPointSize) *
                                (parseFloat(globeZoom));
                    } catch (err) {
                        return 1;
                    }
                }));
        }
    }

    function setupSlider(min, max) {
        function updateLabel() {
            var years = getScaledYears();
            $("#range-label").text(years[0] + " to " + years[1]);
        }

        updateLabel();

        $("#range-button").click(updateWithRange);
    }

    function updateRotation() {
        if (globeRotation.y > 800) globeRotation.y = 800;
        if (globeRotation.y < 0)   globeRotation.y = 0;

        projection.rotate([λ(globeRotation.x), φ(globeRotation.y)]);
        svg.selectAll("path").attr("d", path);
    }

    function updateZoom() {
        if (globeZoom > globeZoomMax) { globeZoom = globeZoomMax; desGlobeZoom = globeZoomMax; }
        if (globeZoom < globeZoomMin) { globeZoom = globeZoomMin; desGlobeZoom = globeZoomMin; }
        projection.scale(globeZoom * 400);
    }

    function getText(eventsObject) {
        var fullText = "";

        eventsObject.events.sort(function(a, b) {
            return a.date.getTime() - b.date.getTime();
        });

        eventsObject.events.forEach(function(e, i) {
            if (i == 0)
                fullText +=
                    "<div class='event-location'>" +
                        e.location.name +
                    "</div>";

            fullText +=
                "<div class='event-date'>" +
                    formatDate(e.date) +
                "</div>";

            fullText +=
                "<div class='event-desc'>" +
                    formatDescription(e.desc, e.location.matchedName) +
                "</div>";
        });

        return fullText;
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
            .replace(linkRegex, "<a href='http://en.wikipedia.org/wiki/$1' target='_blank'>$1</a>")
            .replace(linkWithBarRegex, "<a href='http://en.wikipedia.org/wiki/$1' target='_blank'>$2</a>");
    }

    function getScaledYears() {
        try {
            var val1 = $startDate.val();
            var val2 = $endDate.val();

            return [val1, val2];
        } catch (err) {
            return defaultYears;
        }
    }

    function updateTransformations() {
        var schedule = setInterval(function() {
            globeZoom += (desGlobeZoom - globeZoom) * globeZoomCatchUp;
            globeRotation.x += (desGlobeRotation.x - globeRotation.x) * globeRotCatchUp;
            globeRotation.y += (desGlobeRotation.y - globeRotation.y) * globeRotCatchUp;

            if (Math.abs(globeZoom - desGlobeZoom) < 0.01 &&
                Math.abs(globeRotation.x - desGlobeRotation.x) < 1 &&
                Math.abs(globeRotation.y - desGlobeRotation.y) < 1) clearInterval(schedule);

            updateZoom();
            updateRotation();
        });
    }

    function formatDate(d) {
        return d.getDate() + "/" + (d.getMonth() + 1) + "/" + d.getFullYear();
    }

    function getLocationForEvents(eo) {
        if (eo.events && eo.events.length > 0 && eo.events[0].location) {
            return eo.events[0].location;
        } else {
            return undefined;
        }
    }

    updateTransformations();
});