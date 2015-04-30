define(['exports', './A'], function(exports, a){
    print('Module B got reference to A = ' + a);
    exports.text = 'Module B that depends on A.';
});