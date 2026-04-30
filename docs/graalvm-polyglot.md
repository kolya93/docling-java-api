# Option A: GraalVM Polyglot (GraalPy)

## What It Is

GraalVM is a high-performance JDK from Oracle that extends the standard JVM with several capabilities, the most relevant being the Polyglot API. This API lets you run code written in other languages, Python, JavaScript, Ruby, R, directly inside the same JVM process as your Java code. The languages share the same memory space, meaning a Python object and a Java object can reference each other without serialization or IPC.

GraalPy is GraalVM's implementation of Python. It runs Python code by compiling it to the JVM's intermediate representation rather than using the standard CPython interpreter.

## Why It's Appealing

On paper, GraalVM Polyglot is the most elegant solution to the Java/Python problem. There is no separate process, no socket, no serialization, and no data copying. A Java method can call a Python function and receive a Python object back as if it were a local Java object. For PyTorch, this would mean calling torch.tensor() from a Java method and getting a live tensor back, all in-process.

## Why It Fails for PyTorch

PyTorch is not a pure Python library. Its actual computation engine is written in C++ and exposed to Python through C extensions, compiled native code that plugs into the Python interpreter. When you call torch.tensor() in Python, you're ultimately calling into compiled C++ code in libtorch.

GraalPy can run pure Python code, but C extensions present a serious challenge. GraalPy implements the Python C extension API, but compatibility is incomplete for complex native libraries like PyTorch, which has an enormous and deeply non-standard C++ extension layer.

The more immediate problem is that PyTorch has no pre-built wheel for GraalPy. A wheel is a pre-compiled package. CPython wheels exist for Linux, Mac, and Windows across multiple Python versions, but GraalPy is a separate runtime and requires its own separately compiled wheels. The GraalPy wheel repository contains packages like numpy and scipy, but not torch.

Without a pre-built wheel, pip falls back to building PyTorch from source. This requires compiling hundreds of thousands of lines of C++, a process that takes significant RAM (more than WSL2's default memory limit), requires specific build tools, and has dependency resolution issues specific to GraalPy's isolated build subprocess behavior. See the Troubleshooting Guide for the specific errors that arise.

## Project Structure

The project layout for GraalVM Polyglot follows standard Maven conventions with an additional directory for Python scripts. The Java entry point uses GraalVM's Context API to create a Python execution environment, configure it to allow native access and file I/O, and point it at a GraalPy virtual environment where packages are installed. Python scripts live in a separate directory and are loaded as strings at runtime.

## GraalPy Virtual Environment

GraalPy uses the same virtual environment concept as CPython, an isolated directory containing a Python interpreter and installed packages. The key difference is that you must create it using the graalpy command rather than python3, and packages installed into a standard CPython venv are not compatible with GraalPy.

A persistent pitfall when using pyenv (a Python version manager) is that the pip command resolves through pyenv's shim system, which may point to the base GraalPy installation rather than your virtual environment. Packages installed this way are invisible to the isolated build subprocesses that pip spawns when compiling packages from source. Always verify that which pip points inside your virtual environment directory before installing anything.

## WSL2 Memory Constraints

Building large packages from source inside WSL2 requires more memory than WSL2 allocates by default. WSL2 caps memory at roughly half of physical RAM or 8GB, whichever is lower. Compiling numpy (a PyTorch build dependency) alone can exceed this limit, causing the build process to be killed by the OS with exit code -9. This can be addressed by creating a .wslconfig file in your Windows user directory that increases the memory limit, but this only helps with the memory problem, the dependency resolution issue in pip's build subprocess is a separate blocker.

## Current Verdict

As of GraalPy 25.0.2, running PyTorch via GraalVM Polyglot is not feasible. The path forward would require either Oracle shipping a pre-built GraalPy wheel for PyTorch, or PyTorch's build system becoming compatible with GraalPy's pip subprocess environment. Neither is imminent. Use one of the other approaches in this documentation.
