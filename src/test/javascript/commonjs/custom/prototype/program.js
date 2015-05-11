var test = require("test");
var a = require("a");

test.assert(a.obj instanceof Object, "Objects created in different modules must have the same prototype.");
