exports.assert = function(guard, message){
    if(!guard)
        throw "Assert failed: " + message
};

exports.evalOrDefault = function(code, def){
    try {
        return eval(code);
    } catch(err){
        return def;
    }
};