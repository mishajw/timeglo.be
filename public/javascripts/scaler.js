$(function() {
    var $window = $(window);
    var width = $window.width();
    var height = $window.height();

    $("#wikimap-d3")
        .css("width", width)
        .css("height", height);

    $("#sidebar")
        .css("height", height);

    //$("#options")
    //    .css("height", height * 0.20);

    $("#infobox")
        .css("height", height - $("#options").height() - 100);
});