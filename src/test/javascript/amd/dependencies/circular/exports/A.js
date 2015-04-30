define(['exports', './B'], function(exports, b){
    print('Module A got reference to B = ' + b);
    exports.text = 'Module A that depends on B.';
});