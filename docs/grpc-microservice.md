# Option H: gRPC Microservice

Define a typed contract in a proto file, run PyTorch in a Python gRPC server, and call it from Java using generated stub classes over HTTP/2. A binary protocol, strong typing, and built-in streaming make this the most robust service-to-service communication option.

Best fit for production microservice architectures where speed and type safety matter.

## What gRPC Is

gRPC is a remote procedure call framework developed by Google and now maintained as an open-source project. It uses two technologies together: Protocol Buffers (protobuf) as the data serialization format, and HTTP/2 as the transport protocol.

Protocol Buffers are a binary serialization format. Unlike JSON, which represents data as human-readable text, protobuf represents data as compact binary, typically 3 to 10 times smaller and faster to serialize and deserialize. The format is defined in a .proto schema file that both the server and client use as a shared contract.

HTTP/2 is the transport layer. It supports multiplexing (multiple concurrent requests over a single connection), header compression, and built-in streaming, which are significant improvements over HTTP/1.1.

## The Proto File as Contract

The defining characteristic of gRPC, and its main advantage over REST, is that the service contract is defined formally in a .proto file. This file specifies exactly what methods the service exposes, what data each method accepts, and what it returns. Both the Java client and the Python server generate their code from this same file, guaranteeing that they agree on every detail of the interface.

When the proto file changes, both sides regenerate their code. The compiler catches type mismatches immediately. There is no runtime discovery of API shapes, no manual documentation to keep in sync, and no guessing what field names the other side expects. This is the principal advantage over REST with JSON, where the interface is often described only in human-readable documentation that can drift from the implementation.

## Code Generation

gRPC's toolchain reads the .proto file and generates code for both sides. For Java, it generates data classes (representing the request and response messages), a service interface (describing the methods the server must implement), and a stub class (which the Java client instantiates to make calls). For Python, it generates equivalent data classes and a servicer base class that your PyTorch code inherits from and implements.

The result is that calling the Python server from Java looks like calling a local Java method, with typed arguments, typed return values, and exceptions for errors. The fact that execution is happening in a separate Python process across a socket is entirely hidden.

## Streaming

gRPC has built-in support for streaming responses. A single client request can produce a stream of responses from the server, where the server sends results as they become available rather than waiting to assemble a complete response. This is useful for batch inference: instead of waiting for all items in a batch to be processed before returning anything, the Python server can send results for each item as soon as it is done. The Java client processes results incrementally. For long-running document processing jobs, streaming significantly improves responsiveness.

## Compared to REST

REST with JSON is simpler to set up and universally understood. Any HTTP client in any language can call a REST API. Browser-based clients work naturally with REST. For exposing an API to external consumers, REST is almost always the right choice.

gRPC is better for internal service-to-service communication where both sides are controlled, performance matters, and the overhead of a proto schema is acceptable. The binary protocol is faster than JSON for large payloads. The generated stubs eliminate a class of runtime errors that would only manifest as bugs with JSON. The streaming support is a capability REST does not have natively.

## Tradeoffs

The main cost of gRPC is setup complexity. You need to install the protobuf compiler, configure the Maven plugin to run code generation, manage the proto file as a shared artifact between the Java and Python projects, and regenerate code on both sides whenever the interface changes. For a simple project, this overhead may not be justified.

The other consideration is that gRPC requires Python to run as a separate process, so the cold start problem still applies. If Docling or PyTorch's model loading time is a concern, the Long-Running Worker pattern (which keeps the Python process alive) should be used in combination with gRPC, or used instead of it.
