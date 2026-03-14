// Adapted from https://github.com/graalvm/graal-languages-demos
//  Copyright (c) 2024, 2026 Oracle and/or its affiliates
//  Licensed under the Universal Permissive License v1.0
//  https://oss.oracle.com/licenses/upl

package org.example;

import org.graalvm.polyglot.io.ByteSequence;

interface IO {
    BytesIO BytesIO();

    interface BytesIO {
        ByteSequence getvalue();
    }
}