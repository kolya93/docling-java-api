# Porting Docling to GraalPy — Status & Notes

This document tracks the effort to install and run [IBM's Docling](https://github.com/docling-project/docling) on [GraalPy 25.0.2](https://github.com/oracle/graalpython), specifically within a WSL2 (Ubuntu) environment on Windows.

---

## Environment

- **OS**: Windows 11 with WSL2 (Ubuntu)
- **GraalPy version**: 25.0.2 (Python 3.12.8, Oracle GraalVM Native)
- **CPython version**: 3.10
- **Docling version**: 2.81.0
- **RAM**: 16GB (14GB free during testing)
- **GraalPy installed via**: pyenv

---

## What We Tried

### 1. Direct install via `graalpy -m pip install docling`

The most straightforward approach. GraalPy's pip correctly identified that PyTorch has no pre-built GraalPy wheel and attempted to compile it from source (286MB source tarball). GraalPy automatically applied its own patches to PyTorch's C++ source before building.

**Result**: Killed at the "Installing build dependencies" step. Reproduced consistently across multiple attempts.

---

### 2. Install PyTorch separately via the PyTorch CPU wheel index

```bash
graalpy -m pip install torch --index-url https://download.pytorch.org/whl/cpu
```

**Result**: Failed. Using `--index-url` replaces the default PyPI index entirely, so pip could not find `wheel` or `setuptools` from the PyTorch-only index.

---

### 3. Install PyTorch with `--extra-index-url`

```bash
graalpy -m pip install wheel setuptools
graalpy -m pip install torch --extra-index-url https://download.pytorch.org/whl/cpu
```

**Result**: Same kill at "Installing build dependencies". The process is terminated by the OS before compilation even begins.

---

### 4. Check GraalPy's own pre-built wheels index

Checked https://www.graalvm.org/python/wheels/ for a pre-built PyTorch wheel.

**Result**: PyTorch is not available. The GraalPy wheels index currently provides only:
`contourpy`, `httptools`, `jiter`, `kiwisolver`, `numpy`, `oracledb`, `psutil`, `pydantic-core`, `ujson`, `uvloop`, `watchfiles`, `xxhash`, `polyleven`

---

## What Works

- **GraalPy installation** via pyenv works cleanly on WSL2
- **Docling itself is pure Python** — no `.so`/`.dll`/`.dylib` files in the docling package; all native code is in its dependencies
- **Most of docling's dependencies** appear compatible with GraalPy; the sole blocker identified so far is PyTorch
- **pydantic-core** has a pre-built GraalPy wheel available at the GraalPy wheels index, so Pydantic (heavily used throughout docling) should install without issues

