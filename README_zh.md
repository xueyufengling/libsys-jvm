[English](README.md)

## libsys-jvm

    libsys-jvm为OpenJDK 21+提供了纯Java/纯C++的JVM内部系统级操作。<br>
    Java实现包括完全受信的反射、句柄操作，内部的Unsafe操作，以及基于Panama实现的高性能Java层FFI和配套的C++类型、对象模型解析。<br>
    C++实现提供跨进程操作目标JVM、在目标JVM调用方法等操作，并且提供基于Panama的up call函数获取。<br>

# 许可

    libsys-jvm的Java实现以LGPL-3.0许可进行分发。<br>
    libsys-jvm的C++实现以LGPL-3.0 Linking Exception许可进行分发，其在原LGPL-3.0许可的基础上移除了您在静态链接时必须提供最小对应源代码的义务。简而言之，只要您不修改本库的源代码，不论您是静态链接还是动态链接，都无需承担开源义务。<br>