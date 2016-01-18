$(function() {
    var $window = $(window);
    var width = $window.width();
    var height = $window.height();

    $("#wikimap-d3")
        .css("width", width)
        .css("height", height);

    $("#sidebar")
        .css("height", height * 0.9);

    $("#infobox")
        .css("height", height * 0.85);
});