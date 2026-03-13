# QR Code Example

This example demonstrates calling a Python library (called `qrcode`) from Java using
GraalVM's Polyglot API. It serves as a proof of concept for the core technique
used in this project to bridge Java and Docling's Python library.

The program generates a QR code from a string and displays it in a Swing window.

## Prerequisites
- GraalVM JDK installed and set as your active JDK
- Maven installed
- GraalPy installed for GraalVM

## Building
Navigate to the `examples/qrcode` directory and compile:
```
mvn compile
```

## Running
```
mvn exec:java "-Dexec.mainClass=org.example.App" "-Dgraalpy.resources=./python-resources"
```

## Expected Output
A window will appear displaying a QR code encoding the string:
```
Hello from GraalPy on JDK <your JDK version>
```

## How It Works
The example uses GraalVM's Polyglot API to load and call Python's `qrcode`
library directly from Java. The key steps are:

1. A Python context is created using `GraalPy.createPythonContext()`
2. The `qrcode` and `io` Python modules are imported into the context
3. A QR code image is generated and written to an in-memory byte stream
4. Java reads the byte stream and renders the image in a Swing window

The Java interfaces `QRCode` and `IO` act as type-safe bridges to their
Python counterparts, allowing Java code to call Python objects naturally.