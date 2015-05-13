var test = require("test");
var a = require("./submodules/a");

test.assert(a.getB(), "Should load module B.");