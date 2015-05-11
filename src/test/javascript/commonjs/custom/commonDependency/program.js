var test = require("test");

var b = require('./b');
test.assert(b.name == 'B', 'Module B is invalid');

var c = require('./c');
test.assert(c.name == 'C', 'Module C is invalid');

exports.name = 'A';