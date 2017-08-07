* Java
  * sort out cross-project dependencies
  
    Generally it works but there are probably many strange cases that deserve special treatment.
    
  * properly use `compileOnly` (akin to Maven's provided scope) / compile / testCompile
  * ~~check test execution~~
    
    Test are executed. Many of them are failing due broken resource loading (lookup).
    That is something what can be fixed relatively easily.
  * Java 7/8

* packaging / deployment
  * build fat JAR with proper content
  * find how to do release and deployment to nexus (is tagging enough or do we need to keep RELCFG.versions?, how to do major, minor, patch releases) 
  * deploy to production like per RELCFG

* Scala
  * build scala code - 80% due some problems with modeling
  * enable 2.10/2.11 switching 
    This is tricky. 
    Some simple cases look OK but modeling is full of strange patterns. 
    One possibility is to conditionally include subprojects but it makes it harder to switch between 2.10 and 2.11.
  * scalatests
  * build flags

* Others
  * dependency resolution checks - Mool doesn't do transitive dependency resolution so Gradle (and Maven) can drag in some additional dependencies. And it can substitute different versions in some cases. Need to check. I can make it compatible 1:1 but it feels like shoehorning.
  * ~~custom protoc~~
  
    OK for the moment. It can refer to `$HOME/.mooltool` temporarily.
     
    Check if artifact in Nexus is patched or not.
  * ~~thrift~~
  
    Pointing to binary in `$HOME/.mooltool` seems enough.
    
  * style checks
  * sort out duplicated code, missing conversions
  * merge some projects manually
  * Python libs, binaries, tests
  * verify IDE integration
  * explore composite builds (it would make it possible to use smaller or bigger set of repos as one Gradle build)
