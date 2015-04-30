/**
 * Require and define functions that simply call loader.
 */

(function(global){
    // loader is exposed under the same name initially, to prevent conflicts.
    var loader = require;

    global.require = function(modules, callback){
        if(typeof modules === "string"){
            return loader.require(modules);
        }
        else {
            return loader.require(modules, callback);
        }
    };

    global.define = function(arg1, arg2, arg3){
        var moduleId;
        var deps = [];
        var factory;

        if(typeof arg1 === "string"){
            moduleId = arg1;

            if(Array.isArray(arg2)){
                deps = arg2;
                factory = arg3;
            }
            else {
                factory = arg2;
            }
        }
        else if(Array.isArray(arg1)){
            deps = arg1;
            factory = arg2;
        }
        else {
            factory = arg1;
        }

        if(moduleId){
            return loader.define(moduleId, deps, factory);
        }
        else{
            return loader.define(deps, factory);
        }
    };

})(this);
