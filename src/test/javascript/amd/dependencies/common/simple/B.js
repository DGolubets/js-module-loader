define(['./D'], function(d){
    print('Module B got reference to D.text = ' + d.text);
    if(!d.text) throw "d.text is undefined";

    return {
        text: 'Module B that depends on D.'
    };
});