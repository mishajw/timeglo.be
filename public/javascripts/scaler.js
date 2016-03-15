$(function() {
    function handleResize() {
        var $window = $(window);
        var width = $window.width();
        var height = $window.height();

        $("#wikimap-d3")
            .width(width)
            .height(height);

        $("#sidebar")
            .height(height);

        //$("#options-container")
        //    .css("height", height * 0.20);

        $("#infobox-container")
            .height(height - $("#options-container").height());

        $("#infobox")
            .height($("#infobox-container").height() * 0.95);

        if (graph) graph.updateScale();
    }

    $(window).resize(handleResize);

    handleResize();
});