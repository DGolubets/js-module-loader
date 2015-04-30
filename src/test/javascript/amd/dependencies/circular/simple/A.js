define(['./B'], function(b){
    print('Module A got reference to B = ' + b);
    return {
        text: 'Module A that depends on B.'
    };
});