define(['./B', './C'], function(b, c){
    print('Module A got reference to B = ' + b);
    print('Module A got reference to C = ' + c);

    return {
        text: 'Module A that depends on B and C.'
    };
});