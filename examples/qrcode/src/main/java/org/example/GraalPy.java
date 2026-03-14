// Adapted from https://github.com/graalvm/graal-languages-demos
//  Copyright (c) 2024, 2026 Oracle and/or its affiliates
//  Licensed under the Universal Permissive License v1.0
//  https://oss.oracle.com/licenses/upl

package org.example;

import java.nio.file.Path;

import org.graalvm.polyglot.Context;
import org.graalvm.python.embedding.*;

public class GraalPy {
    static VirtualFileSystem vfs;

    public static Context createPythonContext(String pythonResourcesDirectory) { // ①
        return GraalPyResources.contextBuilder(Path.of(pythonResourcesDirectory)).build();
    }

    public static Context createPythonContextFromResources() {
        if (vfs == null) { // ②
            vfs = VirtualFileSystem.newBuilder().allowHostIO(VirtualFileSystem.HostIO.READ).build();
        }
        return GraalPyResources.contextBuilder(vfs).build();
    }
}