package sys.jvm;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;

import as.libas;
import ld.libld;
import sys.jvm.abi.call_convention;
import sys.jvm.type.cxx_type.function_signature;
import sys.jvm.type.cxx_type.pointer;

/**
 * 汇编操作
 */
public class asm
{
	/**
	 * 汇编成的.o
	 */
	public static class o
	{
		private long o;

		o(long o)
		{
			this.o = o;
		}

		public final void free()
		{
			libas.free_array(o);
			this.o = 0;
		}
	}

	public static class assembler
	{
		private final ArrayList<pointer> src_cache = new ArrayList<>();

		private long assembler;

		private assembler(long assembler)
		{
			this.assembler = assembler;
		}

		public final void add_src(String src)
		{
			src_cache.add(libas.assembler_add_src(assembler, src));
		}

		public final o assemble(boolean PIC, boolean LargeCodeModel, int syntax)
		{
			return new o(libas.assemble_unit(assembler, PIC, LargeCodeModel, syntax));
		}

		public o assemble()
		{
			return assemble(true, false, libas.assembly_syntax.ASM_SYNTAX_ATT);
		}

		public final void new_unit()
		{
			for (pointer src : src_cache)
			{
				src.delete();// 释放源码缓存
			}
			src_cache.clear();
			libas.assembler_clear_unit(assembler);
		}
	}

	public static final assembler host_assembler = new assembler(libas.host_assembler);

	public static class dynamic_lib_target
	{
		private long target;
		private String lib_name;

		dynamic_lib_target(long target, String lib_name)
		{
			this.target = target;
			this.lib_name = lib_name;
		}

		public final String lib_name()
		{
			return lib_name;
		}

		public final void free()
		{
			libld.free_dynamic_lib(target);
			this.target = 0;
		}

		public final void add_o(o o)
		{
			libld.dynamic_link_o(target, o.o);
		}

		public final long lookup(String sym_name)
		{
			return libld.dynamic_symbol_lookup(target, sym_name);
		}

		public final MethodHandle lookup(call_convention call_conv, function_signature signature)
		{
			return libld.dynamic_symbol_lookup(target, call_conv, signature);
		}

		public final MethodHandle lookup(function_signature signature)
		{
			return lookup(call_convention.host, signature);
		}
	}

	public static class dynamic_linker
	{
		private long linker;

		private dynamic_linker(long linker)
		{
			this.linker = linker;
		}

		public final dynamic_lib_target link_target(String lib_name)
		{
			return new dynamic_lib_target(libld.dynamic_link_target(linker, lib_name), lib_name);
		}
	}

	public static final dynamic_linker global_dynamic_linker = new dynamic_linker(libld.global_dynamic_linker);

	private static final dynamic_lib_target jit_lib = global_dynamic_linker.link_target("jit_lib");

	public static final void jit_link(o o)
	{
		jit_lib.add_o(o);
	}

	public static final long jit_lookup(String sym_name)
	{
		return jit_lib.lookup(sym_name);
	}

	public static final MethodHandle jit_lookup(call_convention call_conv, function_signature signature)
	{
		return jit_lib.lookup(call_conv, signature);
	}

	public static final MethodHandle jit_lookup(function_signature signature)
	{
		return jit_lib.lookup(signature);
	}
}
