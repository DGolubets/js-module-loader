define(['exports', 'module', 'require'], function(exports, module, require){
   exports.text = "module3 has some text: my id is " + module.id;
   //console.log(require('./module1'));
   console.log('loaded module3 into context: ' + this);
});