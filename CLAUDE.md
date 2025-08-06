# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JNPM is a Java implementation of Node Package Manager (NPM) that allows native Java applications to query, retrieve, and manage JavaScript packages. The project consists of two main modules:

- **jnpm**: Core library providing synchronous and RxJava APIs for NPM operations
- **maven**: Maven plugin for integrating NPM packages into Java build processes

## Build Commands

### Maven Commands
```bash
# Build entire project
mvn clean compile

# Run tests
mvn test

# Package JARs (includes uber JAR for CLI)
mvn package

# Install to local repository
mvn install

# Run checkstyle validation
mvn checkstyle:check

# Start test server (jnpm module only)
cd jnpm && mvn jetty:run
```

### CLI Usage
```bash
# After building, run CLI (from jnpm/target/)
java -jar jnpm-uber.jar --help

# Download packages
java -jar jnpm-uber.jar download vue@2.6.11

# Extract packages
java -jar jnpm-uber.jar extract -o /path/to/output vue@2.6.11
```

## Architecture Overview

### Core Components

**JNPMService** (`jnpm/src/main/java/org/orienteer/jnpm/JNPMService.java`):
- Main entry point for all JNPM operations
- Singleton pattern with configuration via `JNPMSettings`
- Provides both synchronous API and access to RxJava-based `RxJNPMService`
- Uses Retrofit2 + OkHttp for NPM registry communication

**RxJNPMService** (`jnpm/src/main/java/org/orienteer/jnpm/RxJNPMService.java`):
- Reactive API using RxJava2 for asynchronous operations
- Handles package info retrieval, searching, and dependency traversal

**Traversal System** (`jnpm/src/main/java/org/orienteer/jnpm/traversal/`):
- `TraversalTree`: Represents package dependency trees
- `ITraversalRule`: Defines which dependencies to include (dev, prod, optional, peer)
- `TraverseDirection`: Controls traversal order (WIDER vs DEEPER)

**Installation Strategies** (`jnpm/src/main/java/org/orienteer/jnpm/InstallationStrategy.java`):
- `NPM`: Standard NPM-style node_modules structure
- `WEBJARS`: WebJars-compatible layout
- `FLAT`, `SIMPLE`, `DIST`: Alternative extraction patterns

**CDN Components** (`jnpm/src/main/java/org/orienteer/jnpm/cdn/`):
- `CDNServlet`: Servlet for serving NPM packages as CDN
- `CDNWicketResource`: Apache Wicket integration for CDN functionality

**Maven Plugin** (`maven/src/main/java/org/orienteer/maven/jnpm/JNPMMojo.java`):
- Goal: `jnpm:install`
- Downloads and extracts NPM packages during Maven build
- Attaches resources to build classpath automatically

### Data Models (`jnpm/src/main/java/org/orienteer/jnpm/dm/`)
- `PackageInfo`: NPM package metadata
- `VersionInfo`: Specific version information and dependencies
- `RegistryInfo`: NPM registry information
- `SearchResults`: Package search results

## Key Dependencies

- **Retrofit2 + OkHttp3**: HTTP client for NPM registry API
- **Jackson**: JSON parsing for NPM API responses
- **RxJava2**: Reactive programming for async operations
- **Apache Commons Compress**: Tarball extraction
- **Picocli**: CLI argument parsing
- **Lombok**: Code generation for data classes

## Testing

The project uses JUnit 4 with Mockito for testing. Test files are located in:
- `jnpm/src/test/java/`
- `maven/src/test/java/`

Run specific test classes:
```bash
mvn test -Dtest=JNPMTest
mvn test -Dtest=JNPMMojoTest
```

## Development Notes

- Java 8+ required (configured in parent POM)
- Uses Checkstyle for code style validation (`check_style.xml`)
- Maven 3.6.0+ required for building
- Project follows standard Maven directory structure
- Uber JAR created via maven-shade-plugin enables CLI usage
- Jetty plugin configured for testing web components