define(function(require, exports, module){
    assert(typeof require === "function", "'require' argument is invalid: " + JSON.stringify(require));
    assert(typeof exports === "object", "'exports' argument is invalid: " + JSON.stringify(exports));
    assert(module && typeof module.id === "string", "'module' argument is invalid: " + JSON.stringify(module));
});