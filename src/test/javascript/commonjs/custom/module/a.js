var test = require("test");

test.assert(typeof module === "object", "In a module, there must be a free variable 'module', that is an Object");
test.assert(module.id === "a", "The 'module' object must have a 'id' property that is the top-level 'id' of the module.");
test.assert(require(module.id) === exports, "The 'id'' property must be such that require(module.id) will return the exports object from which the module.id originated.");