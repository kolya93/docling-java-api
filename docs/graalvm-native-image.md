# Option L: GraalVM Native Image

Compile your Java application ahead-of-time into a standalone native binary. The result starts in milliseconds, uses less memory than a JVM-based application, and requires no Java installation on the target machine.

## What Native Image Is

Standard Java applications run on the JVM, which is itself a program that interprets or JIT-compiles your bytecode at runtime. Starting a JVM takes time (typically 300-500ms for a simple application, longer for applications with many dependencies). The JVM also uses memory for its own infrastructure, the JIT compiler, the class loader, the garbage collector metadata.

GraalVM Native Image takes a different approach. It analyzes your entire application at build time, your code, all your dependencies, and the JVM libraries you use, and compiles everything down to a native machine code binary. The result is a single executable file that runs directly on the operating system without a JVM. It starts in tens of milliseconds and uses less memory because there's no JVM infrastructure at runtime.

## The Build-Time/Runtime Tradeoff

Native Image works by doing at build time what the JVM normally does at runtime. The JIT compiler in a standard JVM observes which code paths are actually used during execution and optimizes them based on runtime profiling. Native Image doesn't have that luxury, it must compile everything ahead of time without knowing which paths will be hot.

This means Native Image applications typically have lower peak throughput than the same application running on a well-warmed JVM for long-running workloads. The JVM's adaptive JIT compilation, given enough time, produces highly optimized code. Native Image produces good code but not JIT-adaptive code.

The practical consequence: Native Image is a strong win for short-lived applications (CLI tools, serverless functions) where the JVM never gets warm enough for JIT to matter, and a more nuanced tradeoff for long-running servers where JIT optimization would eventually pay off.

## The Closed-World Assumption

Native Image requires what it calls a "closed world", at build time, it must be able to see all the code that will ever be executed. This is a stronger requirement than standard Java, which can load classes dynamically at runtime from arbitrary locations.

The specific features that break this assumption are reflection (accessing classes, fields, or methods by name as strings), dynamic class loading (loading .class files at runtime), JNI (calling native code), and resources loaded from the classpath at runtime. All of these are common patterns in Java libraries.

To handle them, Native Image requires configuration files that declare which classes are accessed reflectively, which resources must be included, and which native methods are called. These configurations must be accurate and complete, anything not declared may be silently absent at runtime, causing failures that only manifest when that code path is reached.

## Generating Configuration Automatically

GraalVM ships a tracing agent that can generate these configuration files automatically. You run your application normally under the JVM with the agent attached, exercise every code path you care about, and the agent records every reflection access, resource load, and JNI call. The resulting configuration files can then be fed to the Native Image build.

The quality of the generated configuration depends entirely on how thoroughly you exercise the application during the tracing run. Code paths that aren't executed during tracing won't be recorded, and they won't work in the native binary. This is why Native Image is easiest for applications with simple, deterministic behavior and harder for frameworks that use extensive reflection for flexibility.

## ML Inference and Native Image

The most viable combination of ML inference and Native Image is ONNX Runtime. ONNX Runtime's Java API has reasonable Native Image support, its reflection usage is bounded and configurable. The .onnx model file can be bundled inside the binary as a classpath resource, producing a genuinely single-file deployment.

DJL's support for Native Image is more limited. DJL uses reflection and dynamic class loading extensively for its engine discovery mechanism, and getting it working with Native Image requires significant manual configuration effort.

## When It Makes Sense

The value proposition of Native Image is clearest for applications that start, do a defined task, and exit. A command-line document processor built on ONNX Runtime would benefit: instead of waiting half a second for the JVM to start before processing begins, the binary starts immediately. In serverless environments like AWS Lambda, where cold start time directly affects cost and response latency, Native Image can be transformative.

For long-running inference servers that stay up for hours or days, the startup time advantage matters less, and the peak throughput disadvantage relative to JIT compilation may matter more. The decision depends on measuring both in your specific context.

## Tradeoffs

Native Image imposes a slow build process (typically 2-5 minutes per build) that makes rapid iteration painful during development. It is best introduced late in a project's lifecycle, after the application is relatively stable. The configuration requirements for reflection and resources add ongoing maintenance burden as dependencies change. But for the right use case, especially CLI tooling or serverless deployment, the fast startup and reduced memory footprint make it worth the investment.
