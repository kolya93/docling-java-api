# Option D: Jython

Jython is an implementation of Python that runs on the JVM. Python code compiles to Java bytecode and runs inside the same JVM process as your Java code, with no subprocess, no IPC, and no serialization.

Not viable for PyTorch. Jython has no C extension support. This document explains what Jython is actually useful for.

## What It Is

Most people know only one Python implementation: CPython, the standard interpreter written in C. Jython is an alternative implementation written in Java that targets the JVM instead of producing native machine code. When you run a Python script with Jython, it compiles the Python source to Java bytecode and runs it on the JVM just like any Java class.

The appeal is seamless Java/Python interoperability. A Jython script can import and use Java classes directly, as if they were Python modules. A Java program can create a Jython interpreter, run Python code inside it, and pass Java objects to Python and receive Python objects back, all without any serialization or process boundaries.

## Why It Doesn't Work for PyTorch

PyTorch's actual computation engine is C++ code compiled into native shared libraries. These libraries plug into CPython through a mechanism called the Python C API, a set of C functions that extension modules use to interact with the interpreter's internals (reference counting, object types, memory management, and so on).

Jython does not implement the Python C API. It cannot load C extension modules because there is no CPython interpreter for them to plug into. When you try to import torch in Jython, the import fails because Jython cannot load the underlying .so file that contains PyTorch's actual implementation.

This is a fundamental architectural limitation, not a version gap. Jython 3 (which would target Python 3) has been in development for years but has not reached a stable release, and even a complete Jython 3 would still face the same C extension problem for PyTorch specifically.

## What Jython Is Good For

Jython is useful in a narrow set of scenarios where you need pure Python scripting tightly integrated with a Java application, and where your Python code does not depend on any C extensions.

The classic use case is embedding a scripting engine in a Java application. If you want to let users write Python scripts that interact with your application's Java objects, querying data, configuring behavior, or automating workflows, Jython gives you that capability with direct object sharing between the two languages. No serialization, no subprocess management, no network.

Another use case is legacy Python 2 code that needs to run inside a JVM environment, such as a Java-based ETL pipeline that needs to execute existing Python 2 business logic.

## Limitations

Beyond the C extension problem, Jython is also Python 2.7 only. Python 2 reached end of life in 2020, and most modern libraries, including the entire scientific Python stack, have dropped Python 2 support. If you are starting a new project, Python 2 is not a viable target.

The Jython project itself has been largely dormant for extended periods. The last stable release was in 2022. Community support and documentation are thin compared to CPython.

## Verdict for ML Workloads

Jython cannot run PyTorch, numpy, scipy, pandas, scikit-learn, Docling, or any other library that uses C extensions. For any ML use case, use one of the other approaches in this documentation.
