/**
 * Initializes specified scope with require method.
 */
(function(scope, loader){
    scope.require = function(module){
        return loader.require(module);
    };
});
