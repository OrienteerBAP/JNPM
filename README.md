[![Build Status](https://travis-ci.org/OrienteerBAP/JNPM.svg?branch=master)](https://travis-ci.org/OrienteerBAP/JNPM) [![javadoc](https://javadoc.io/badge2/org.orienteer.jnpm/jnpm/javadoc.svg)](https://javadoc.io/doc/org.orienteer.jnpm/jnpm)

# JNPM

Native Java API to work with JavaScript Node Package Manager (NPM): query, retrieve, pack into jar (webjars), CDN

## Table of Contents

1. [Quick Start](#quick-start)
2. [Java API](#java-api)
3. [Command Line Interface](#command-line-interface)
4. [Maven Plugin](#maven-plugin)
5. [Installation Strategies](#installation-strategies)
6. [CDN](#cdn)
7. [API Reference](#api-reference)
8. [Troubleshooting](#troubleshooting)

JNPM is a lightweight and fast replacement for [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin), [npm-maven-plugin](https://github.com/jy4618272/npm-maven-plugin) and even [WebJars](https://www.webjars.org/).

## Quick Start

### Maven Dependency

Add JNPM to your project:

```xml
<dependency>
    <groupId>org.orienteer.jnpm</groupId>
    <artifactId>jnpm</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Basic Usage

```java
import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;
import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.VersionInfo;

// Configure JNPM (required before first use)
JNPMService.configure(JNPMSettings.builder().build());

// Get package information
PackageInfo packageInfo = JNPMService.instance().getPackageInfo("vue");
System.out.println("Latest Vue.js version: " + packageInfo.getLatest());

// Get specific version info
VersionInfo versionInfo = JNPMService.instance().getVersionInfo("vue", "3.3.4");
System.out.println("Description: " + versionInfo.getDescription());

// Download package
versionInfo.downloadTarball().blockingAwait();
System.out.println("Downloaded to: " + versionInfo.getLocalTarball().getAbsolutePath());
```

### Maven Plugin Quick Example

Add to your `pom.xml` to download NPM packages during build:

```xml
<plugin>
    <groupId>org.orienteer.jnpm</groupId>
    <artifactId>jnpm-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>install</goal>
            </goals>
            <configuration>
                <packages>
                    <package>vue@3.3.4</package>
                </packages>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Java API

JNPM provides both **Synchronous API** and **RxJava API** for different use cases:

```java
JNPMService jnpmService = JNPMService.instance(); //Synchronous Java API
RxJNPMService rxJnpmService = JNPMService.instance().getRxService(); //RxJava API
```

### Complete Examples

#### Package Information and Search

```java
import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;
import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.dm.search.SearchResults;

// Configure JNPM
JNPMService.configure(JNPMSettings.builder().build());

// Get NPM Registry Information
System.out.println(JNPMService.instance().getRegistryInfo());

// Get package information
PackageInfo vuePackage = JNPMService.instance().getPackageInfo("vue");
System.out.println("Latest Vue.js version: " + vuePackage.getLatest());

// Get specific version information
VersionInfo vueVersion = JNPMService.instance().getVersionInfo("vue", "3.3.4");
System.out.println("Description: " + vueVersion.getDescription());
System.out.println("Dependencies: " + vueVersion.getDependencies());

// Find best matching version
VersionInfo bestMatch = JNPMService.instance().bestMatch("vue@^3.0.0");
System.out.println("Best match for ^3.0.0: " + bestMatch.getVersionAsString());

// Search for packages
SearchResults searchResults = JNPMService.instance().search("vue ui component");
searchResults.getObjects().forEach(result -> {
    System.out.println(result.getSearchPackage().getName() + ": " + 
                      result.getSearchPackage().getDescription());
});
```

#### Package Download and Installation

```java
import org.orienteer.jnpm.InstallationStrategy;
import java.nio.file.Paths;

// Download and install package
VersionInfo vueVersion = JNPMService.instance().getVersionInfo("vue", "3.3.4");

// Download tarball to cache
vueVersion.downloadTarball().blockingAwait();
System.out.println("Downloaded to: " + vueVersion.getLocalTarball().getAbsolutePath());

// Install using different strategies
vueVersion.install(Paths.get("target", "webjars"), InstallationStrategy.WEBJARS).blockingAwait();
vueVersion.install(Paths.get("target", "node_modules"), InstallationStrategy.NPM).blockingAwait();
```

#### Dependency Traversal with RxJava

```java
import org.orienteer.jnpm.RxJNPMService;
import org.orienteer.jnpm.traversal.TraverseDirection;
import org.orienteer.jnpm.traversal.ITraversalRule;
import io.reactivex.disposables.Disposable;

RxJNPMService rxService = JNPMService.instance().getRxService();

// Traverse and install all production dependencies
Disposable subscription = rxService
    .traverse(TraverseDirection.WIDER, ITraversalRule.DEPENDENCIES, "vue@3.3.4")
    .subscribe(
        traversalTree -> {
            System.out.println("Installing: " + traversalTree.getVersion().getName() + 
                             "@" + traversalTree.getVersion().getVersionAsString());
            traversalTree.install(Paths.get("target", "node_modules"), InstallationStrategy.NPM)
                        .blockingAwait();
        },
        error -> System.err.println("Error: " + error.getMessage()),
        () -> System.out.println("Installation complete")
    );

// Install dev dependencies too
rxService.traverse(TraverseDirection.WIDER, 
                  ITraversalRule.getRuleFor(true, true, false, false), // prod + dev
                  "vue@3.3.4")
         .subscribe(t -> t.install(Paths.get("target", "all-deps"), InstallationStrategy.SIMPLE));
```

#### Authentication Example

```java
// Configure with authentication
JNPMService.configure(JNPMSettings.builder()
    .registryUrl("https://npm.company.com/")
    .username("your-username")
    .password("your-npm-token")  // Use auth token, not password
    .build());

// Or configure with specific cache location
JNPMService.configure(JNPMSettings.builder()
    .homeDirectory(Paths.get("/opt/jnpm-cache"))
    .downloadDirectory(Paths.get("/tmp/npm-downloads"))
    .useCache(true)
    .build());
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
                  Home directory for JNPM (default: ~/.jnpm)
      --install-dir=<installDirectory>
                  Global install directory for JNPM (default: <home-dir>/)
  -L, --http-logger-level
                  HTTP Logger Level for debugging
                  Valid values: NONE, BASIC, HEADERS, BODY
  -P, --password=<password>
                  Password for authentication (optional)
      --registry=<registryUrl>
                  NPM registry URL to be used for package lookup and retrieval
                    (default: https://registry.npmjs.org/)
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
To include `vue` and other packages into your WAR please add the following into build>plugins section of your `pom.xml`

```xml
<plugin>
<groupId>org.orienteer.jnpm</groupId>
    <artifactId>jnpm-maven-plugin</artifactId>
	<version>${project.version}</version>
	<executions>
		<execution>
			<goals>
				<goal>install</goal>
			</goals>
			<configuration>
				<packages>
					<package>vue@3.3.4</package>
					<package>vuex@4.0.0</package>
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
      NPM packages to be downloaded and extracted (For example: vue@3.3.4)
      Required: Yes

    password
      Password for authentication (optional)

    pathPrefix
      Prefix for the directory under outputDirectory to which files will be
      placed

    registryUrl (Default: https://registry.npmjs.org/)
      NPM registry URL to be used for package lookup and retrieval

    serverId
      Server id from settings to get username and password from

    strategy (Default: WEBJARS)
      Installation strategy to be used

    useCache (Default: true)
      Use local cache. Useful if the same package version can be re-uploaded to a registry

    username
      Username for authentication (optional)
```

## Installation Strategies

JNPM supports different strategies for extracting and organizing NPM packages. Choose the strategy that best fits your project structure:

| Strategy | Use Case | Output Structure |
|----------|----------|------------------|
| **NPM** | Node.js compatibility | Standard `node_modules/` structure |
| **WEBJARS** | Spring Boot WebJars | `META-INF/resources/webjars/` structure |
| **FLAT** | Simple file serving | All files extracted to root directory |
| **SIMPLE** | Basic package structure | `packageName/` directory |
| **SIMPLE_VERSIONED** | Versioned packages | `packageName-version/` directory |
| **DIST** | Production builds only | Only `/dist` folder contents |
| **ONE_DUMP** | Single directory | All files in one flat directory |

### Strategy Examples

#### NPM Strategy
Perfect for Node.js applications or when you need standard NPM structure:

```bash
# Input: vue@3.3.4
# Output:
target/jnpm/node_modules/vue/
├── dist/
│   ├── vue.js
│   └── vue.min.js
├── package.json
└── README.md
```

Maven configuration:
```xml
<plugin>
    <groupId>org.orienteer.jnpm</groupId>
    <artifactId>jnpm-maven-plugin</artifactId>
    <configuration>
        <strategy>NPM</strategy>
        <packages>
            <package>vue@3.3.4</package>
        </packages>
    </configuration>
</plugin>
```

#### WEBJARS Strategy (Default)
Ideal for Spring Boot applications using WebJars:

```bash
# Input: vue@3.3.4
# Output:
target/jnpm/META-INF/resources/webjars/vue/3.3.4/
├── dist/
│   ├── vue.js
│   └── vue.min.js
└── package.json

# Access in web app: /webjars/vue/3.3.4/dist/vue.js
```

#### FLAT Strategy
All files extracted to root level (good for simple static serving):

```bash
# Input: vue@3.3.4
# Output:
target/jnpm/
├── vue.js
├── vue.min.js
├── vue.esm.js
└── ... (all files from package)
```

#### DIST Strategy
Only production-ready files from `/dist` directory:

```bash
# Input: vue@3.3.4
# Output:
target/jnpm/
├── vue.js
├── vue.min.js
└── vue.esm.js
```

### CLI Usage with Strategies

```bash
# Extract with NPM strategy
java -jar jnpm-uber.jar extract -s NPM -o ./output vue@3.3.4

# Extract with WEBJARS strategy
java -jar jnpm-uber.jar extract -s WEBJARS -o ./output vue@3.3.4 bootstrap@5.3.0

# Extract only dist files
java -jar jnpm-uber.jar extract -s DIST -o ./static vue@3.3.4
```

### Choosing the Right Strategy

- **Use NPM** when integrating with Node.js tools or maintaining compatibility
- **Use WEBJARS** for Spring Boot applications (automatically adds to classpath)
- **Use DIST** when you only need production-ready files
- **Use FLAT** for simple static file serving
- **Use SIMPLE_VERSIONED** when you need multiple versions of the same package

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

You can use `init-param` to specify extra JNPM parameters, for example, `registryUrl`, `username`, `password` and etc.

Files from NPM packages will be available through URLs with the following pattern: `http(s)://<host>:<port>/<deploy-folder>/cdn/<package expression>/<required file>`.
For example: `http://localhost:8080/cdn/vue@3.3.4/dist/vue.js`

### Apache Wicket

Add the following code to your [Apache Wicket](https://wicket.apache.org/) WebApplication:

```java
if(!JNPMService.isConfigured()) 
   JNPMService.configure(JNPMSettings.builder().build()); //Configure as you wish
   CDNWicketResource.mount(this, "/cdn");
```

Files from NPM packages will be available through URLs with the following pattern: `http(s)://<host>:<port>/<deploy-folder>/cdn/<package expression>/<required file>`.
For example: `http://localhost:8080/cdn/vue@3.3.4/dist/vue.js`

## API Reference

### Core Classes

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `JNPMService` | Main synchronous API | `getPackageInfo()`, `getVersionInfo()`, `bestMatch()`, `search()` |
| `RxJNPMService` | Reactive API | `traverse()`, async versions of sync methods |
| `JNPMSettings` | Configuration | `registryUrl()`, `homeDirectory()`, `username()`, `password()` |
| `VersionInfo` | Package version data | `downloadTarball()`, `install()`, `getDependencies()` |
| `PackageInfo` | Package metadata | `getLatest()`, `getVersions()`, `getDescription()` |

### Method Signatures

#### JNPMService Methods
```java
// Package information
PackageInfo getPackageInfo(String packageName)
VersionInfo getVersionInfo(String packageName, String version)
VersionInfo bestMatch(String packageName, String versionConstraint)
VersionInfo bestMatch(String expression) // e.g., "vue@^3.0.0"

// Search
SearchResults search(String text)
SearchResults search(String text, Integer size)
SearchResults search(String text, Integer size, Integer from)

// Registry
RegistryInfo getRegistryInfo()
```

#### RxJNPMService Additional Methods
```java
// Dependency traversal
Observable<TraversalTree> traverse(TraverseDirection direction, 
                                  ITraversalRule rule, 
                                  String... packageExpressions)

// All sync methods have async Maybe<T>/Single<T> versions
Maybe<PackageInfo> getPackageInfo(String packageName)
Maybe<VersionInfo> getVersionInfo(String packageName, String version)
```

#### Configuration Options
```java
JNPMSettings.builder()
    .registryUrl("https://registry.npmjs.org/")     // NPM registry URL
    .homeDirectory(Paths.get("~/.jnpm"))           // Cache location
    .downloadDirectory(Paths.get("/tmp"))          // Download temp dir
    .username("username")                          // NPM username
    .password("token")                             // NPM auth token
    .useCache(true)                               // Enable caching
    .validateSignature(true)                      // Verify packages
    .httpLoggerLevel(Level.BASIC)                 // HTTP debug logging
    .build();
```

### Traversal Rules

| Rule | Description |
|------|-------------|
| `ITraversalRule.DEPENDENCIES` | Production dependencies only |
| `ITraversalRule.DEV_DEPENDENCIES` | Development dependencies only |
| `ITraversalRule.OPTIONAL_DEPENDENCIES` | Optional dependencies only |
| `ITraversalRule.PEER_DEPENDENCIES` | Peer dependencies only |
| `ITraversalRule.getRuleFor(prod, dev, optional, peer)` | Custom combination |

## Troubleshooting

### Common Issues

#### Authentication Problems

**Issue**: 401 Unauthorized when accessing private registry
```
Solution: Use NPM auth token instead of password
```

```java
JNPMService.configure(JNPMSettings.builder()
    .registryUrl("https://npm.company.com/")
    .username("your-username")
    .password("npm_TOKEN_HERE")  // Use token from ~/.npmrc
    .build());
```

#### Corporate Proxy

**Issue**: Connection timeouts behind corporate firewall
```
Solution: Configure proxy through system properties
```

```java
// Set before JNPM configuration
System.setProperty("https.proxyHost", "proxy.company.com");
System.setProperty("https.proxyPort", "8080");
System.setProperty("https.proxyUser", "username");
System.setProperty("https.proxyPassword", "password");

JNPMService.configure(JNPMSettings.builder().build());
```

#### Cache Issues

**Issue**: Stale or corrupted package data
```bash
# Clear JNPM cache
rm -rf ~/.jnpm/cache/

# Or disable caching temporarily
```

```java
JNPMService.configure(JNPMSettings.builder()
    .useCache(false)  // Disable for clean downloads
    .build());
```

#### Version Resolution Problems

**Issue**: `bestMatch()` returns unexpected version
```java
// Debug version matching
List<VersionInfo> availableVersions = JNPMService.instance()
    .retrieveVersions("vue", "^3.0.0");
availableVersions.forEach(v -> System.out.println(v.getVersionAsString()));

// Use specific version if needed
VersionInfo specific = JNPMService.instance().getVersionInfo("vue", "3.3.4");
```

#### Memory Issues with Large Dependencies

**Issue**: OutOfMemoryError during dependency traversal
```java
// Process dependencies in batches
RxJNPMService rxService = JNPMService.instance().getRxService();
rxService.traverse(TraverseDirection.WIDER, ITraversalRule.DEPENDENCIES, "package")
    .buffer(10)  // Process 10 packages at a time
    .subscribe(batch -> {
        batch.forEach(tree -> {
            // Process each package
            tree.install(targetPath, strategy).blockingAwait();
        });
    });
```

### Debug Logging

Enable HTTP request logging to troubleshoot network issues:

```java
JNPMService.configure(JNPMSettings.builder()
    .httpLoggerLevel(Level.BASIC)    // NONE, BASIC, HEADERS, BODY
    .build());
```

### Performance Tips

1. **Use caching** - Keep `useCache(true)` for development
2. **Batch operations** - Use RxJava operators for large dependency sets  
3. **Specific versions** - Avoid version ranges when possible for faster resolution
4. **Shared cache** - Configure `homeDirectory` to shared location for team development

