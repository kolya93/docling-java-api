package org.example;
import org.graalvm.polyglot.*;

public class PolyglotTest {
    public static void main(String[] args) {
        try (Context ctx = Context.create()) {
            ctx.eval("python", "print('Hello from GraalPython')");
        }
    }
}