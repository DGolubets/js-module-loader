define(['exports', './B', './C'], function(exports, b, c){
    print('Module A got reference to B = ' + b);
    print('Module A got reference to C = ' + c);
    exports.text = 'Module A that depends on B and C.';
});