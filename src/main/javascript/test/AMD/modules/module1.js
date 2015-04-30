define(['./module2', './module3'], function(m2, m3){
   console.log('loaded module1 into context: ' + this);

   return {
      func1: function(){
         console.log("invoked func1 in module1!");
         m2.func2();
      },
      func11: function(){
         console.log("invoked func11 in module1!");
      }
   };
});