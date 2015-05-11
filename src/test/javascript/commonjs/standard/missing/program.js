var test = require('test');
var throwed = false;
try {
    require('bogus');
} catch (exception) {
    throwed = true;
}

test.assert(throwed, 'FAIL require throws error when module missing', 'fail');