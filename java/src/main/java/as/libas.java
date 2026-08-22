package as;

import java.lang.invoke.MethodHandle;

import sys.jvm.abi;
import sys.jvm.memory;
import sys.jvm.shared_object;
import sys.jvm.unsafe;
import sys.jvm.abi.call_convention;
import sys.jvm.type.cxx_type;
import sys.jvm.type.cxx_type.function_signature;
import sys.jvm.type.cxx_type.pointer;

public class libas
{
	public static abstract class assembly_syntax
	{
		public static final int ASM_SYNTAX_ATT = 0;
		public static final int ASM_SYNTAX_INTEL = 1;
	};

	private static final long _libas;

	private static final MethodHandle create_new_assembler;
	private static final MethodHandle assembler_add_src;
	private static final MethodHandle assemble_unit;
	private static final MethodHandle assembler_clear_unit;
	private static final MethodHandle free_assembler;
	private static final MethodHandle create_new_disassembler;
	private static final MethodHandle disassemble_text;
	private static final MethodHandle disassemble_o;
	private static final MethodHandle disassembler_find_return;
	private static final MethodHandle disassembler_find_call;
	private static final MethodHandle disassembler_find_opcode;

	private static final MethodHandle free_array;

	public static final long host_architecture_context;
	public static final long host_assembler;
	public static final long host_disassembler;

	static
	{
		_libas = shared_object.dlopen("libas");
		// 查找库函数
		create_new_assembler = shared_object.dlsym(_libas, function_signature.of("create_new_assembler", cxx_type.pvoid, cxx_type.pvoid, cxx_type.bool, cxx_type.pchar));
		assembler_add_src = shared_object.dlsym(_libas, function_signature.of("assembler_add_src", cxx_type._void, cxx_type.pvoid, cxx_type.pchar));
		assemble_unit = shared_object.dlsym(_libas, function_signature.of("assemble_unit", cxx_type.pvoid, cxx_type.pvoid, cxx_type.bool, cxx_type.bool, cxx_type.unsigned_int));
		assembler_clear_unit = shared_object.dlsym(_libas, function_signature.of("assembler_clear_unit", cxx_type._void, cxx_type.pvoid));
		free_assembler = shared_object.dlsym(_libas, function_signature.of("free_assembler", cxx_type._void, cxx_type.pvoid));
		create_new_disassembler = shared_object.dlsym(_libas, function_signature.of("create_new_disassembler", cxx_type.pvoid, cxx_type.pvoid, cxx_type.unsigned_int));
		disassemble_text = shared_object.dlsym(_libas, function_signature.of("disassemble_text", cxx_type.pvoid, cxx_type.pvoid, cxx_type.pvoid, cxx_type.size_t, cxx_type.uint64_t));
		disassemble_o = shared_object.dlsym(_libas, function_signature.of("disassemble_o", cxx_type.pvoid, cxx_type.pvoid, cxx_type.pvoid, cxx_type.size_t));
		disassembler_find_return = shared_object.dlsym(_libas, function_signature.of("disassembler_find_return", cxx_type.uint64_t, cxx_type.pvoid, cxx_type.pvoid, cxx_type.size_t, cxx_type.uint64_t, cxx_type._int));
		disassembler_find_call = shared_object.dlsym(_libas, function_signature.of("disassembler_find_call", cxx_type.uint64_t, cxx_type.pvoid, cxx_type.pvoid, cxx_type.size_t, cxx_type.uint64_t, cxx_type._int));
		disassembler_find_opcode = shared_object.dlsym(_libas, function_signature.of("disassembler_find_opcode", cxx_type.uint64_t, cxx_type.pvoid, cxx_type.pvoid, cxx_type.size_t, cxx_type.uint64_t, cxx_type._int, cxx_type._int));

		free_array = shared_object.dlsym(_libas, function_signature.of("free_array", cxx_type._void, cxx_type.pvoid));

		// 全局变量
		host_architecture_context = unsafe.read_ptr(shared_object.dlsym(_libas, "host_architecture_context"));
		host_assembler = unsafe.read_ptr(shared_object.dlsym(_libas, "host_assembler"));
		host_disassembler = unsafe.read_ptr(shared_object.dlsym(_libas, "host_disassembler"));
	}

	/**
	 * 当前汇编添加源码
	 * 
	 * @param assembler
	 * @param src
	 * @return 内存中的源码指针
	 */
	public static final pointer assembler_add_src(long assembler, String src)
	{
		pointer cstr = memory.c_str(src);
		try
		{
			assembler_add_src.invokeExact(assembler, cstr.address());
			return cstr;// 必须在汇编成.o以后才能释放源码
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("call assembler_add_src() failed", ex);
		}
	}

	public static final void assembler_add_src(String src)
	{
		assembler_add_src(host_assembler, src);
	}

	/**
	 * 汇编当前的单元，编译结果为.o
	 * 
	 * @param assembler
	 * @param PIC
	 * @param LargeCodeModel
	 * @param syntax
	 * @return
	 */
	public static final long assemble_unit(long assembler, boolean PIC, boolean LargeCodeModel, int syntax)
	{
		try
		{
			return (long) assemble_unit.invokeExact(assembler, PIC, LargeCodeModel, syntax);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("call assemble_unit() failed", ex);
		}
	}

	public static final long assemble_unit(boolean PIC, boolean LargeCodeModel, int syntax)
	{
		return assemble_unit(host_assembler, PIC, LargeCodeModel, syntax);
	}

	public static final long assemble_unit()
	{
		return assemble_unit(true, false, assembly_syntax.ASM_SYNTAX_ATT);
	}

	/**
	 * 清除当前汇编单元
	 * 
	 * @param assembler
	 */
	public static final void assembler_clear_unit(long assembler)
	{
		try
		{
			assembler_clear_unit.invokeExact(assembler);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("call assembler_clear_unit() failed", ex);
		}
	}

	public static final void assembler_clear_unit()
	{
		assembler_clear_unit(host_assembler);
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
