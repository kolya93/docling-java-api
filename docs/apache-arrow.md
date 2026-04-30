# Option F: Apache Arrow / Shared Memory

Use Apache Arrow's standard in-memory format to pass large tensors and arrays between Java and Python processes with minimal serialization overhead. For scenarios involving large data volumes, Arrow eliminates the bottleneck that JSON or file-based communication creates.

## The Problem Arrow Solves

Every approach where Java and Python run as separate processes faces the same fundamental challenge: data must be copied from one process's memory into a format that can be transmitted to the other, then copied again into the receiving process's memory. For small payloads like a few hundred floats, this overhead is negligible. For large tensors, a batch of images, a document embedding matrix, a large feature table, it becomes the dominant cost.

JSON is particularly wasteful for numerical data. A 32-bit float stored in memory as four bytes becomes something like "3.14159265" in JSON, ten or more bytes of text, plus the CPU time to convert between the two representations in both directions.

## What Apache Arrow Is

Apache Arrow is an open standard for representing columnar data in memory. It defines a specific binary layout for arrays, tables, and tensors that is designed to be the same in every language and on every platform. Because Java and Python agree on exactly how the bytes are arranged, neither side needs to convert anything, they just point at the same memory and interpret it directly.

Both Java (via the Arrow Java library) and Python (via PyArrow) implement this standard. A float array laid out in Arrow format in Python's memory is bit-for-bit identical to an Arrow float array in Java's memory. This is what makes zero-copy data sharing possible.

## Two Approaches

### Arrow IPC (Inter-Process Communication)

The more practical of the two approaches. Java and Python exchange Arrow-formatted batches of data over a Unix domain socket or a TCP connection. Each side serializes its data into Arrow's IPC wire format (which is very close to the in-memory format, minimal conversion), sends it over the socket, and the receiving side maps it directly into its own memory.

The "zero-copy" claim refers to what happens on each side: within a single process, reading an Arrow buffer requires no copying, you work directly with the raw bytes. The data does still cross the process boundary once, but the format conversion that dominates the cost in JSON-based approaches is eliminated.

### Plasma Shared Memory Store

The more advanced approach, and the one that achieves true zero-copy end-to-end. Plasma is a shared memory object store (originally part of Apache Arrow, now being deprecated in newer versions) that both Java and Python processes attach to. Data written to the Plasma store lives in physical memory that is mapped into both processes' address spaces simultaneously. Neither process ever copies the data, they both work on the exact same physical RAM pages.

This is the fastest possible data exchange between processes, but it comes with operational complexity: the Plasma store process must be running and managed, memory limits must be configured, and object IDs must be coordinated between processes. For most use cases the IPC approach is sufficient.

## When Arrow Is Worth the Complexity

The overhead of setting up Arrow-based communication is higher than a simple subprocess. It's worth that overhead when the data volumes justify it, roughly when individual payloads are larger than a few hundred kilobytes, or when you're making many calls per second and serialization is measurably your bottleneck.

For typical inference use cases, passing a few tensors, getting a result back, simpler approaches like subprocess or Py4J are easier to set up and maintain. Arrow becomes relevant for batch processing pipelines, streaming inference on large datasets, or multi-process data preprocessing pipelines where the same data is shared between several workers.

## Tradeoffs

Arrow is a well-maintained open standard backed by the Apache Software Foundation and used heavily in the data engineering world (Pandas, Spark, DuckDB, and many others use it internally). The Java and Python libraries are mature. The main cost is conceptual overhead, understanding Arrow's data model and the IPC protocol requires more initial learning than simpler communication approaches. For teams already using Arrow or Parquet in their data infrastructure, the incremental cost of adopting it here is low.
