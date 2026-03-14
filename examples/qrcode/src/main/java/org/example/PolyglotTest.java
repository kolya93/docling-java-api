// Adapted from https://github.com/graalvm/graal-languages-demos
//  Copyright (c) 2024, 2026 Oracle and/or its affiliates
//  Licensed under the Universal Permissive License v1.0
//  https://oss.oracle.com/licenses/upl

package org.example;
import org.graalvm.polyglot.*;

public class PolyglotTest {
    public static void main(String[] args) {
        try (Context ctx = Context.create()) {
            ctx.eval("python", "print('Hello from GraalPython')");
        }
    }
}