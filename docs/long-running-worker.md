# Option K: Long-Running Python Worker

Keep a single Python process alive for the lifetime of your Java application, loading models once at startup and serving all subsequent requests without restarting. This eliminates the cold start overhead that makes the basic subprocess approach impractical for heavy libraries like Docling or PyTorch.

## The Problem This Solves

When Java launches a Python process to run inference, that process must start the Python interpreter, import all libraries, and load model weights into memory before it can do any useful work. For lightweight scripts, this takes a fraction of a second. For PyTorch, it takes 2-5 seconds. For Docling, which loads a layout analysis model, a table structure model, and an OCR pipeline, it can take 10-15 seconds.

If your Java application spawns a new Python process for each document or each inference request, users wait 10-15 seconds before processing even begins, and that cost is paid again for every single request. The work of loading models is repeated hundreds or thousands of times, even though the models never change between calls.

The long-running worker pattern inverts this: the Python process starts once, loads everything once, and then waits for work. Subsequent calls skip the startup cost entirely and go straight to inference.

## How It Works

The Java application starts the Python process at application startup and keeps a handle to it. Rather than sending a complete "run this script" instruction and waiting for the process to exit, Java sends individual work requests to the already-running process and reads back responses. The Python process runs a loop: wait for a request, process it, send back a result, wait for the next request.

The communication channel between them is typically either the process's standard input/output streams or a local Unix socket. Both options work; the stdin/stdout approach is simpler to set up, and the socket approach is better when you need multiple concurrent requests.

## The Buffering Problem

The most common failure mode with this pattern is output buffering. When Python writes to stdout in a non-interactive context, which is exactly what happens when it's launched as a child process by Java, Python's runtime buffers that output, collecting it internally before actually sending it down the pipe. Java is sitting on the other end of the pipe waiting for a response that Python has already written but hasn't actually transmitted yet. Both processes end up waiting for each other indefinitely.

The fix is to disable Python's output buffering, which can be done either by setting an environment variable before launching the process or by explicitly flushing stdout after every response in the Python code. This must be done consistently, a single response that doesn't flush will cause the entire application to hang.

## Why This Matters for Docling

Docling is a document understanding library that uses multiple deep learning models internally. It is not designed as a web service or a streaming processor, it's a Python library that you call like any other. Running it in a subprocess is the only way to use it from Java. But Docling's initialization time is substantial enough that the per-call subprocess approach is genuinely unusable for any volume of documents.

The long-running worker is the right pattern for Docling specifically. You start one (or more) worker processes at application startup, let them initialize Docling completely, and then feed documents to them one at a time. Processing a document takes whatever time Docling takes to process it, but there's no additional startup overhead layered on top.

## Concurrency

A single worker process handles one request at a time. Python's global interpreter lock (GIL) means that even if the Python code were written to handle concurrent requests, it couldn't do so efficiently for CPU-bound work. For higher throughput, the standard approach is to run multiple worker processes in parallel, a pool of workers that Java distributes work across. Each worker is single-threaded but the pool as a whole processes multiple documents simultaneously.

The Java side manages the pool: tracking which workers are busy, queuing requests when all workers are occupied, handling worker crashes and restarts.

## Crash Recovery

Worker processes can die unexpectedly, an out-of-memory error, an unhandled exception, an external signal. The Java side needs to detect when a worker has died (the process is no longer alive, or reading from its output stream returns nothing) and restart it. The restart pays the cold start cost once but then the worker is healthy again. A robust implementation tracks restart counts and alerts or fails permanently if a worker crashes repeatedly in a short period.

## Tradeoffs

The long-running worker pattern adds lifecycle management complexity that the basic subprocess approach doesn't have, the Java application must start workers, monitor them, restart them on failure, and shut them down cleanly. For simple, infrequent calls where cold start is acceptable, the basic subprocess is simpler. For any production use of Docling or similar heavy libraries, the long-running worker is essentially required.
