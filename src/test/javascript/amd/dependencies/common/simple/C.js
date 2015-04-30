define(['./D'], function(d){
    print('Module C got reference to D.text = ' + d.text);

    return {
        text: 'Module C that depends on D.'
    };
});