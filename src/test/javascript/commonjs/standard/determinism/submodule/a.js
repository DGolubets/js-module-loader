var test = require('test');
var pass = false;
try {
    require('b');
} catch (exception) {
    pass = true;
}
test.assert(pass, 'require does not fall back to relative modules when absolutes are not available.')
