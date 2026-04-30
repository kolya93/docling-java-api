# Option C: Deep Java Library (DJL)

Run PyTorch model inference directly in Java with no Python required. DJL is an open-source Java framework from AWS that wraps the native PyTorch C++ library (libtorch) and exposes it through a clean Java API.

## What It Is

DJL (Deep Java Library) is a Java framework for deep learning inference and training. Its PyTorch engine works by bundling or downloading the native libtorch library, the same C++ engine that powers Python's PyTorch, and calling into it from Java using JNI. From the perspective of the actual computation, DJL and Python PyTorch are doing the same thing: both are orchestrating the same native C++ code. DJL simply replaces Python as the orchestration layer.

This matters because Python's role in a PyTorch program is almost entirely orchestration. Setting up tensors, calling operations, reading results, none of this computation happens in Python itself. It happens in C++ and CUDA. Python (or in this case Java) just tells the C++ layer what to do and when. DJL is a production-tested, maintained implementation of that orchestration layer in Java.

## How It Works

When your application first uses DJL's PyTorch engine, DJL downloads the appropriate version of the native libtorch binaries for your operating system and CPU/GPU configuration, caches them locally, and loads them into the JVM process via JNI. Subsequent startups use the cached binaries and are fast. All inference then runs natively in-process, there is no subprocess, no socket, no serialization, and no Python interpreter.

## The Model Format

DJL loads models that have been exported from Python as TorchScript, a serialized, portable representation of a PyTorch model. TorchScript freezes the model's computation graph into a format that can be loaded and executed without the original Python class definition. The exported file has a .pt extension and contains both the model architecture and the trained weights.

Exporting a model to TorchScript is done in Python at training time and is a one-time step. Once exported, the .pt file can be deployed alongside your Java application and loaded by DJL.

## Tensor Operations

DJL exposes a tensor abstraction called NDArray (N-dimensional array), similar in concept to PyTorch's tensor or numpy's ndarray. You can create tensors from Java arrays, perform arithmetic and linear algebra operations, and read values back into Java arrays. The full set of operations available is determined by the underlying libtorch version DJL is using.

## Project Structure

A DJL project follows standard Maven or Gradle conventions. The DJL dependencies are declared in the build file and include the core API, the PyTorch engine, and a native binaries artifact that specifies the platform and whether to use CPU or GPU. The model file lives alongside the application and is loaded at startup.

## First-Run Download

The first time a DJL application runs, it downloads the native libtorch binaries, approximately 500MB for the CPU build, larger for CUDA builds. This download is cached in a local directory and does not repeat on subsequent runs. In production environments where outbound internet access is restricted, you can pre-download the binaries and configure DJL to load them from a local path instead.

## GPU Support

DJL supports CUDA through the CUDA-enabled native binaries artifact. Switching from CPU to GPU inference is a matter of changing which native artifact your build depends on and optionally specifying a CUDA device when creating inference sessions. The rest of your application code is unchanged.

## Tradeoffs

DJL's main constraint is that it is an inference framework. Models must be exported from Python before they can be used, you cannot train a model with DJL in the same way you would in Python, and you cannot run arbitrary Python scripts. If your workflow is "train in Python, deploy in Java," DJL is an excellent fit. If you need to run existing Python scripts that do more than just inference, look at the subprocess or Py4J approaches instead.

The other consideration is that DJL abstracts away libtorch behind its own API. For most inference use cases this is a benefit, the API is clean and well-documented. For cases where you need to use specific low-level PyTorch features that DJL doesn't expose, the Native JNI/JNA approach gives you direct access.
