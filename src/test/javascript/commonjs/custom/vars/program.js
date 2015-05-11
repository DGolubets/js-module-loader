var test = require("test");
var a = require("a");

test.assert(test.evalOrDefault("var1") === undefined, "Module 'a' variable 'var1' should not be declared in the global scope.");