define(['exports', './D'], function(exports, d){
    print('Module C got reference to D.text = ' + d.text);
    exports.text = 'Module C that depends on D.';
});