[中文](README_zh.md)

## libsys-jvm

    libsys-jvm provides system-level internal operations for OpenJDK 21+, featuring both pure Java and pure C++ implementations.<br>
    The Java implementation includes fully trusted reflection, handle manipulation, internal Unsafe operations, high-performance Java-layer FFI based on Project Panama, and a supporting C++ type and object model parser.<br>
    The C++ implementation enables cross-process manipulation of the target JVM, method invocation within the target JVM, and the retrieval of upcall functions based on Project Panama.<br>

# License

    libsys-jvm Java implementation is distributed under the LGPL-3.0.<br>
    libsys-jvm C++ implementation is distributed under the LGPL-3.0 with Linking Exception. This license removes the obligation to provide Minimal Corresponding Source when statically linking, which is otherwise required by the original LGPL-3.0 license. In short, as long as you do not modify the source code of this library, you are not required to open-source your code, regardless of whether you choose static or dynamic linking.<br>