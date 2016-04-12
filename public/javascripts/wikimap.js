function Graph() {
//$(function() {
    // Container for the visualisation
    var $container = $("#wikimap-d3");

    $container.height($(window).height());

    // D3 VARIABLES
    var width = $container.width(),
        height = $container.height();
    var colors = d3.scale.category10();

    // GLOBE VARS
    var globeSize = Math.min(height, width) * 0.4;
    var globeSea = undefined;
    var globeRotation = {x: 670, y: 400};
    var desGlobeRotation = {x: globeRotation.x, y: globeRotation.y};
    var globeRotCatchUp = 0.3;

    var globeZoom = 1;
    var desGlobeZoom = globeZoom;
    var globeZoomCatchUp = 0.2;
    var globeZoomMax = 6, globeZoomMin = 1;

    var globeMaxEvents = 0;
    var globeDefaultMax = 10;
    var globeMaxPointSize = 50;
    var globeMinPointSize = 5;

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
    var $searchButton = $("#search-button");
    var $searchBox = $("#search-box");
    var $toggleButton = $("#sidebar-toggle");
    var $sidebar = $("#sidebar");

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
    d3.json("/assets/res/world-110m-0.6.json", function(error, world) {
        if (error) throw error;

        addSea();

        svg.append("path")
            .datum(topojson.feature(world, world.objects.countries))
            .attr("stroke", "#222")
            .attr("stroke-width", "0.5px")
            .attr("class", "land")
            .attr("fill", "#333")
            .attr("d", path);

        updateEvents();
        $svg.fadeIn(1000);

        updateTranslation();

        if (isMobile()) {
            $toggleButton.click();
        }
    });

    // MOUSE/KEYBOARD EVENTS
    $svg.on("mousemove touchmove", function(e) {
        mouseLocation = getEventLocation(e);

        $tooltip.css({
            top: mouseLocation.y - $tooltip.height() / 2,
            left: mouseLocation.x + 15
        });

        if (!isMouseDown) return;

        globeRotation.x += (mouseLocation.x - mouseDownLocation.x) / (globeZoom);
        globeRotation.y += (mouseLocation.y - mouseDownLocation.y) / (globeZoom);

        desGlobeRotation = {x: globeRotation.x, y: globeRotation.y};

        mouseDownLocation = getEventLocation(e);

        updateRotation();
    });

    $svg.on("mousedown touchstart", function(e) {
        isMouseDown = true;

        mouseDownLocation = getEventLocation(e);

        e.preventDefault();
        return false;
    });

    $svg.on("mouseup touchend", function(e) {
        isMouseDown = false;

        e.preventDefault();
        return false;
    });

    $svg.on("mousedrag", function(e) {
        e.preventDefault();
        return false;
    });

    function eventMouseClick(d) {
        showSidebar();

        $infobox.html(getText(d));
    }

    function getEventLocation(e) {
        if (e.type.substring(0, 5) == "mouse") {
            return {
                x: e.clientX,
                y: e.clientY
            };
        } else {
            return {
                x: e.originalEvent.touches[0].clientX,
                y: e.originalEvent.touches[0].clientY
            };
        }
    }

    function eventMouseOver(d) {
        var location = getLocationForEvents(d);

        $tooltip.html(
            location.name +
                "<span class='event-amount'>" +
                d.events.length + (d.events.length != 1 ? " events" : " event") +
                (
                    location.type && location.type != "" ?
                    " | " + capitalise(location.type) : ""
                ) +
            "</span>");
        $tooltip.show();

        svg.select("#" + d.pointID)
            .attr("stroke-width", "5px");
    }

    function eventMouseOut(d) {
        $tooltip.hide();

        svg.select("#" + d.pointID)
            .attr("stroke-width", "1px");
    }

    $searchButton.click(function() {
        if (isMobile()) {
            hideSidebar();
        }

        updateEvents();
    });

    $searchBox.keypress(function(e) {
        if (e.which == 13) $searchBox.click();
    });

    var updateTranslation = function() {
        var newTranslate = [getWidthMiddle(), height / 2];
        projection.translate(newTranslate);
        svg.selectAll("path").attr("d", path);
        globeSea
            .attr("cx", newTranslate[0])
            .attr("cy", newTranslate[1]);
    };

    function showSidebar() {
        if (!document.getElementById("sidebar-toggle").checked) {
            $toggleButton.click();
        }
    }

    function hideSidebar() {
        if (document.getElementById("sidebar-toggle").checked) {
            $toggleButton.click();
        }
    }

    $toggleButton.click(updateTranslation);

    $(function() {
        var zoomListener = d3.behavior.zoom()
            .scaleExtent([globeZoomMin, globeZoomMax])
            .on("zoom", function() {
                desGlobeZoom = d3.event.scale;
                updateTransformations();
            });

        svg.call(zoomListener)
    });

    $(window).resize(updateTranslation);

    function updateEvents() {
        var keywords = sanitise($searchBox.val());
        var years = getScaledYears().map(sanitise);

        $.ajax("/search/1.1." + years[0] + "/31.12." + years[1] + "/" + keywords, {
            type: "GET",
            success: function(e) {
                handleEvents(JSON.parse(e));
            },
            error: function(e) {
                console.log("Couldn't get events");
                console.log(e);
            }
        });
    }

    function addSea() {
        globeSea =
            svg.append("circle")
                .attr("cx", getWidthMiddle())
                .attr("cy", height * 0.5)
                .attr("r", globeSize)
                .attr("fill", "#EEE")
                .attr("stroke", "#DDD")
                .attr("stroke-width", "0.5");
    }

    function handleEvents(events) {
        svg.selectAll(".points").remove();

        if (events.length == 0) {
            $infobox.html("No events");
            return;
        }

        $infobox.html("Click on a point to see events");

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
            var split = e.date.split(".");

            e.date = new Date();
            e.date.setDate(split[0]);
            e.date.setMonth(parseInt(split[1]) - 1);
            e.date.setFullYear(split[2]);
        });

        // Convert it into a list:
        groupedEventsList = [];
        for (coord in groupedEvents) {
            groupedEventsList.push(groupedEvents[coord]);
        }
        // Sort that list by events:
        groupedEventsList.sort(function(a, b) {
            return b.length - a.length;
        });

        // Get the max amount of events
        globeMaxEvents = groupedEventsList[0].length;

        for (var i = 0; i < groupedEventsList.length; i++) {
            var group = groupedEventsList[i];

            var eventObject = {
                events: group,
                pointID: "point" + i
            };

            // Set the topojson object to have details for this event
            topojsonObject.objects.events.coordinates = [[
                parseInt(group[0].location.long),
                parseInt(group[0].location.lat),
                // Inject into coordinates so we can get the data back later
                eventObject
            ]];

            svg.append("path")
                .datum(topojson.feature(topojsonObject, topojsonObject.objects.events))
                .attr("class", "points")
                .attr("id", eventObject.pointID)
                .attr("fill", function(eo) {
                    var type = getLocationForEvents(eo.geometry.coordinates[0][2]).type;
                    return colors(type);
                })
                .attr("stroke", "white")
                .attr("opacity", 0.9)
                .on("click", function(e) {
                    eventMouseClick(e.geometry.coordinates[0][2]);
                })
                .on("touchend", function(e) {
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
                        var max = globeMaxEvents > globeDefaultMax ? globeMaxEvents : globeDefaultMax;
                        var min = 1;
                        var amount = d.geometry.coordinates[0][2].events.length;
                        var amountScale = (amount - min) / (max - min);
                        var unscaledSize = ((amountScale) * (globeMaxPointSize - globeMinPointSize)) + globeMinPointSize;
                        return unscaledSize * parseFloat(globeZoom) * (globeSize / 400);
                    } catch (err) {
                        // Not a point
                        return 1;
                    }
                }));
        }
    }

    function setupSlider() {
        function updateLabel() {
            var years = getScaledYears();
            $("#range-label").text(years[0] + " to " + years[1]);
        }

        updateLabel();

        $("#range-button").click(updateEvents);

        $startDate.keypress(function(e) {
            if (e.which == 13) updateEvents();
        });

        $endDate.keypress(function(e) {
            if (e.which == 13) updateEvents();
        });

        $searchBox.keypress(function(e) {
            if (e.which == 13) updateEvents();
        });
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
        if (projection) projection.scale(globeZoom * globeSize);
        if (globeSea) globeSea.attr("r", globeZoom * globeSize);
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

            fullText += "<div class='event-container'>";

            fullText +=
                "<div class='event-date'>" +
                    formatDate(e.date) +
                "</div>";

            fullText +=
                "<div class='event-desc'>" +
                    formatDescription(e.desc, e.wiki_page) +
                "</div>";

            fullText += "</div>";
        });

        return fullText;
    }

    function formatDescription(desc, wikiPage) {
        var linkRegex = /\[\[([^\[\|\]]*)\]\]/g;
        var linkWithBarRegex = /\[\[([^\[\|\]]*)\|([^\[\|\]]*)\]\]/g;

        if (desc.length > 140) {
            desc = desc.substring(0, 300).trim() + "..."
        }

        if (wikiPage) {
            desc =
                "<a href='" + wikiPage + "' class='wiki-page' target='_blank'>" +
                    wikiPage
                        .replace("http://en.wikipedia.org/wiki/", "")
                        .replace(/_/g, " ")
                        .replace(/\?.+/g, "") +
                "</a>" + desc;
        }

        return desc
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
        return d.getDate() + "/" + (d.getMonth() + 1) + "/" + (d.getFullYear() > 0 ? d.getFullYear() : -d.getFullYear() + " BC");
    }

    function getLocationForEvents(eo) {
        if (eo.events && eo.events.length > 0 && eo.events[0].location) {
            return eo.events[0].location;
        } else {
            return undefined;
        }
    }

    function formatNumber(x) {
        if (x > 1000000000) {
            return (x / 1000000000) .toFixed(2) + " billion";
        } else if (x > 1000000) {
            return (x / 1000000)    .toFixed(2) + " million";
        } else if (x > 1000) {
            return (x / 1000)       .toFixed(2) + " thousand";
        }

        return x;
    }

    function capitalise(s) {
        return s.charAt(0).toUpperCase() + s.slice(1);
    }

    function sanitise(s) {
        return s.replace(/[^A-Za-z0-9\- ]/, "");
    }

    function getWidthMiddle() {
        width = $container.width();

        if (document.getElementById("sidebar-toggle").checked && !isMobile()) {
            return width * 0.65;
        } else {
            return width * 0.5;
        }
    }

    function isMobile() {
        return $(window).width() < 850;
    }

    updateTransformations();

    // PUBLIC FUNCTIONS

    this.updateScale = function() {
        width = $container.width();
        height = $container.height();
        projection.translate([getWidthMiddle(), height / 2]);
    }
}

var graph;

$(function() {
    graph = new Graph();
});
