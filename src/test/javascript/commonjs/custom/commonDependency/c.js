var test = require("test");

var d = require('./d');
test.assert(d.name == 'D', 'Module D is invalid');

exports.name = 'C';