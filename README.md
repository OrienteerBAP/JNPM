[![Build Status](https://travis-ci.org/OrienteerBAP/JNPM.svg?branch=master)](https://travis-ci.org/OrienteerBAP/JNPM) [![javadoc](https://javadoc.io/badge2/org.orienteer.jnpm/jnpm/javadoc.svg)](https://javadoc.io/doc/org.orienteer.jnpm/jnpm)

# JNPM

Native Java API to work with JavaScript Node Package Manager (NPM): query, retrieve, pack into jar (webjars), CDN

1. [Java API](#java-api)
2. [Command Line Interface](#command-line-interface)
3. [Maven Plugin](#maven-plugin) (lightweight and fast replacement for [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin), [npm-maven-plugin](https://github.com/jy4618272/npm-maven-plugin) and even [WebJars](https://www.webjars.org/))
4. [Make your CDN](#cdn)
	* [Servlet](#servlet)
	* [Apache Wicket](#apache-wicket)

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
            [-P=<password>] [--registry=<registryUrl>] [-U=<username>] [COMMAND]
Java implementation of Node Package Manager
      --download-dir=<downloadDirectory>
                  Cache directory for JNPM to download packages to (default:
                    <home-dir>/cache/)
  -f, --force     Force to fetch remote resources even if a local copy exists                                                                                                    on disk
  -h, --help      Show this help message and exit.
      --home-dir=<homeDirectory>
                  Home directory for JNPM (default: C:\Users\naryzhny\.jnpm)
      --install-dir=<installDirectory>
                  Global install directory for JNPM (default:
                    <home-dir>/node_modules/)
  -L, --http-logger-level
                  HTTP Logger Level for debugging
                  Valid values: NONE, BASIC, HEADERS, BODY
  -P, --password=<password>
                  Password for authentication (optional)
      --registry=<registryUrl>
                  NPM registry URL to be used for package lookup and retrieval
                    (default: http://registry.npmjs.org/)
  -U, --username=<username>
                  Username for authentication (optional)
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
                            SIMPLE_VERSIONED, WEBJARS, ONE_DUMP, DIST
```
## Maven Plugin

JNPM maven plugin allows you natively integrate NPM resources into your build process.
For example, you can download and pack JS packages inside your WAR to use later through WebJars extensions.
To include `vue` and into your WAR please add the following into build>plugins section of your `pom.xml`

```xml
<plugin>
<groupId>org.orienteer.jnpm</groupId>
    <artifactId>jnpm-maven-plugin</artifactId>
	<version>${LATEST_JNPM_VERSION}</version>
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
  Goal to download, extract and attach npm resources

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
      
    httpLoggerLevel (Default: NONE)
    	HTTP Logger Level for debugging

    includes
      What should be included as resources (Default: empty - means everything)

    outputDirectory (Default: ${project.build.directory}/jnpm/)
      Location of the output directory
      Required: Yes

    packages
      NPM packages to be downloaded and extracted (For example: vue@2.6.11)
      Required: Yes

    password
      Password for authentication (optional)

    pathPrefix
      Prefix for the directory under outputDirectory to which files will be
      placed

    registryUrl (Default: http://registry.npmjs.org/)
      NPM registry URL to be used for package lookup and retrieval

    strategy (Default: WEBJARS)
      Installation strategy to be used

    useCache (Default: true)
      Use local cache. Useful if the same package version can be re-uploaded to a registry

    username
      Username for authentication (optional)
```

## CDN

### Servlet

Add the following mapping to your `web.xml`. Adjust as needed:

```xml
  <servlet>
    <servlet-name>CDNServlet</servlet-name>
    <servlet-class>org.orienteer.jnpm.cdn.CDNServlet</servlet-class>
  </servlet>

  <servlet-mapping>
    <servlet-name>CDNServlet</servlet-name>
    <url-pattern>/cdn/*</url-pattern>
  </servlet-mapping>
```

Files from NPM packages will be available through URLs with the following pattern: `http(s)://<host>:<port>/<deploy-folder>/cdn/<package expression>/<required file>`.
For example: `http://localhost:8080/cdn/vue@2.6.11/dist/vue.js`

You can user `init-param` to specify extra JNPM parameters, for example, `registryUrl`, `username`, `password` and etc.

### Apache Wicket

Add the following code to your [Apache Wicket](https://wicket.apache.org/) WebApplication:

```java
if(!JNPMService.isConfigured()) 
   JNPMService.configure(JNPMSettings.builder().build()); //Configure as you wish
   CDNWicketResource.mount(this, "/cdn");
```

Files from NPM packages will be available through URLs with the following pattern: `http(s)://<host>:<port>/<deploy-folder>/cdn/<package expression>/<required file>`.
For example: `http://localhost:8080/cdn/vue@2.6.11/dist/vue.js`
