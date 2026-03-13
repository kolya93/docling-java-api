# Running Hello World with GraalVM

This guide covers how to run the Hello World examples in this project
using GraalVM and Maven.

## Prerequisites
- GraalVM JDK installed and set as your active JDK
- Maven installed
- GraalPy installed for GraalVM (required for PolyglotTest)

## Building
Before running either example, compile the project:
```
mvn compile
```

## Java Hello World
A sanity check that your Java and Maven setup is working correctly. 

### Running
Navigate to the project directory and run:
```
mvn exec:java -Dexec.mainClass="org.example.App"
```

### Expected Output
```
Hello World!
```

## GraalVM Polyglot Hello World

Demonstrates GraalVM's Polyglot API by executing Python code from within a Java program.
This is the basis for how this project bridges Java and Docling's Python library.

### Running
```
mvn exec:java -Dexec.mainClass="org.example.PolyglotTest"
```

### Expected Output
```
Hello from GraalPython
```
