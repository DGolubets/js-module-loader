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
val loader = AMDScriptLoader(engine, FileModuleReader("src/javascript/amd"))
```
then it can be used in Scala
```
loader.requireAsync("app").map { module => 
  // module.value will be a ScriptObjectMirror or boxed Java primitive, depending on the module return value
}
loader.requireAsync(Seq("React", "CommentBox")).map {
  case Seq(ScriptModule(react: ScriptObjectMirror), ScriptModule(commentBox: ScriptObjectMirror)) =>
    val commentBoxHtml = react.callMember("renderToString", react.callMember("createElement", commentBox)).toString
}
```
and in Javascript
```
// javascript should be evaluated with loader.context
engine.eval("require(['app'], function(app){ console.log(app); })", loader.context)
```

# Known Issues

Play < 2.4 doesn't have Nashorn classes in it's boot loader. 
To workaround it, add the following code to your play SBT config
```
Play.playCommonClassloader := {
  val oldCl = Play.playCommonClassloader.value
  def isNashornClass(className: String) = className.startsWith("jdk.nashorn.")
  def notNashornResource(resource: String) = resource != "META-INF/services/javax.script.ScriptEngineFactory"
  new classpath.DualLoader(
    oldCl, !isNashornClass(_), notNashornResource,
    ClassLoader.getSystemClassLoader, isNashornClass, notNashornResource
  )
}
```
