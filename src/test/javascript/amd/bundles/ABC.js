// module A
define(['B', 'C'], function(b, c){
    return {
        text: 'Module A'
    };
});

// module B
define('B', function(){
    return {
        text: 'Module B'
    };
});

// module C
define('C', function(){
    return {
        text: 'Module C'
    };
});