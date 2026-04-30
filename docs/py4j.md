# Option E: Py4J (Python Gateway Server)

Py4J lets Java call Python objects running in a separate Python process as if they were local Java objects. A Python process starts a gateway server; Java connects to it over a local socket and calls Python functions through generated proxy objects.

Good fit if you want Python/PyTorch to feel like native Java calls with minimal serialization boilerplate.

## What It Is

Py4J is a communication bridge between Java and Python. Unlike the raw subprocess approach, where Java launches Python, writes data to stdin, and reads from stdout, Py4J provides a higher-level abstraction. Python objects become callable from Java through proxy objects, and the data marshaling happens transparently under the hood.

The Python side starts a gateway server that listens on a local port. The Java side connects to that port and retrieves an entry point object, typically a Python class instance that exposes the functionality you want. From that point, calling methods on the entry point looks like calling any other Java method, even though the execution is happening inside the separate Python process.

## How the Communication Works

When you call a method on a Py4J proxy object in Java, Py4J serializes the method name and arguments into a message, sends it over the local socket to the Python gateway server, which deserializes it, calls the actual Python method, serializes the return value, and sends it back. Java deserializes the response and returns it to the caller. All of this happens transparently.

The key constraint is that only certain types can cross the bridge: primitives, strings, lists, and dictionaries. A raw torch.Tensor object cannot be handed to Java as-is. It must be converted to a Python list first, which then crosses the bridge as a Java list of floats. On the way back in, Java passes a list of floats which Python converts back to a tensor. This conversion adds some overhead but is manageable for typical inference inputs and outputs.

## Compared to Raw Subprocess

The subprocess approach requires you to design your own communication protocol, deciding what format to use, how to handle errors, and how to signal the end of a response. Py4J provides all of that infrastructure. The tradeoff is that Py4J introduces a dependency on both sides (the Python py4j package and the Java py4j library), and the socket-based communication has its own overhead.

For simple, occasional calls, raw subprocess is often simpler. For more complex interactions, such as calling multiple different Python functions, passing varied argument types, and handling responses with structure, Py4J's abstraction reduces boilerplate significantly.

## Bidirectionality

One feature Py4J has that raw subprocess does not is bidirectional calling. Not only can Java call Python, but Python can also call back into Java. This means your Python code can invoke Java methods, use Java objects, and interact with your Java application's state directly. This is useful for patterns where Python needs to report progress, log events, or query Java-side data during a long-running operation.

## Process Lifecycle

The Python gateway server must be running before the Java application tries to connect. In development this means starting the Python process manually. In production, your Java application typically manages the Python process lifecycle, starting it at application startup and stopping it at shutdown. This requires the same process management logic you would write for the subprocess approach, plus the Py4J connection setup on top.

## Tradeoffs

Py4J works well for relatively infrequent calls where the abstraction value outweighs the overhead. For high-frequency, low-latency inference, such as calling PyTorch hundreds of times per second, the socket round-trip and serialization overhead accumulates. In those cases, the Apache Arrow approach (for data throughput) or DJL/ONNX Runtime (for eliminating Python entirely) are better choices.

Py4J is well-established and used in production by tools like Apache Spark's PySpark, which uses Py4J to bridge Python and Spark's Java core. Its stability and real-world usage at scale are reassuring signals for production use.
