# Option I: TensorFlow Java

Google's official Java API for TensorFlow. Load and run TensorFlow SavedModels directly in the JVM with no Python and no subprocess. If your model is trained in TensorFlow or Keras, this is the cleanest Java deployment path.

Best fit if your model is trained in TensorFlow, or you are open to retraining.

## What It Is

TensorFlow Java (TF4J) is the official Java binding for TensorFlow, maintained by Google and the TensorFlow community. It wraps the native TensorFlow C library, the same engine that powers Python TensorFlow, and exposes it through a Java API. The model format it uses is TensorFlow's SavedModel: a directory containing the model's computation graph and trained weights in a portable binary format.

The relationship between TF4J and Python TensorFlow is analogous to the relationship between DJL and Python PyTorch. Both replace Python as the orchestration layer while keeping the same native compute engine underneath. The computation itself, including matrix multiplications, activation functions, and loss calculations, runs in the same C++ code regardless of which language is orchestrating it.

## The SavedModel Format

A TensorFlow SavedModel is a directory containing two things: a protocol buffer file describing the model's computation graph, and a variables subdirectory containing the trained weight values. It is portable: the same SavedModel can be loaded by Python TensorFlow, TensorFlow Java, TensorFlow.js, TensorFlow Lite, and TensorFlow Serving.

Keras models are exported to SavedModel format with a single method call at the end of training. The export captures both the architecture and the weights. The SavedModel also records the model's input and output signatures, which are the names and shapes of the tensors it expects and produces, and which the Java loader uses to know how to feed data in and read results out.

## Signatures and Input/Output Names

One of the practical challenges with TF4J is knowing the correct names for the model's inputs and outputs. TensorFlow SavedModels can have multiple signatures (different ways of calling the model), and each signature's inputs and outputs have string names. The Java code must pass data using exactly these names.

The names are determined at export time by the Python code that saves the model. Keras uses default names based on layer names, such as "dense_input" for the first dense layer's input. These can be inspected from the command line using the saved_model_cli tool that ships with TensorFlow, or by loading the model in Python and examining its signatures. Getting these names wrong causes runtime errors that can be confusing until you understand the naming convention.

## Relationship to PyTorch

TF4J is not a viable option if your model is written in PyTorch. You cannot load a PyTorch .pt file with TF4J. The two frameworks are entirely separate ecosystems. If you want to use TF4J with a model originally trained in PyTorch, you would need to convert the model to TensorFlow format, typically via ONNX as an intermediate step (PyTorch to ONNX to TensorFlow). This conversion can be lossy for complex models and adds workflow complexity.

If you are starting a new ML project and know that Java deployment matters, training in TensorFlow/Keras from the start eliminates the conversion step entirely. TF4J then becomes the natural, zero-friction deployment path.

## Tradeoffs

TF4J benefits from Google's long-term investment in TensorFlow's Java ecosystem. The SavedModel format is stable and portable across TensorFlow versions. GPU support is available through an alternative native artifact. The main costs are that models must be in TensorFlow format (not PyTorch), the Java API is more verbose than Python TensorFlow, and the native library download is substantial (around 300MB). For PyTorch-centric teams, DJL or ONNX Runtime are more natural paths.
