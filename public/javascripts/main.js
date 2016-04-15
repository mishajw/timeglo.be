function Graph() {
//$(function() {
    // Container for the visualisation
    var $container = $("#wikimap-d3");

    $container.height($(window).height());

    // D3 VARIABLES
    var width = $container.width(),
        height = $container.height();

    // GLOBE VARS
    var globeSize = Math.min(height, width) * 0.4;
    var globeSea = undefined;
    var globeRotation = {x: parseInt(width) / 2, y: parseInt(height) * 0.7};
    var desGlobeRotation = {x: globeRotation.x, y: globeRotation.y};
    var globeRotCatchUp = 0.3;

    var globeZoom = 1;
    var desGlobeZoom = globeZoom;
    var globeZoomCatchUp = 0.2;
    var globeZoomMax = 6, globeZoomMin = 1;

    var globeMaxEvents = 0;
    var globeDefaultMax = 10;
    var globeMaxPointSize = 50;
    var globeMinPointSize = 4;
    var globeBusyThreshold = 3000;
    var globeMinPointSizeBusy = 2;

    var maxTimestamp;
    var minTimestamp;
    var continueCount = 0;
    var touchCount = 0;

    var lastNotify;
    var firstLoad = true;

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

    // MOUSE VARS
    var isMouseDown = false;
    var mouseDownLocation = {x: 0, y: 0};
    var mouseLocation = {x: 0, y: 0};

    var defaultYears = [2013, 2016];

    try {
        // Get the years from the URL, or use the defaults
        if (urlStart && urlEnd) {
            defaultYears = [parseInt(urlStart), parseInt(urlEnd)];
        }

        // If the URL has set the search term, fill the box
        if (urlSearch) {
            $searchBox.val(unsanitise(urlSearch));
        }
    } catch (e) {}

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
        if (touchCount > 1) {
            return;
        }

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
        //touchCount ++;

        mouseDownLocation = getEventLocation(e);

        e.preventDefault();
        return false;
    });

    $svg.on("mouseup touchend", function(e) {
        isMouseDown = false;
        //touchCount --;

        e.preventDefault();
        return false;
    });

    $svg.on("touchstart", function() { touchCount ++; });
    $svg.on("touchend",   function() { touchCount --; });

    function eventMouseClick(d) {
        if (touchCount > 1) {
            return;
        }

        showSidebar();

        $("#infobox-container").scrollTop(0);
        $infobox.html(getText(d));

        $(".continue-button")
            .click(function() {
                var $this = $(this);
                var number = $this.attr("id").replace("continue", "");
                $("#hidden" + number).show();
                $("#ellipsis" + number).hide();
                $this.hide();
            });
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
        notify("Searching...");

        if (isMobile()) {
            hideSidebar();
        }

        updateEvents();
    });

    $searchBox.keypress(function(e) {
        if (e.which == 13) $searchBox.click();
    });

    $(window).on("popstate", function () {
        location.reload();
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
        
        if (years[0] == "" || years[1] == "") {
            notify("Not valid dates", "danger");
            return;
        }

        if (firstLoad) {
            firstLoad = false;
        } else {
            updateSearchTerms(years[0], years[1], keywords);
        }

        updateShareButtons(years[0], years[1], keywords);

        $.ajax("/search/1.1." + years[0] + "/31.12." + years[1] + "/" + keywords, {
            type: "GET",
            success: function(e) {
                handleEvents(JSON.parse(e));
            },
            error: function(e) {
                var errorMessage;

                try {
                    var json = $.parseJSON(e.responseText);
                    errorMessage = json.error;
                } catch (e) {
                    errorMessage = "Error from server"
                }

                notify(errorMessage, "danger")
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
            notify("No events found", "danger");
            return;
        }

        if (events.length < 7000) {
            notify("Loaded " + events.length + " events");
        } else {
            notify("Found too many events, only showing some", "warning");
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
            // Put it in a group
            var key = e.location.name;
            if (groupedEvents[key]) {
                groupedEvents[key].push(e);
            } else {
                groupedEvents[key] = [e];
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

        // Get the timestamp ranges
        minTimestamp = getAverageTimestamp(groupedEventsList[0]);
        maxTimestamp = getAverageTimestamp(groupedEventsList[0]);
        groupedEventsList.forEach(function(es) {
            var timestamp = getAverageTimestamp(es);
            if (minTimestamp > timestamp) minTimestamp = timestamp;
            if (maxTimestamp < timestamp) maxTimestamp = timestamp;
        });

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
                    var timestamp = getAverageTimestamp(eo.geometry.coordinates[0][2].events);
                    var colour = (timestamp - minTimestamp) / (maxTimestamp - minTimestamp);
                    return scaleToTrafficLights(colour)
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
                        var minSize = events.length > globeBusyThreshold ? globeMinPointSizeBusy : globeMinPointSize;
                        var min = 1;
                        var amount = d.geometry.coordinates[0][2].events.length;
                        var amountScale = (amount - min) / (max - min);
                        var unscaledSize = ((amountScale) * (globeMaxPointSize - minSize)) + minSize;
                        return unscaledSize * Math.sqrt(parseFloat(globeZoom) * (globeSize / 400));
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
            if (a.date.getFullYear() != b.date.getFullYear()) {
                return a.date.getFullYear() - b.date.getFullYear();
            } else if (a.date.getMonth() != b.date.getMonth()) {
                return a.date.getMonth() - b.date.getMonth();
            } else {
                return a.date.getDate() - b.date.getDate();
            }
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
                    formatDate(e.date, e.datePrecision) +
                "</div>";

            fullText +=
                "<div class='event-desc'>" +
                    formatDescription(e.desc, e.wikiPage) +
                "</div>";

            fullText += "</div>";
        });

        return fullText;
    }

    function formatDescription(desc, wikiPage) {
        var linkRegex = /\[\[([^\[\|\]]*)\]\]/g;
        var linkWithBarRegex = /\[\[([^\[\|\]]*)\|([^\[\|\]]*)\]\]/g;

        //if (desc.length > 140) {
        //    desc = desc.substring(0, 300).trim() + "..."
        //}

        desc = desc
            .replace(/\{\{convert\|([\d.]+)\|([^\{\}\|]+)\|([^\{\}\|]+)(\|[^\{\}\|]+)?\}\}/g, "$1 $2")
            .replace(/\{\{([^\{\}\|]+)\|([^\{\}\|]+)(\|([^\{\}]+))\}\}/g, "$1 $2")
            .replace(linkRegex, "<a href='http://en.wikipedia.org/wiki/$1' target='_blank'>$1</a>")
            .replace(linkWithBarRegex, "<a href='http://en.wikipedia.org/wiki/$1' target='_blank'>$2</a>")
        ;

        desc = concatDescription(desc);

        if (wikiPage) {
            desc =
                "<a href=\"" + wikiPage + "\" class='wiki-page' target='_blank'>" +
                wikiPage
                    .replace("http://en.wikipedia.org/wiki/", "")
                    .replace(/_/g, " ")
                    .replace(/\?.+/g, "") +
                "</a>" + desc;
        }

        return desc;
    }

    function concatDescription(desc) {
        var maxAmount = 200 +
            (desc.length - desc.replace(/<[^\<\>]+>/g, "").length);

        if (desc.length < maxAmount)
            return desc;

        var closestSpace = maxAmount;
        while (closestSpace < desc.length && desc.charAt(closestSpace) != ' ') {
            closestSpace ++;
        }

        var descEnd = desc.substring(closestSpace, desc.length);
        var endTagIndex = descEnd.indexOf(">") + closestSpace;
        var startTagIndex = descEnd.indexOf("<") + closestSpace;
        if (endTagIndex != -1 && startTagIndex != -1 && endTagIndex < startTagIndex) {
            if (desc.charAt(startTagIndex + 1) == "/") {
                closestSpace = startTagIndex + 3;
            } else {
                closestSpace = endTagIndex;
            }
        } else if (endTagIndex != -1 && startTagIndex == -1) {
            return desc;
        }

        var descStart = desc.substring(0, closestSpace);
            descEnd = desc.substring(closestSpace, desc.length);

        continueCount ++;

        return descStart +
            "<span id='ellipsis" + continueCount + "'>...</span>" +
            "<a class='continue-button' id='continue" + continueCount + "'>continue</a>" +
            "<span class='continue-text' id='hidden" + continueCount + "'>" + descEnd + "</span>"
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

    function formatDate(date, precision) {
        var d = date.getDate();
        var m = date.getMonth() + 1;
        var y = date.getFullYear() > 0 ? date.getFullYear() : -date.getFullYear() + " BC";

        switch (precision) {
            case "PreciseToDate":
                return d + "/" + m + "/" + y;
            case "PreciseToMonth":
                return m + "/" + y;
            case "PreciseToYear":
                return y;
            case "NotPrecise":
                return "N/A";
        }
    }

    function getLocationForEvents(eo) {
        if (eo.events && eo.events.length > 0 && eo.events[0].location) {
            return eo.events[0].location;
        } else {
            return undefined;
        }
    }


    function capitalise(s) {
        return s.charAt(0).toUpperCase() + s.slice(1);
    }

    function sanitise(s) {
        return s
            .replace(/[^A-Za-z0-9\- ]/g, "")
            .replace(/ /g, "_");
    }

    function unsanitise(s) {
        return s.replace(/_/g, " ");
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

    function getAverageTimestamp(es) {
        var total = 0;
        es.forEach(function(e) { total += e.date.getTime(); });
        return total / es.length;
    }

    function scaleToTrafficLights(i) {
        var colorScale = 210;

        if (i < 0.5) {
            return d3.rgb(colorScale, (i * 2) * colorScale, 0);
        } else {
            return d3.rgb((1 - (i * 2)) * colorScale, colorScale, 0);
        }
    }

    function notify(message, type) {
        if (!type) type = "info";

        if (lastNotify) lastNotify.close();

        lastNotify = $.notify(message, {
            placement: {
                from: isMobile() ? "bottom" : "top",
                align: "right"
            },
            type: type,
            animate: {
                enter: 'animated fadeIn',
                exit: 'animated fadeOut'
            },
            delay: 3000,
            allow_dismiss: false
        });
    }

    function updateSearchTerms(start, end, search) {
        var hasSearch = search && search != "";

        window.history.pushState(
            undefined, "timeglo.be",
            "/" + start + "/" + end + (hasSearch ? "/" + search : ""));

        ga('send', 'pageview', location.pathname);
    }

    function updateShareButtons(start, end, search) {
        var hasSearch = search && search != "";

        var text = "All" + 
            (hasSearch ? " " + search : "")
            + " events between " + start + " and " + end;
        var url = "http://timeglo.be" + window.location.pathname;

        $("#twitter-container")
            .attr("href", "https://twitter.com/share" +
                "?text=" + encodeURIComponent(text) +
                "&url=" + encodeURIComponent(url) +
                "&hashtags=timeglobe");

        $("#facebook-container")
            .attr("href", "https://www.facebook.com/sharer/sharer.php?u=" + encodeURIComponent(url));
    }

    updateTransformations();
}

var graph;

$(function() {
    graph = new Graph();
});
