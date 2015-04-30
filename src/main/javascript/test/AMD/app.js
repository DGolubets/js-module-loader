define('app', ['./modules/module1', './modules/module2', './modules/module3'], function(m1, m2, m3){
   m1.func1();
   console.log('loaded module app into context: ' + this);
   console.log(m3.text);

   return {
   	text: "app text"
   };
});

require(['app'], function(app){
   console.log('I\'ve got my app reference:' + app);
   console.log(app.text);

});