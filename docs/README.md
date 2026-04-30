# PyTorch in Java, Documentation

This documentation covers the options for running PyTorch from Java, based on real-world setup experience with GraalVM on WSL2. All docs are concept-focused, they explain what each approach is, how it works, and when to use it, without implementation code.

## Background

The core challenge is that PyTorch is a Python library built on C++ and CUDA extensions. Java applications cannot import it directly. Every approach in this documentation is essentially a strategy for bridging that gap, either by bringing Python into the Java process, calling the underlying C++ directly, converting the model to a format Java can read natively, or running Python alongside Java and communicating between them.

## Approaches

### [Option A: GraalVM Polyglot (GraalPy)](./graalvm-polyglot.md)
Run Python scripts directly inside the JVM using GraalVM's Polyglot API and GraalPy runtime.

### [Option B: CPython Subprocess](./cpython-subprocess.md)
Install PyTorch in a standard CPython virtual environment, then call Python scripts from Java as a child process.

### [Option C: Deep Java Library (DJL)](./djl.md)
A Java-native library that wraps libtorch directly. No Python required at runtime.

### [Option D: Jython](./jython.md)
Python 2.7 implemented directly on the JVM. Shares objects between Java and Python with no IPC overhead.

### [Option E: Py4J (Python Gateway Server)](./py4j.md)
Start a Python gateway server; Java connects over a local socket and calls Python objects as if they were local Java objects.

### [Option F: Apache Arrow / Shared Memory](./apache-arrow.md)
Use Apache Arrow's IPC format or Plasma shared memory store to pass large tensors between Java and Python processes with zero-copy overhead.

### [Option G: Native libtorch + JNI/JNA](./native-libtorch-jni.md)
Call libtorch's C++ API directly from Java via JNI or JNA, no Python in the loop at all.

### [Option H: gRPC Microservice](./grpc-microservice.md)
Define a shared contract in a proto file, run PyTorch in a Python gRPC server, and call it from Java with generated typed stubs over HTTP/2.

### [Option I: TensorFlow Java](./tensorflow-java.md)
Google's official Java API for TensorFlow. Load SavedModels and run inference directly in the JVM, no Python at runtime.

### [Option J: ONNX Runtime in Java](./onnx-runtime.md)
Export your PyTorch model to the ONNX format once, then load and run it in Java with no Python at runtime.

### [Option K: Long-Running Python Worker](./long-running-worker.md)
Keep a single Python process alive for the lifetime of your Java application, loading models once and reusing them across all calls.

### [Option L: GraalVM Native Image](./graalvm-native-image.md)
Compile your Java + ONNX Runtime application into a standalone native binary with no JVM required at runtime.

### [Option M: Panama / Foreign Function & Memory API](./panama-ffm.md)
Java 22's built-in mechanism for calling native C functions and managing off-heap memory without writing JNI bridge code.

### [Model Quantization Guide](./model-quantization.md)
Reduce model size and speed up CPU inference by representing weights in lower precision.

### [Troubleshooting Guide](./troubleshooting.md)
Every error encountered during the real GraalVM + PyTorch + WSL2 setup session, root causes and fixes.

## Quick Comparison

The following summarises each approach across five dimensions. "PyTorch works" means the approach can run PyTorch without workarounds. "No Python needed" means Python does not need to be installed on the deployment machine. "Runs Python scripts" means existing .py files can be executed as-is.

GraalVM Polyglot does not currently work with PyTorch. It requires no Python at deployment but only in theory, since installation fails. Complexity is high.

CPython Subprocess works with PyTorch and can run existing Python scripts, but requires Python on the deployment machine. Performance is medium and complexity is low.

DJL works with PyTorch and requires no Python at deployment. It cannot run arbitrary Python scripts. Performance is high and complexity is medium.

Jython does not work with PyTorch. No Python required at deployment but the approach is not viable for ML. Complexity is low.

Py4J works with PyTorch and can run Python scripts. Python is required. Performance is medium and complexity is medium.

Apache Arrow works with PyTorch and can run Python scripts. Python is required. Performance is high and complexity is medium to high.

Native JNI/JNA works with PyTorch and requires no Python. It cannot run Python scripts directly. Performance is the highest available and complexity is high.

gRPC Microservice works with PyTorch and can run Python scripts. Python is required. Performance is high and complexity is medium to high.

TensorFlow Java works with TensorFlow format models only and requires no Python. Performance is high and complexity is medium.

ONNX Runtime works with PyTorch models exported to ONNX format. No Python required at deployment. Performance is high and complexity is low to medium.

Long-Running Worker works with PyTorch and can run Python scripts. Python is required. Performance is medium to high and complexity is low.

GraalVM Native Image works via ONNX Runtime and requires no Python. Performance is high and complexity is high.

Panama/FFM works with PyTorch via native bindings and requires no Python. Performance is the highest available and complexity is high.

## Decision Guide

- I have existing Python scripts to run as-is → CPython Subprocess or Long-Running Worker
- I want the simplest path to model inference in Java → DJL or ONNX Runtime
- I'm passing large tensors frequently and latency matters → Apache Arrow
- I need maximum performance or CUDA control → Native JNI/JNA or Panama/FFM
- I use Docling and want to avoid cold starts → Long-Running Worker
- I want a single deployable binary → GraalVM Native Image
- I want type-safe service-to-service calls → gRPC
- I heard about Jython → Read the Jython doc; it won't work for PyTorch

## Environment

The setup in these docs was performed on Windows 11 with WSL2 (Ubuntu), using GraalPy 25.0.2, Maven, and GraalVM JDK 21.
