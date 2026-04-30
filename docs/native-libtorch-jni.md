# Option G: Native libtorch + JNI/JNA (Reverse Embedding)

Call libtorch's C++ API directly from Java, cutting Python out of the picture entirely. Instead of Java calling Python which calls C++, you call C++ directly from Java. This is the highest-performance architecture available.

Best fit for maximum performance, CUDA control, or custom C++ ops.

## The Core Insight

Python's role in a typical PyTorch program is almost entirely orchestration. Setting up tensors, calling operations, reading results back: none of this computation happens in Python. It all happens in C++ and CUDA code inside libtorch. Python is a thin layer that decides what to call and when.

Java is a faster orchestration language than Python for most purposes. It has lower overhead per method call, better multithreading, and no global interpreter lock. Replacing Python as the orchestrator while keeping the same native C++ compute engine is theoretically the most efficient possible architecture: the best orchestrator (Java) directing the best compute engine (libtorch/CUDA).

## What libtorch Is

libtorch is the C++ library that underlies all of PyTorch. When you install PyTorch in Python, you are getting Python wrappers around libtorch. libtorch can be downloaded separately as a standalone C++ library and does not require Python at all. It contains the tensor data structures, the autograd engine, the operator library, TorchScript model loading, and the CUDA integration.

## JNI vs JNA

There are two mechanisms for calling native code from Java.

JNI (Java Native Interface) is the traditional, lower-level approach. You write a C or C++ bridge file that uses special JNI-defined function signatures and data types to communicate with the JVM. This bridge is compiled into a shared library (.so on Linux, .dll on Windows) that Java loads at runtime. JNI gives you full control but requires writing and maintaining C/C++ code.

JNA (Java Native Access) is a higher-level alternative that lets you call C functions from Java without writing any C code at all. You declare the function signatures you want to call in Java, and JNA handles the calling convention and type marshaling automatically. JNA works with C APIs and cannot directly call C++ APIs with name-mangled symbols, which is why a thin C wrapper around libtorch is still usually needed.

## The Role of DJL

DJL (see djl.md) is essentially a well-maintained, production-tested implementation of this exact pattern. It has already written the JNI bridge to libtorch, handled the platform-specific compilation, managed the native library download and loading, and wrapped everything in a clean Java API. For the vast majority of use cases, DJL is the right choice because it gives you most of the benefit of this approach with a fraction of the implementation cost.

Rolling your own JNI/JNA bridge makes sense when you need to use specific libtorch APIs that DJL does not expose, when you have custom C++ extensions that need to be integrated, or when you need absolute control over memory layout and calling conventions.

## Zero-Copy Tensor Passing

One of the key performance advantages of the JNI approach is the ability to pass tensor data without copying it. When Java has a float array and needs to run a computation on it, the JNI bridge can pin the Java array in memory (preventing the garbage collector from moving it) and create a libtorch tensor that points directly at that memory. The C++ computation operates on the original Java memory without copying. This eliminates one of the main overhead sources in data-intensive inference workloads.

## CUDA Support

libtorch's CUDA support is available through the same JNI bridge. Moving a tensor to the GPU, running operations there, and moving the result back to CPU are all operations that the C++ layer handles natively. From the Java perspective, this is just a matter of calling the right libtorch functions through the bridge. The CUDA-enabled libtorch binaries are a separate download from the CPU-only version.

## Tradeoffs

The performance ceiling of this approach is higher than any Python-based approach. The implementation cost is also significantly higher. You need to be comfortable with C++ and with the JNI calling convention, and you need to manage the build system for the native bridge (typically CMake). Bugs in the native bridge can cause segmentation faults that crash the entire JVM process, which is a qualitatively different failure mode from a Java exception.

For teams with C++ experience who need capabilities beyond what DJL exposes, this approach is well worth the investment. For everyone else, DJL covers the common cases at much lower complexity.
