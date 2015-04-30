var global = this;
var console = {};
console.debug = print;
console.warn = print;
console.log = print;

function assert(res, message){
    if(!res)
        throw "Assert failed: " + message
}