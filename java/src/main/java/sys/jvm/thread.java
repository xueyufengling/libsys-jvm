package sys.jvm;

import java.lang.invoke.MethodHandle;

public abstract class thread
{
	static Class<?> jdk_internal_vm_ContinuationScope;
	static Class<?> jdk_internal_vm_Continuation;

	static
	{
		try
		{
			jdk_internal_vm_ContinuationScope = Class.forName("jdk.internal.vm.ContinuationScope");
			jdk_internal_vm_Continuation = Class.forName("jdk.internal.vm.Continuation");
		}
		catch (ClassNotFoundException ex)
		{
			ex.printStackTrace();
		}
	}

	public static abstract class virtual_thread
	{
		static Class<?> java_lang_VirtualThread;

		private static MethodHandle VirtualThread_continuationScope;

		static
		{
			try
			{
				java_lang_VirtualThread = Class.forName("java.lang.VirtualThread");
			}
			catch (ClassNotFoundException ex)
			{
				ex.printStackTrace();
			}
			VirtualThread_continuationScope = symbols.find_static_method(java_lang_VirtualThread, "continuationScope", jdk_internal_vm_ContinuationScope);
		}

		/**
		 * 获取虚拟线程的ContinuationScope
		 * 
		 * @return
		 */
		public static final Object continuation_scope()
		{
			try
			{
				return VirtualThread_continuationScope.invoke();
			}
			catch (Throwable ex)
			{
				throw new java.lang.InternalError("get virtual thread continuation scope failed", ex);
			}
		}
	}

}
