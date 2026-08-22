package ld;

import java.lang.invoke.MethodHandle;

import sys.jvm.abi;
import sys.jvm.memory;
import sys.jvm.shared_object;
import sys.jvm.unsafe;
import sys.jvm.abi.call_convention;
import sys.jvm.type.cxx_type;
import sys.jvm.type.cxx_type.function_signature;
import sys.jvm.type.cxx_type.pointer;

public class libld
{
	public static abstract class assembly_syntax
	{
		public static final int ASM_SYNTAX_ATT = 0;
		public static final int ASM_SYNTAX_INTEL = 1;
	};

	private static final long _libld;

	private static final MethodHandle dynamic_link_target;
	private static final MethodHandle dynamic_link_o;
	private static final MethodHandle dynamic_symbol_lookup;
	private static final MethodHandle free_dynamic_lib;

	private static final MethodHandle free_array;

	public static final long global_dynamic_linker;

	static
	{
		_libld = shared_object.dlopen("libld");
		// 查找库函数
		dynamic_link_target = shared_object.dlsym(_libld, function_signature.of("dynamic_link_target", cxx_type.pvoid, cxx_type.pvoid, cxx_type.pchar));
		dynamic_link_o = shared_object.dlsym(_libld, function_signature.of("dynamic_link_o", cxx_type._void, cxx_type.pvoid, cxx_type.pvoid));
		dynamic_symbol_lookup = shared_object.dlsym(_libld, function_signature.of("dynamic_symbol_lookup", cxx_type.pvoid, cxx_type.pvoid, cxx_type.pchar));
		free_dynamic_lib = shared_object.dlsym(_libld, function_signature.of("free_dynamic_lib", cxx_type._void, cxx_type.pvoid));

		free_array = shared_object.dlsym(_libld, function_signature.of("free_array", cxx_type._void, cxx_type.pvoid));

		// 全局变量
		global_dynamic_linker = unsafe.read_ptr(shared_object.dlsym(_libld, "global_dynamic_linker"));
	}

	/**
	 * 创建动态链接目标
	 * 
	 * @param linker
	 * @param lib_name
	 * @return
	 */
	public static final long dynamic_link_target(long linker, String lib_name)
	{
		try (pointer cstr = memory.c_str(lib_name).auto())
		{
			return (long) dynamic_link_target.invokeExact(linker, cstr.address());
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("call dynamic_link_target() failed", ex);
		}
	}

	public static final long dynamic_link_target(String lib_name)
	{
		return dynamic_link_target(global_dynamic_linker, lib_name);
	}

	/**
	 * 链接汇编出来的.o
	 * 
	 * @param lib_ctx
	 * @param o
	 */
	public static final void dynamic_link_o(long lib_ctx, long o)
	{
		try
		{
			dynamic_link_o.invokeExact(lib_ctx, o);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("call dynamic_link_o() failed", ex);
		}
	}

	/**
	 * 从动态链接器链接的库中查找符号
	 * 
	 * @param lib_ctx
	 * @param sym_name
	 * @return
	 */
	public static final long dynamic_symbol_lookup(long lib_ctx, String sym_name)
	{
		try (pointer cstr = memory.c_str(sym_name).auto())
		{
			return (long) dynamic_symbol_lookup.invokeExact(lib_ctx, cstr.address());
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("call dynamic_symbol_lookup() failed", ex);
		}
	}

	public static final MethodHandle dynamic_symbol_lookup(long lib_ctx, call_convention call_conv, function_signature signature)
	{
		return abi.func(dynamic_symbol_lookup(lib_ctx, signature.function_name), call_conv, signature.func_type);
	}

	public static final MethodHandle dynamic_symbol_lookup(long lib_ctx, function_signature signature)
	{
		return dynamic_symbol_lookup(lib_ctx, call_convention.host, signature);
	}

	/**
	 * 释放动态链接目标
	 * 
	 * @param lib_ctx
	 */
	public static final void free_dynamic_lib(long lib_ctx)
	{
		try
		{
			free_dynamic_lib.invokeExact(lib_ctx);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("call free_dynamic_lib() failed", ex);
		}
	}

	/**
	 * 释放编译的.o内存
	 * 
	 * @param arr
	 */
	public static final void free_array(long arr)
	{
		try
		{
			free_array.invokeExact(arr);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("call free_array() failed", ex);
		}
	}
}
