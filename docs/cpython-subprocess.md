# Option B: CPython Subprocess

Run PyTorch in a standard CPython process and call it from Java by launching that process as a child of the Java application. Java and Python run as separate OS processes and communicate by passing data between them.

## What It Is

CPython is the standard Python interpreter, the one you get when you run python3 on any Linux, Mac, or Windows system. It fully supports PyTorch because PyTorch was designed for it. Installing PyTorch into a CPython virtual environment just works, without any of the compatibility issues that arise with GraalPy.

The subprocess approach uses Java's process management API to launch a Python script as a child process, send it input, and read back output. From the operating system's perspective, two completely separate programs are running, a Java program and a Python program, and they communicate through standard input/output streams or files.

## How the Communication Works

When Java launches a Python child process, it gets handles to three streams: the process's standard input (where Java can write data), standard output (where Python writes results that Java reads), and standard error (where Python writes error messages). These streams are the simplest possible IPC mechanism, they're just byte streams connected between two processes.

The most common patterns for structuring the data passing through these streams are plain text for simple values, JSON for structured data, and files for large payloads like document content or image data. JSON is the most practical general-purpose choice because both Java and Python have mature libraries for serializing and deserializing it, and it handles nested structures, lists, and mixed types cleanly.

## The Cold Start Problem

The most significant downside of the basic subprocess approach is that every call to Python pays the full startup cost: launching the Python interpreter, importing PyTorch (which loads large native libraries), and loading any model weights. For PyTorch alone this is typically 2-5 seconds. For heavier libraries like Docling, which loads multiple ML models, it can be 10-15 seconds.

If you only need to run inference occasionally, say, once per user request with long gaps between, this cost may be acceptable. If you need to process many documents or run many inferences, it becomes a serious bottleneck. The Long-Running Worker pattern (see long-running-worker.md) addresses this by keeping the Python process alive between calls.

## Project Structure

A subprocess-based project has a standard Java/Maven structure with an additional directory for Python scripts. The Python scripts are completely standalone, they read input from command-line arguments or stdin and write results to stdout. They have no knowledge of Java and can be developed and tested independently using a normal Python workflow. The Java side is responsible only for launching the process, passing data in, and reading data out.

## Virtual Environment Setup

Installing PyTorch into a CPython virtual environment is straightforward because CPython wheels exist for all major platforms and Python versions. You create the virtual environment using python3 -m venv, activate it, and install PyTorch with pip. Unlike GraalPy, you do not need to worry about wheel compatibility or build-from-source failures.

The only path you need to track is the location of the Python interpreter inside the virtual environment. This path gets hardcoded into your Java configuration so that Java knows which Python executable to launch. It's good practice to make this path configurable rather than hardcoded, so the application works across different machines and deployment environments.

## Passing Data Between Processes

The three main patterns for data exchange are:

Command-line arguments work well for simple scalar inputs, numbers, strings, file paths. The Java process passes arguments when launching the Python script, and Python reads them from sys.argv. This is appropriate for invoking a script that takes a file path and returns a result.

JSON over stdin/stdout is the most flexible option for structured data. Java writes a JSON object to the process's stdin, Python reads and parses it, runs inference, serializes the result to JSON, and writes it to stdout. Java reads and parses the response. This handles arrays, nested objects, and mixed types without custom serialization logic.

Temporary files are best for large data like raw tensors, images, or document content. Java writes input to a temp file, passes the path to Python, Python reads the file, processes it, and writes results to another file. Java reads the result file. This avoids memory pressure from holding large payloads in process memory on the Java side while Python is working.

## Tradeoffs

The subprocess approach requires Python and PyTorch to be installed on every machine where the Java application runs. This is a deployment dependency that must be managed, documented, installed, and kept at the right version.

The approach is however the most straightforward to develop and debug. The Python scripts are ordinary Python programs. You can run them directly from the command line, test them with standard Python testing tools, and debug them with a Python debugger, entirely independently of Java. The Java side is equally simple: it's just process management code with no ML-specific dependencies.

For projects that need to run existing Python scripts, this is usually the right starting point. Its simplicity and debuggability outweigh the deployment overhead for most use cases.
