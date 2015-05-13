# js-module-loader
AMD and CommonJs loader for JVM

# Status
0.1-SNAPSHOT

AMD: works, but requires more testing.

CommonJs: works, but requires more testing.

I plan to test it for Java compatibility later and will make a Java compartible wrapper if needed.

# AMD
AMD Loader is created as follows
```
val loader = AMDScriptLoader(FileModuleReader("src/javascript/amd"))
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
loader.engine.eval("require(['app'], function(app){ console.log(app); })")
```

# CommonJs
CommonJs Loader is created as follows
```
val loader = CommonJsLoader(FileModuleReader("src/javascript/commonjs"))
```
then it can be used in Scala
```
loader.require("app") match { 
  case Success(module) => ???
  case _ => ???
}
```
and in Javascript
```
// javascript should be evaluated with loader.context
loader.engine.eval("require('app').startMyApp();")
```

# Thread safety
Loaders are safe to use from different threads.
However, once you got javascript objects or deal with a ScriptEngine you are one on one with a Nashorn which is not thread safe.
You can read more about Nashorn and MT-safety there: https://blogs.oracle.com/nashorn/entry/nashorn_multi_threading_and_mt

I plan to add a small thread safety guide here later.

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
