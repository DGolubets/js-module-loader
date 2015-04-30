define(['exports', './D'], function(exports, d){
    print('Module B got reference to D.text = ' + d.text);
    assert(d.text, "d.text is undefined");
    exports.text = 'Module B that depends on D.';
});