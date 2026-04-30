# Option J: ONNX Runtime in Java

Export your PyTorch model to the ONNX format once, then load and run it in Java with no Python at runtime. Microsoft's ONNX Runtime Java API is mature, actively maintained, and among the most practical production paths for PyTorch model deployment in Java.

One of the most practical production paths. Recommended alongside DJL.

## What ONNX Is

ONNX (Open Neural Network Exchange) is an open standard for representing machine learning models. It defines a portable file format, the .onnx file, that describes a model's computation graph in a way that is independent of the framework used to train it. A model trained in PyTorch, TensorFlow, scikit-learn, or any other ONNX-supporting framework can be exported to ONNX format and then run by any ONNX Runtime implementation.

Think of ONNX like a PDF for machine learning models. Just as a PDF can be created by many different applications and viewed by any PDF reader, an ONNX file can be created by many different ML frameworks and run by any ONNX Runtime. The format standardizes the representation of operations such as convolutions, matrix multiplications, activations, and normalizations, so that a runtime can execute them without knowing anything about the original framework.

## The Export Step

Converting a PyTorch model to ONNX is done once in Python, typically as part of the training or model preparation workflow. PyTorch has built-in support for ONNX export. The export process traces the model's execution, running it once with dummy inputs and recording every operation performed, and saves that trace as an ONNX graph. The resulting .onnx file contains both the computation graph and the trained weights.

This is a one-time step. Once the .onnx file exists, Python is no longer needed. The file is the artifact you deploy alongside your Java application.

## ONNX Runtime

ONNX Runtime is a high-performance inference engine for ONNX models, developed and maintained by Microsoft. It has official bindings for Java, Python, C#, C++, and several other languages. The Java binding includes the native runtime as a bundled dependency. You declare it in your Maven or Gradle build file and it is downloaded and configured automatically, including the native shared libraries for your operating system.

Loading and running an ONNX model in Java is straightforward: you create a session from the .onnx file path, create tensors from your Java arrays, run the session with the input tensors, and read values back from the output tensors. The session is expensive to create and should be reused across many inference calls.

## Execution Providers

ONNX Runtime's performance can be extended through execution providers, which are plugins that route specific operations to specialized hardware or libraries. The default execution provider runs all operations on the CPU using generic implementations. Additional execution providers can route operations to CUDA (NVIDIA GPU), DirectML (Windows GPU), TensorRT, OpenVINO, and others. Switching execution providers is typically a single configuration change with no other code changes required.

## What Can and Cannot Be Exported

ONNX export works well for the vast majority of standard PyTorch models: feedforward networks, convolutional networks, transformers, and recurrent networks. The operations they use, such as linear layers, attention, normalization, and convolutions, are all part of the ONNX standard and are supported by ONNX Runtime.

Some patterns export less cleanly. Models with dynamic control flow, such as if statements or loops whose behavior depends on the input data rather than being fixed at compile time, can be difficult to export because ONNX's graph format is static. Models that use custom C++ extensions may not be exportable at all if those extensions do not have ONNX equivalents. Very new PyTorch operations may not yet have ONNX representations in older opset versions.

When a model cannot be exported cleanly, the alternatives are to use DJL (which loads TorchScript directly) or the native JNI/JNA approach (which calls libtorch's full API).

## Relationship to Docling

Docling uses multiple ML models internally for document understanding, including layout analysis, table structure recognition, and OCR. These are standard deep learning models that in principle can be exported to ONNX. However, Docling's full document processing pipeline involves more than just running these models: it includes preprocessing, postprocessing, coordinate transformations, and business logic that is not easily encapsulated in a single ONNX graph.

For the full Docling pipeline, the Long-Running Worker approach is more practical. ONNX Runtime is relevant if you want to use Docling's specific ML models in isolation, for example running just the layout detection model as part of a custom Java document processing pipeline.

## Tradeoffs

ONNX Runtime is among the easiest paths for running PyTorch models in Java. The export step adds a small amount of workflow overhead, but the resulting deployment is clean: no Python, no subprocess, no network, just a .onnx file and a Java dependency. The main limitation is that it is inference-only. You cannot train or fine-tune a model through ONNX Runtime. For any project where "train in Python, deploy in Java" is the workflow, ONNX Runtime is an excellent fit.
