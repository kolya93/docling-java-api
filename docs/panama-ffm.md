# Option M: Panama / Foreign Function & Memory API

Java 22's built-in mechanism for calling native C functions and managing off-heap memory directly from Java, without writing any C or JNI bridge code. For ML inference, this means calling into libtorch's C API or any native ML library directly from pure Java.

Best fit when no Java binding exists for a native library you need, or when you want precise native memory control.

## What the Problem Was Before Panama

Before Java 22, calling native code from Java required JNI, the Java Native Interface. JNI works, but it requires writing a C or C++ bridge file for every native function you want to call. This bridge must be compiled separately for every target platform, linked against the correct libraries, packaged alongside the Java application, and loaded at runtime. The boilerplate is significant, and bugs in the bridge code can crash the entire JVM.

JNA was a partial improvement, letting you describe C function signatures in Java and call them without writing C code, but it was a third-party library with its own constraints and limitations.

## What Panama Is

The Foreign Function & Memory (FFM) API, known as Project Panama, is Java's official built-in solution to native interop, finalized in Java 22. It provides two capabilities.

The Foreign Function part lets you load a native shared library, look up a function by name, describe its type signature in Java, and call it as if it were a Java method, all without writing any C code. The type descriptions use a Java class hierarchy that maps to C's type system: integers, floats, pointers, and structs.

The Foreign Memory part gives you a new abstraction for off-heap memory, which is memory that lives outside the Java heap and is not managed by the garbage collector. You can allocate it, write to it, read from it, and pass pointers to it into native functions. You can also wrap existing native pointers as Java memory segments, letting you work with memory that native code has allocated.

## Why Off-Heap Memory Matters for ML

Machine learning involves large tensors, arrays of millions of floats. The Java garbage collector is designed to manage Java objects efficiently, but it has overhead: it periodically pauses the application to scan the heap, and it may move objects around in memory. For large float arrays being fed to native inference code, this creates two problems: the GC overhead adds latency, and moving objects means you cannot give stable pointers to native code.

Off-heap memory managed through Panama's Arena API solves both problems. You allocate a chunk of memory outside the Java heap, write your tensor data into it, and pass a pointer directly to the native library. The native code operates on that memory without any GC interference. When the inference is done, you read results back. The memory is freed when you close the Arena.

## The Arena Memory Model

Panama introduces the Arena concept for managing native memory lifetime. An Arena is a scope: when the Arena is open, memory allocated from it is valid and accessible. When the Arena closes, all memory allocated from it is freed immediately.

Arenas come in several varieties. A confined Arena can only be used from a single thread. A shared Arena can be used from multiple threads. A global Arena lives forever. An automatic Arena is freed by the garbage collector when it becomes unreachable.

For ML inference, the natural pattern is a confined Arena per inference call: allocate input and output buffers at the start of the call, run inference, read results, and let the Arena close at the end of the call. This is deterministic, with no memory leaks, no GC pauses, and no dangling pointers.

## C APIs vs C++ APIs

Panama works with C function signatures. C functions have simple, predictable names and calling conventions. C++ functions use name mangling, where their compiled names are complex strings encoding the function's full type signature, which makes them impossible to look up by a simple string name.

libtorch is a C++ library. Its functions have mangled names and use C++ types such as references, templates, and classes. Panama cannot call C++ functions directly. The standard approach is to write a thin C wrapper, a small C++ file that exposes a C-compatible API over the C++ library, and call the C wrapper from Panama. This is less code than a full JNI bridge but still requires some C/C++ work.

## jextract

Oracle ships a tool called jextract that reads a C header file and automatically generates Java Panama bindings for all the functions and types it describes. For libraries with a C API, this eliminates the manual work of describing function signatures in Java. The generated code is verbose but correct, and it updates automatically when the header changes.

For libtorch, whose headers are complex C++ templates, jextract is not directly applicable. But for libraries that expose a clean C API, including ONNX Runtime's C API, jextract can significantly reduce the effort of creating Panama bindings.

## Relationship to JNI

Panama does not make JNI obsolete for all use cases. JNI has been stable and well-understood for decades, and many existing native libraries have JNI bindings that work well. Panama is better for new native interop work, especially when the target library has a C API and no existing Java binding. For mature libraries like ONNX Runtime that already have a JNI-based Java API, using the existing binding is simpler than building a Panama-based alternative from scratch.

## Tradeoffs

Panama is a powerful tool that requires understanding of C's type system and memory model. Using it incorrectly, such as passing a pointer to freed memory, misrepresenting a function's type signature, or writing past the end of an allocated segment, produces undefined behavior that can crash the JVM or silently corrupt data. These are qualitatively different failure modes from most Java programming.

The reward for managing this complexity correctly is the most efficient possible path from Java to native code: direct function calls, direct memory access, and zero unnecessary copying. For libraries that have no Java binding and for workloads where native call overhead matters, Panama is the right tool.
