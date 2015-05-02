define(["require", "./B"], function(require){
    var b = require("./B");
    var c = require("./C");
    assert(b, "Module B should have been loaded.");
    assert(b.text, "Module B text is not here");
    assert(c === undefined, "Module C should be undefined, cos it can't be loaded synchronously.");
    return {};
});