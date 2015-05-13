var test = require("test");
var a = require("a");

test.assert(a.foo() === "bar", "Module a should export a single object by means of assigning to 'module.exports'.");