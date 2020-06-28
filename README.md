# JNPM

Native Java API to work with JavaScript Node Package Manager (NPM): query, retrieve, pack into jar (webjars), CDN

1. [Java API](#java-api)
2. [Command Line Interface](#command-line-interface)
3. [Maven Plugin](#maven-plugin) (very lightweight replacement for [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin))
4. [Make your CDN](#cdn)

## Java API

Include JNPM Jar into pom.xml:

```xml
<dependency>
    <groupId>org.orienteer.jnpm</groupId>
    <artifactId>jnpm</artifactId>
    <version>LATEST JNPM VERSION</version>
</dependency>
```

Initialize JNPM API prior to use.

```java
JNPMService.configure(JNPMSettings.builder()
  .homeDirectory(Paths.get("/home/myuser/.jnpm")) //Optional
  .downloadDirectory(Paths.get("/tmp")) //Optional
  //Other optional configurations: see JavaDoc for more info
 	.build());
```

JNPM API has 2 options: **Synchronous API** and **RXJava API**. Please use required one per your needs:

```java
JNPMService jnpmService = JNPMService.instance(); //Synchronous Java API
RxJNPMService rxJnpmService = JNPMService.instance().getRxService() //RXJava API
```

Examples of API use:

```java
//Print NPM Registry Information
System.out.println(JNPMService.instance().getRegistryInfo());
//Retrieve and print VUE package latest version
System.out.println(JNPMService.instance().getPackageInfo("vue").getLatest());
//Print package description for vue@2.6.11
System.out.println(JNPMService.instance().getVersionInfo("vue", "2.6.11").getDescription());
//Print latest version prior to vue version 2 official release
System.out.println(JNPMService.instance().bestMatch("vue@<2").getVersionAsString());
//Download tarball for vue@2.6.11 and print localpath
VersionInfo vueVersion = JNPMService.instance().getVersionInfo("vue", "2.6.11");
vueVersion.downloadTarball().blockingAwait();
System.out.println(vueVersion.getLocalTarball().getAbsolutePath());
//Search for "vue" and print description for first result
System.out.println(JNPMService.instance().search("vue").getObjects().get(0).getSearchPackage().getDescription());
//Traverse through all dev dependencies of latest vue package, print information
// and install as NPM do (node_modules/vue and etc)
JNPMService.instance().getRxService()
   .traverse(TraverseDirection.WIDER, TraversalRule.DEV_DEPENDENCIES, "vue")
   .subscribe(t -> {System.out.println(t); t.install(Paths.get("target", "readme"), InstallationStrategy.NPM);});
```

Read [![javadoc](https://javadoc.io/badge2/org.orienteer.jnpm/jnpm/javadoc.svg)](https://javadoc.io/doc/org.orienteer.jnpm/jnpm) for more information.

## Command Line Interface

## Maven Plugin

## CDN

