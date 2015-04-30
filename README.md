# js-module-loader
AMD and CommonJs loader for JVM

# Status
AMD: works, but requires more testing and a bit more features (shim config, plugins)

CommonJs: not here yet, but will have the same interface

I plan to test it for Java compatibility later and will make a Java compartible wrapper if needed.

# Usage
Loader is created as follows
```
val engineManager = new ScriptEngineManager(null)
val engine = engineManager.getEngineByName("nashorn")
val loader = AMDScriptLoader(engine, new File("src/javascript/amd"))
```
then it can be used in Scala
```
loader.require("app").map { module => 
  // module.value will be a ScriptObjectMirror or boxed Java primitive, depending on the module return value
}
```
and in Javascript
```
engine.eval("require(['app'], function(app){ console.log(app); })")
```
