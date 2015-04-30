define(['./A'], function(a){
    print('Module B got reference to A = ' + a);
    return {
        text: 'Module B that depends on A.'
    };
});