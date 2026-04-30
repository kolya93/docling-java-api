# Model Quantization Guide

Reduce model size and speed up CPU inference by representing weights in lower numerical precision. For Java inference workloads on CPU, especially with Docling's transformer-based models, quantization is often the single most impactful optimization available.

Read this before deploying any model to CPU. It often produces a 2 to 4 times speedup with minimal accuracy loss.

## What Quantization Is

A neural network's weights are numbers. By default, PyTorch stores them as 32-bit floating point values (FP32). Each weight occupies 4 bytes. A model with 100 million parameters, modest by modern standards, occupies 400MB in memory.

Quantization replaces those 32-bit floats with lower-precision representations, typically 8-bit integers (INT8). Each weight now occupies 1 byte. The same 100 million parameter model shrinks to 100MB. More importantly, the arithmetic operations during inference change too: instead of 32-bit floating point multiplications and additions, the CPU performs 8-bit integer operations.

Modern CPUs have SIMD (Single Instruction, Multiple Data) hardware that can process multiple values simultaneously. The wider the SIMD registers relative to the data type, the more values fit. With INT8, a CPU can process four times as many values per instruction as with FP32. The result is typically 2 to 4 times faster inference on CPU, with the speedup depending on the specific model architecture and which operations dominate its runtime.

## The Accuracy Tradeoff

Replacing 32-bit floats with 8-bit integers introduces approximation error. The 256 distinct values an INT8 can represent are mapped to the original float range, and any original value that does not map exactly to one of those 256 points gets rounded. For most operations in neural networks, such as matrix multiplications and dot products, this rounding error is small and has minimal effect on the model's predictions.

The degree of accuracy loss depends on the model and the quantization approach. For classification tasks where you care about the top prediction, quantization typically causes no measurable degradation. For regression tasks where the exact output value matters, the rounding error may be more significant. Measuring accuracy before and after quantization on a representative test set is the only reliable way to know.

## Types of Quantization

Dynamic quantization quantizes only the weights, not the activations. Activations are the intermediate values computed during inference, and they are still computed in float and converted to integer only when needed for a specific operation. This approach requires no calibration data and works well for transformer-based models like those used in Docling, where the weight matrices dominate inference time. It is the easiest form of quantization to apply and a good starting point.

Static quantization quantizes both weights and activations. Because it quantizes activations, it needs to know the typical range of values those activations take. This range is determined during a calibration step: you run the model on a representative sample of real data and collect statistics on the activation values. Static quantization generally achieves better speedup than dynamic quantization because the activation conversions are precomputed, but it requires calibration data and more setup.

Quantization-aware training (QAT) incorporates quantization into the training process itself. The model learns to be robust to quantization noise during training, typically resulting in better accuracy than post-training quantization for the same precision level. QAT is the most involved approach and only applicable when you control the training process.

FP16 (half precision) is a different form of precision reduction that uses 16-bit floating point instead of 8-bit integer. It preserves more numeric range than INT8 and is particularly effective on GPUs, which have hardware acceleration for FP16 arithmetic. On CPU, FP16 is less impactful than INT8.

## The ONNX Runtime Quantization Pipeline

For Java deployment via ONNX Runtime, the recommended quantization workflow happens entirely in Python before deployment. You export the PyTorch model to ONNX, apply quantization to the ONNX model using ONNX Runtime's quantization tooling, validate the quantized model's accuracy, and deploy the quantized .onnx file.

The Java side is completely unaware that quantization has occurred. Loading and running a quantized ONNX model is identical to loading and running an unquantized one, because ONNX Runtime handles the INT8 arithmetic internally. This clean separation between the quantization workflow (Python, build time) and the inference code (Java, runtime) is one of the reasons the ONNX-based pipeline is attractive for Java deployments.

## Quantizing Docling's Models

Docling uses transformer-based models for layout analysis and table structure recognition. Transformer models are a particularly good target for dynamic quantization because their dominant operation, attention, involves large matrix multiplications where the weight matrices dominate. Dynamic quantization of transformer weights typically produces a 1.5 to 2 times speedup with minimal accuracy degradation.

Docling's models are stored in its cache directory after the first run. In principle they can be extracted, quantized, and replaced. In practice, this depends on Docling's current API for specifying custom model paths, which may change between versions. The more robust approach is to use Docling as-is in a long-running worker and rely on its own internal optimizations, rather than trying to replace its models.

## CPU Thread Configuration

Beyond quantization, ONNX Runtime's CPU inference performance is also affected by threading configuration. Each inference call can use multiple threads for parallelism within a single operation (intra-op threads) and across independent operations in the graph (inter-op threads). Setting these appropriately for your hardware, typically matching intra-op threads to the number of physical CPU cores, can provide additional speedup independent of quantization.

## Measuring Before and After

Quantization is an optimization, and like all optimizations it should be measured rather than assumed. The key metrics to capture before and after are model file size, inference latency for a single request, throughput under load (requests per second), and accuracy on a representative test set. The combination of these measurements tells you whether quantization is delivering the expected benefit and whether the accuracy tradeoff is acceptable for your use case.
