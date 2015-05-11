exports.foo = function(){
    return "bar";
};

exports.bar = function(){
    // closure over exports
    return exports.foo();
};