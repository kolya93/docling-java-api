# Docling PDF-to-Markdown Conversion: Session Notes

## Goal

Trace the Python imports of the `docling` CLI tool and convert a PDF to markdown.

## Problem: Tracing Imports of a CLI Entry Point

`docling` is not a Python module runnable with `python -m`, it is a setuptools entry point script. To trace its imports, the underlying script needs to be invoked directly with Python.

The script at `~/.local/bin/docling` contains:

```python
#!/usr/bin/python3
import re
import sys
from docling.cli.main import app
if __name__ == '__main__':
    sys.argv[0] = re.sub(r'(-script\.pyw|\.exe)?$', '', sys.argv[0])
    sys.exit(app())
```

## Problem: Wrong Python Interpreter

Running `python3 docling` initially failed:

```
ModuleNotFoundError: No module named 'docling'
```

This was because `python3` resolved to a `pyenv` shim pointing to a different Python version, while docling was installed under `python3.10`. The fix was to invoke `python3.10` explicitly:

```
python3.10 -X importtime ~/.local/bin/docling --to md quiz4_practice_part2.pdf
```

## Import Tracing

Used Python's built-in `-X importtime` flag to trace all imports at startup. Output was saved to a file:

```bash
python3.10 -X importtime docling --to md quiz4_practice_part2.pdf 2>&1 | tee imports.txt
```

The output format is:
```
import time: self [us] | cumulative | imported package
```

Where `self` is the time for that module alone and `cumulative` includes all sub-imports.

To find the slowest imports:
```bash
grep "import time" imports.txt | sort -t'|' -k2 -rn | head -30
```

## Key Findings from Import Trace

The two heaviest dependencies imported by docling are:

- **`torch`** — PyTorch core, including submodules for CUDA, distributed training, neural networks, serialization, and data utilities. `torch._C` alone took ~168ms.
- **`torchvision`** — Companion CV library, importing the full model zoo including ResNet, VGG, object detection (Faster R-CNN, FCOS, RetinaNet), segmentation, and video models. Total cumulative load ~61ms.

The import tree for these two libraries spanned over 1,200 lines.

## PDF Conversion

The command to convert a PDF to markdown:

```bash
docling --to md yourfile.pdf
```

Or using the explicit interpreter:

```bash
python3.10 ~/.local/bin/docling --to md yourfile.pdf
```

## Next Steps

- Verify the output markdown from `quiz4_practice_part2.pdf` looks correct.
- Analyze the full `imports.txt` for a complete picture of all docling dependencies.
- Identify the heaviest imports by cumulative time to explore potential startup optimizations.
- Run the conversion pipeline on additional PDFs to solidify the workflow.
