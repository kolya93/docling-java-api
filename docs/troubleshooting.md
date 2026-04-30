# Troubleshooting Guide

This guide documents the specific errors encountered when setting up PyTorch and GraalVM on WSL2, along with their root causes and fixes. The errors are presented in the order they occurred during a real setup session.

## Error 1: wheel Not Found

What happened: Running pip with a custom index-url pointing to the PyTorch CPU wheel repository caused pip to fail when trying to install wheel, a build tool it needs to compile packages from source.

Root cause: The index-url flag replaces PyPI as pip's primary package index. It does not add an additional index; it substitutes the default one. The PyTorch CPU index exists only to serve pre-compiled PyTorch wheels and does not carry general-purpose Python build tools like wheel, setuptools, or pyyaml. Any package not in that index cannot be found.

Fix: Use extra-index-url to add PyPI as a fallback without replacing the primary index. With both indexes configured, pip can find PyTorch wheels from the PyTorch index and general-purpose packages from PyPI.

## Error 2: pyyaml Not Found in Build Subprocess

What happened: Even after adding PyPI as an extra index, pip failed during the "Installing build dependencies" step, reporting that pyyaml could not be found.

Root cause: This is one of the more confusing pip behaviors. When pip builds a package from source, it spawns a completely isolated subprocess to install that package's build dependencies. This subprocess is isolated by design, with its own clean environment to avoid interference from packages in your virtual environment. The subprocess inherits the index-url setting from the parent pip invocation, but not extra-index-url.

The subprocess therefore only sees the PyTorch CPU index, which has no pyyaml. Installing pyyaml into your virtual environment beforehand has no effect because the subprocess cannot see your virtual environment at all.

Fix: Adding extra-index-url does eventually propagate to the subprocess in newer pip versions, but whether it works depends on the pip version. Pre-installing all of PyTorch's build dependencies (pyyaml, numpy, astunparse, setuptools, wheel) into the virtual environment before attempting to install torch can help, but the subprocess isolation means this is not guaranteed to work.

The deeper fix is that this problem only arises because there is no pre-built GraalPy wheel for PyTorch, forcing pip to attempt a source build. If a binary wheel existed, the build subprocess would never be invoked. See Error 4.

## Error 3: OOM Kill (Exit Code -9)

What happened: After pyyaml was resolved, pip attempted to compile numpy from source as a build dependency. The compilation process was killed by the operating system with exit code -9.

Root cause: Exit code -9 is SIGKILL, the Linux kernel's way of forcibly terminating a process that has consumed too much memory. Compiling numpy from source is memory-intensive: the C compiler must hold multiple large compilation units in memory simultaneously. WSL2 enforces a hard memory limit on the Linux subsystem, defaulting to approximately half of the machine's physical RAM or 8GB, whichever is smaller. The compiler exceeded this limit.

Fix: Increase WSL2's memory limit by creating a .wslconfig file in the Windows user home directory with a higher memory and swap allocation, then restarting WSL. The memory limit should be set below the machine's total physical RAM to leave room for Windows itself. This addresses the memory issue but not the underlying cause, which is that numpy is being compiled from source unnecessarily.

A more direct fix is to install numpy separately using the only-binary flag, which forces pip to use a pre-compiled wheel and refuse to build from source. GraalPy's own wheel repository has a pre-compiled numpy wheel. Once numpy is installed as a binary, the build subprocess no longer tries to compile it.

## Error 4: No Pre-Built Torch Wheel for GraalPy

What happened: Every approach to installing torch resulted in pip downloading the PyTorch source tarball (286MB) and attempting to build it from source, regardless of flags used.

Root cause: This is the fundamental blocker for the GraalVM Polyglot approach. Pre-built Python wheels are platform-specific: they are compiled for a specific Python implementation (CPython), a specific version of the Python ABI (application binary interface), and a specific operating system and architecture. The PyTorch CPU wheel repository contains wheels for CPython on Linux, Mac, and Windows. GraalPy is a different Python implementation with a different ABI, so CPython wheels are not compatible with it.

GraalPy has its own wheel repository that contains wheels compiled specifically for GraalPy's runtime. This repository includes numpy, scipy, and some other scientific packages, but not torch. When pip cannot find a compatible binary wheel anywhere, it falls back to downloading the source distribution and compiling it. For torch, this compilation is an enormous undertaking that fails due to the dependency resolution issues described in Errors 2 and 3.

Fix: There is no fix available as of GraalPy 25.0.2. A pre-built GraalPy wheel for torch would need to be created and published by Oracle (the GraalPy maintainers) or the PyTorch team. Until that happens, the GraalVM Polyglot approach cannot be used for PyTorch. Use one of the alternative approaches documented elsewhere.

## Error 5: Packages Installing to Base Interpreter Instead of Virtual Environment

What happened: After installing packages with pip, pip show reported the package's location as the base GraalPy installation directory rather than the virtual environment directory.

Root cause: The virtual environment was not actually active. When pyenv is installed (a Python version manager), running pip resolves through pyenv's shim system. Pyenv shims intercept commands like pip and python and redirect them to whichever Python version pyenv has selected as active, not necessarily the virtual environment you intended to use.

Activating a virtual environment normally modifies the PATH environment variable to put the virtual environment's bin directory first, so that pip resolves to the virtual environment's pip rather than pyenv's shim. If this modification does not happen, whether because the virtual environment was never created, the activation script was not run, or a previous shell session's activation is no longer in effect, pip resolves to the shim and packages install to the base interpreter.

Fix: Verify that the virtual environment is active by running "which pip" and confirming it points to a path inside the virtual environment directory. If it points to a pyenv shim path, the virtual environment is not active. Activate it explicitly with the source activate command. Always verify with "which pip" rather than relying on the shell prompt prefix, which can be misleading.

## Error 6: Virtual Environment Lost After Shell Restart

What happened: After closing and reopening the WSL terminal, the virtual environment was no longer active despite having been active in the previous session.

Root cause: Virtual environment activation is session-specific. The source activate command modifies environment variables in the current shell session, primarily PATH and VIRTUAL_ENV. These modifications exist only in memory and disappear when the shell exits. The virtual environment directory itself persists on disk, but the shell configuration that makes it active does not carry over between sessions.

Fix: Re-run the activation command at the start of each session. For a more persistent solution, add the activation command to the shell's startup file (.bashrc or .bash_profile). Be aware that auto-activating a virtual environment in the startup file affects all shell sessions, which can cause confusion if you work with multiple Python environments. A more targeted solution is to use a directory-based activation tool that automatically activates the appropriate environment when you enter a project directory.

## Error 7: force-reinstall Installing to Wrong Location

What happened: Running pip install with the force-reinstall flag still installed the package to the base GraalPy installation rather than the virtual environment.

Root cause: Same underlying cause as Error 5. The virtual environment was not active, so the pip command resolved to the base interpreter's pip. The force-reinstall flag changes the installation behavior (reinstall even if the package is already installed at the current version) but does not change which Python environment the package is installed into. It is not a way to target a specific virtual environment.

Fix: Activate the virtual environment first, then run the install command. Verify activation with "which pip" before installing.

## General Diagnostic Checklist

When something goes wrong with pip and Python environments, run through these checks before attempting any fix.

Confirm which pip is active by running "which pip". The path should point inside your virtual environment directory. If it points to a pyenv shim or a system Python directory, the virtual environment is not active.

Confirm whether a virtual environment is active by checking the VIRTUAL_ENV environment variable. If it is empty or unset, no virtual environment is active.

Confirm where pip installs packages by checking the Location field in "pip show pip". This should match your virtual environment directory.

Confirm that "which python" and "which pip" point to the same environment. If they point to different locations, something is misconfigured.

All four of these should be consistent and should all point to the same virtual environment directory when a virtual environment is properly activated.
