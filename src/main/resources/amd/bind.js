/**
 * Initializes specified scope with require method.
 * It's easier to deal with method overloading here.
 */
(function(scope, loader){
    scope.require = function(modules, callback){
        if(typeof modules === "string"){
            return loader.require(modules);
        }
        else {
            return loader.require(modules, callback);
        }
    };

    scope.define = function(arg1, arg2, arg3){
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
        else {
            return loader.define(deps, factory);
        }
    };

    // required in AMD spec
    scope.define.amd = {};
});
