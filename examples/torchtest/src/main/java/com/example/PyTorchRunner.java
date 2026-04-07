package com.example;

import org.graalvm.polyglot.*;
import java.nio.file.*;

public class PyTorchRunner {
    public static void main(String[] args) throws Exception {

        // Path to your python script (relative to project root)
        String scriptPath = "python/script.py";
        String source = Files.readString(Path.of(scriptPath));

        try (Context context = Context.newBuilder("python")
                .allowAllAccess(true)           // Required for C extensions like torch
                .allowIO(IOAccess.ALL)           // Required for file access
                .allowNativeAccess(true)         // Required for libtorch native libs
                .build()) {

            context.eval("python", source);
        }
    }
}