# JNPM

Native Java API to work with JavaScript Node Package Manager (NPM): query, retrieve, pack into jar (webjars), CDN

1. [Java API](#java-api)
2. [Command Line Interface](#command-line-interface)
3. [Maven Plugin](#maven-plugin) (very lightweight and fast replacement for [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin))
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

```
Usage: jnpm [-hV] [--download-dir=<downloadDirectory>]
            [--home-dir=<homeDirectory>] [--install-dir=<installDirectory>]
            [COMMAND]
Java implementation of Node Package Manager
      --download-dir=<downloadDirectory>
                  Cache directory for JNPM to download packages to (default:
                    <home-dir>/cache/)
  -h, --help      Show this help message and exit.
      --home-dir=<homeDirectory>
                  Home directory for JNPM (default: C:\Users\naryzhny\.jnpm)
      --install-dir=<installDirectory>
                  Global install directory for JNPM (default:
                    <home-dir>/node_modules/)
  -V, --version   Print version information and exit.
Commands:
  download, d  Download packages into local cache
  extract, e   Extract packages: very similiar to 'npm install', but without
                 changing package.json modification
``` 
```
Usage: jnpm download [-h] [--[no-]dev] [--[no-]download] [--[no-]optional] [--
                     [no-]peer] [--[no-]prod] <packageStatements>...
Download packages into local cache
      <packageStatements>...
                        Packages to be retrieved
      --[no-]dev        Download dev dependencies
      --[no-]download   Download by default. Negate if  just attempt to lookup
                          is needed
  -h, --help
      --[no-]optional   Download optional dependencies
      --[no-]peer       Download peer dependencies
      --[no-]prod       Download dependencies (default)
```

```
Usage: jnpm extract [-gh] [--[no-]dev] [--[no-]download] [--[no-]optional] [--
                    [no-]peer] [--[no-]prod] [-o=<folder>] [-s=<strategy>]
                    <packageStatements>...
Extract packages: very similiar to 'npm install', but without changing package.
json modification
      <packageStatements>...
                          Packages to be retrieved
      --[no-]dev          Download dev dependencies
      --[no-]download     Download by default. Negate if  just attempt to
                            lookup is needed
  -g                      Extract package(s) globally
  -h, --help
  -o, --output=<folder>   Output folder to extract to
      --[no-]optional     Download optional dependencies
      --[no-]peer         Download peer dependencies
      --[no-]prod         Download dependencies (default)
  -s, --strategy=<strategy>
                          Strategy for extraction: FLAT, NPM, SIMPLE,
                            SIMPLE_VERSIONED, WEBJARS
```
## Maven Plugin

JNPM maven plugin allows you natively integrate NPM resources into your build process.
For example, you can download and pack JS packages inside your WAR to use later through WebJars extensions.
To include `vue` and into your WAR please add the following into build>plugins section of your `pom.xml`

```xml
<plugin>
<groupId>org.orienteer.jnpm</groupId>
    <artifactId>jnpm-maven-plugin</artifactId>
	<version>1.0-SNAPSHOT</version>
	<executions>
		<execution>
			<goals>
				<goal>install</goal>
			</goals>
			<configuration>
				<packages>
					<package>vue@2.6.11</package>
					<package>vuex@~3.4.0</package>
				</packages>
			</configuration>
		</execution>
	</executions>
</plugin>
```

More details about the plugin:

```
jnpm:install
  Description: Goal to download, extract and attach npm resources
  Implementation: org.orienteer.maven.jnpm.JNPMMojo
  Language: java
  Bound to phase: generate-resources

  Available parameters:

    attachResources (Default: true)
      Attach downloaded resources to the build process

    excludes
      What has to be excluded from resources to be attached

    getDev (Default: false)
      Download development dependencies

    getOptional (Default: false)
      Download optional dependencies

    getPeer (Default: false)
      Download peer dependencies

    getProd (Default: false)
      Download direct dependencies

    includes
      What should be included as resources (Default: empty - means everything)

    outputDirectory (Default: ${project.build.directory}/jnpm/)
      Required: true
      Location of the output directory

    packages
      Required: true
      NPM packages to be downloaded and extracted (For example: vue@2.6.11)

    strategy (Default: WEBJARS)
      Installation strategy to be used
```

## CDN

