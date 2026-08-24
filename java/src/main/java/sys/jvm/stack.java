package sys.jvm;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.lang.annotation.ElementType;
import java.lang.annotation.Native;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import sys.jvm.thread.virtual_thread;

/**
 * 栈帧回溯。
 * 由于OpenJDK的实现的Java层与C++层、Java的StackStreamFactory中数个内部类严重地紧耦合，几乎无可替换实现的逻辑。
 * 因此，此类的主要目的是提高拓展性而非性能。
 */
public abstract class stack
{
	// StackStreamFactory定义的遍历模式常量
	public abstract static class unwind_mode
	{
		@Native
		public static final int DEFAULT_MODE = 0x0;
		@Native
		public static final int FILL_CLASS_REFS_ONLY = 0x2;
		@Native
		public static final int GET_CALLER_CLASS = 0x4;
		@Native
		public static final int SHOW_HIDDEN_FRAMES = 0x20;
		@Native
		public static final int FILL_LIVE_STACK_FRAMES = 0x100;
	}

	// 类及字段、函数

	private static Class<?> java_lang_StackStreamFactory_AbstractStackWalker;
	private static Class<?> java_lang_StackStreamFactory_StackFrameTraverser;
	private static Class<?> java_lang_StackStreamFactory_FrameBuffer;

	private static long StackWalker_options_offset;
	private static long StackWalker_estimateDepth_offset;
	private static long StackWalker_extendedOption_offset;
	private static long StackWalker_retainClassRef_offset;
	private static long StackWalker_contScope_offset;
	private static long StackWalker_continuation_offset;

	private static MethodHandle AbstractStackWalker_callStackWalk;
	private static MethodHandle AbstractStackWalker_fetchStackFrames;
	private static MethodHandle AbstractStackWalker_initFrameBuffer;
	private static MethodHandle AbstractStackWalker_walk;

	private static long AbstractStackWalker_mode_offset;
	private static long AbstractStackWalker_frameBuffer_offset;
	private static long AbstractStackWalker_anchor_offset;
	private static long AbstractStackWalker_contScope_offset;
	private static long AbstractStackWalker_continuation_offset;
	private static long AbstractStackWalker_thread_offset;
	private static long AbstractStackWalker_walker_offset;
	private static long AbstractStackWalker_maxDepth_offset;
	private static long AbstractStackWalker_depth_offset;

	private static MethodHandle FrameBuffer_curBatchFrameCount;
	private static MethodHandle FrameBuffer_startIndex;
	private static MethodHandle FrameBuffer_frames;

	private static long StackFrameTraverser_function_offset;

	static
	{
		try
		{
			java_lang_StackStreamFactory_AbstractStackWalker = Class.forName("java.lang.StackStreamFactory$AbstractStackWalker");
			// AbstractStackWalker的Java实现与C++实现过度紧耦合，无法拆分，必须依靠StackFrameTraverser进行栈回溯。
			java_lang_StackStreamFactory_StackFrameTraverser = Class.forName("java.lang.StackStreamFactory$StackFrameTraverser");
			java_lang_StackStreamFactory_FrameBuffer = Class.forName("java.lang.StackStreamFactory$FrameBuffer");
		}
		catch (ClassNotFoundException ex)
		{
			ex.printStackTrace();
		}
		StackWalker_options_offset = unsafe.object_field_offset(StackWalker.class, "options");
		StackWalker_estimateDepth_offset = unsafe.object_field_offset(StackWalker.class, "estimateDepth");
		StackWalker_extendedOption_offset = unsafe.object_field_offset(StackWalker.class, "extendedOption");
		StackWalker_retainClassRef_offset = unsafe.object_field_offset(StackWalker.class, "retainClassRef");
		StackWalker_contScope_offset = unsafe.object_field_offset(StackWalker.class, "contScope");
		StackWalker_continuation_offset = unsafe.object_field_offset(StackWalker.class, "continuation");

		AbstractStackWalker_callStackWalk = symbols.find_special_method(java_lang_StackStreamFactory_AbstractStackWalker, "callStackWalk", Object.class, long.class, int.class, thread.jdk_internal_vm_ContinuationScope, thread.jdk_internal_vm_Continuation, int.class, int.class, Object[].class);
		AbstractStackWalker_fetchStackFrames = symbols.find_special_method(java_lang_StackStreamFactory_AbstractStackWalker, "fetchStackFrames", int.class, long.class, long.class, int.class, int.class, Object[].class);
		AbstractStackWalker_initFrameBuffer = symbols.find_virtual_method(java_lang_StackStreamFactory_AbstractStackWalker, "initFrameBuffer", void.class);
		AbstractStackWalker_walk = symbols.find_special_method(java_lang_StackStreamFactory_AbstractStackWalker, "walk", Object.class);

		AbstractStackWalker_mode_offset = unsafe.object_field_offset(java_lang_StackStreamFactory_AbstractStackWalker, "mode");
		AbstractStackWalker_frameBuffer_offset = unsafe.object_field_offset(java_lang_StackStreamFactory_AbstractStackWalker, "frameBuffer");
		AbstractStackWalker_anchor_offset = unsafe.object_field_offset(java_lang_StackStreamFactory_AbstractStackWalker, "anchor");
		AbstractStackWalker_contScope_offset = unsafe.object_field_offset(java_lang_StackStreamFactory_AbstractStackWalker, "contScope");
		AbstractStackWalker_continuation_offset = unsafe.object_field_offset(java_lang_StackStreamFactory_AbstractStackWalker, "continuation");
		AbstractStackWalker_thread_offset = unsafe.object_field_offset(java_lang_StackStreamFactory_AbstractStackWalker, "thread");
		AbstractStackWalker_walker_offset = unsafe.object_field_offset(java_lang_StackStreamFactory_AbstractStackWalker, "walker");
		AbstractStackWalker_maxDepth_offset = unsafe.object_field_offset(java_lang_StackStreamFactory_AbstractStackWalker, "maxDepth");
		AbstractStackWalker_depth_offset = unsafe.object_field_offset(java_lang_StackStreamFactory_AbstractStackWalker, "depth");

		FrameBuffer_curBatchFrameCount = symbols.find_special_method(java_lang_StackStreamFactory_FrameBuffer, "curBatchFrameCount", int.class);
		FrameBuffer_startIndex = symbols.find_virtual_method(java_lang_StackStreamFactory_FrameBuffer, "startIndex", int.class);
		FrameBuffer_frames = symbols.find_virtual_method(java_lang_StackStreamFactory_FrameBuffer, "frames", Object[].class);

		StackFrameTraverser_function_offset = unsafe.object_field_offset(java_lang_StackStreamFactory_StackFrameTraverser, "function");
	}

	// StackWalker

	/**
	 * 获取StackWalker的options字段值
	 * 
	 * @param stack_walker
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static final Set<StackWalker.Option> stack_walker_options(StackWalker stack_walker)
	{
		return (Set<Option>) unsafe.read_reference(stack_walker, StackWalker_options_offset);
	}

	/**
	 * 设置StackWalker的options字段值
	 * 
	 * @param stack_walker
	 * @param options
	 */
	public static final void set_stack_walker_options(StackWalker stack_walker, EnumSet<StackWalker.Option> options)
	{
		unsafe.write(stack_walker, StackWalker_options_offset, options);
	}

	/**
	 * 获取StackWalker的estimateDepth字段值
	 * 
	 * @param stack_walker
	 * @return
	 */
	public static final int stack_walker_estimate_depth(StackWalker stack_walker)
	{
		return unsafe.read_int(stack_walker, StackWalker_estimateDepth_offset);
	}

	/**
	 * 设置StackWalker的estimateDepth字段值
	 * 
	 * @param stack_walker
	 * @param estimate_depth
	 */
	public static final void set_stack_walker_estimate_depth(StackWalker stack_walker, int estimate_depth)
	{
		unsafe.write(stack_walker, StackWalker_estimateDepth_offset, estimate_depth);
	}

	/**
	 * 获取StackWalker的extendedOption字段值
	 * 
	 * @param stack_walker
	 * @return
	 */
	public static final Object stack_walker_extended_option(StackWalker stack_walker)
	{
		return unsafe.read_reference(stack_walker, StackWalker_extendedOption_offset);
	}

	/**
	 * 设置StackWalker的extendedOption字段值
	 * 该值用于makeStackTraverser()中的hasLocalsOperandsOption()调用，对于直接生成的AbstractStackWalker，此值不参与回溯逻辑。
	 * 
	 * @param stack_walker
	 * @param extended_option
	 */
	public static final void set_stack_walker_extended_option(StackWalker stack_walker, Object extended_option)
	{
		unsafe.write(stack_walker, StackWalker_extendedOption_offset, extended_option);
	}

	/**
	 * 获取StackWalker的retainClassRef字段值
	 * 
	 * @param stack_walker
	 * @return
	 */
	public static final boolean stack_walker_retain_class_ref(StackWalker stack_walker)
	{
		return unsafe.read_bool(stack_walker, StackWalker_retainClassRef_offset);
	}

	/**
	 * 设置StackWalker的retainClassRef字段值
	 * 
	 * @param stack_walker
	 * @param retain_class_ref
	 */
	public static final void set_stack_walker_retain_class_ref(StackWalker stack_walker, boolean retain_class_ref)
	{
		unsafe.write(stack_walker, StackWalker_retainClassRef_offset, retain_class_ref);
	}

	/**
	 * 获取StackWalker的contScope字段值
	 * 
	 * @param stack_walker
	 * @return
	 */
	public static final Object stack_walker_cont_scope(StackWalker stack_walker)
	{
		return unsafe.read_reference(stack_walker, StackWalker_contScope_offset);
	}

	/**
	 * 设置StackWalker的contScope字段值
	 * 
	 * @param stack_walker
	 * @param cont_scope
	 */
	public static final void set_stack_walker_cont_scope(StackWalker stack_walker, Object cont_scope)
	{
		unsafe.write(stack_walker, StackWalker_contScope_offset, cont_scope);
	}

	/**
	 * 获取StackWalker的continuation字段值
	 * 
	 * @param stack_walker
	 * @return
	 */
	public static final Object stack_walker_continuation(StackWalker stack_walker)
	{
		return unsafe.read_reference(stack_walker, StackWalker_continuation_offset);
	}

	/**
	 * 设置StackWalker的continuation字段值
	 * 
	 * @param stack_walker
	 * @param continuation
	 */
	public static final void set_stack_walker_continuation(StackWalker stack_walker, Object continuation)
	{
		unsafe.write(stack_walker, StackWalker_continuation_offset, continuation);
	}

	/**
	 * 构造一个StackWalker，仅用于作为doStackWalk()回调操作对象。
	 * 
	 * @param options
	 * @param estimate_depth  预估栈深度，用于决定缓冲区分配大小
	 * @param extended_option 是否包含局部变量信息，决定了makeStackTraverser()构造的AbstractStackWalker对象类型
	 * @param cont_scope
	 * @param continuation
	 * @return
	 */
	public static final StackWalker new_stack_walker(EnumSet<StackWalker.Option> options, int estimate_depth, Object extended_option, Object cont_scope, Object continuation)
	{
		// 初始化操作见AbstractStackWalker构造函数
		StackWalker stack_walker = unsafe.allocate(StackWalker.class);
		set_stack_walker_options(stack_walker, options);
		set_stack_walker_retain_class_ref(stack_walker, options.contains(Option.RETAIN_CLASS_REFERENCE));
		set_stack_walker_estimate_depth(stack_walker, estimate_depth);
		set_stack_walker_extended_option(stack_walker, extended_option);
		set_stack_walker_cont_scope(stack_walker, cont_scope);
		set_stack_walker_continuation(stack_walker, continuation);
		return stack_walker;
	}

	public static final StackWalker new_stack_walker(int estimate_depth, EnumSet<StackWalker.Option> options)
	{
		return new_stack_walker(options, estimate_depth, null, null, null);
	}

	public static final StackWalker new_stack_walker(EnumSet<StackWalker.Option> options)
	{
		return new_stack_walker(options, 0, null, null, null);
	}

	// AbstractStackWalker

	/**
	 * 获取遍历模式
	 * 
	 * @param abstract_stack_walker
	 * @param mode
	 */
	public static final long abstract_stack_walker_mode(Object abstract_stack_walker)
	{
		return unsafe.read_long(abstract_stack_walker, AbstractStackWalker_mode_offset);
	}

	/**
	 * 设置遍历模式
	 * 
	 * @param abstract_stack_walker
	 * @param mode
	 */
	public static final void set_abstract_stack_walker_mode(Object abstract_stack_walker, long mode)
	{
		unsafe.write(abstract_stack_walker, AbstractStackWalker_mode_offset, mode);
	}

	/**
	 * 获取AbstractStackWalker的thread字段值
	 * 
	 * @param abstract_stack_walker
	 * @return
	 */
	public static final Thread abstract_stack_walker_thread(Object abstract_stack_walker)
	{
		return (Thread) unsafe.read_reference(abstract_stack_walker, AbstractStackWalker_thread_offset);
	}

	/**
	 * 设置AbstractStackWalker的thread字段值
	 * 
	 * @param abstract_stack_walker
	 * @param thread
	 */
	public static final void set_abstract_stack_walker_thread(Object abstract_stack_walker, Thread thread)
	{
		unsafe.write(abstract_stack_walker, AbstractStackWalker_thread_offset, thread);
	}

	/**
	 * 获取AbstractStackWalker的walker字段值
	 * 
	 * @param abstract_stack_walker
	 * @return
	 */
	public static final StackWalker abstract_stack_walker_walker(Object abstract_stack_walker)
	{
		return (StackWalker) unsafe.read_reference(abstract_stack_walker, AbstractStackWalker_walker_offset);
	}

	/**
	 * 设置AbstractStackWalker的walker字段值
	 * 
	 * @param abstract_stack_walker
	 * @param walker
	 */
	public static final void set_abstract_stack_walker_walker(Object abstract_stack_walker, StackWalker stack_walker)
	{
		unsafe.write(abstract_stack_walker, AbstractStackWalker_walker_offset, stack_walker);
	}

	/**
	 * 获取AbstractStackWalker的maxDepth字段值
	 * 
	 * @param abstract_stack_walker
	 * @return
	 */
	public static final int abstract_stack_walker_max_depth(Object abstract_stack_walker)
	{
		return unsafe.read_int(abstract_stack_walker, AbstractStackWalker_maxDepth_offset);
	}

	/**
	 * 设置AbstractStackWalker的maxDepth字段值
	 * 
	 * @param abstract_stack_walker
	 * @param maxDepth
	 */
	public static final void set_abstract_stack_walker_max_depth(Object abstract_stack_walker, int maxDepth)
	{
		unsafe.write(abstract_stack_walker, AbstractStackWalker_maxDepth_offset, maxDepth);
	}

	/**
	 * 获取AbstractStackWalker的depth字段值
	 * 
	 * @param abstract_stack_walker
	 * @return
	 */
	public static final int abstract_stack_walker_depth(Object abstract_stack_walker)
	{
		return unsafe.read_int(abstract_stack_walker, AbstractStackWalker_depth_offset);
	}

	/**
	 * 设置AbstractStackWalker的depth字段值
	 * 
	 * @param abstract_stack_walker
	 * @param depth
	 */
	public static final void set_abstract_stack_walker_depth(Object abstract_stack_walker, int depth)
	{
		unsafe.write(abstract_stack_walker, AbstractStackWalker_depth_offset, depth);
	}

	public static final Object abstract_stack_walker_frame_buffer(Object abstract_stack_walker)
	{
		return unsafe.read_reference(abstract_stack_walker, AbstractStackWalker_frameBuffer_offset);
	}

	/**
	 * 设置AbstractStackWalker的frameBuffer字段值
	 * 
	 * @param abstract_stack_walker
	 * @param frame_buffer
	 */
	public static final void set_abstract_stack_walker_frame_buffer(Object abstract_stack_walker, Object frame_buffer)
	{
		unsafe.write(abstract_stack_walker, AbstractStackWalker_frameBuffer_offset, frame_buffer);
	}

	public static final long abstract_stack_walker_anchor(Object abstract_stack_walker)
	{
		return unsafe.read_long(abstract_stack_walker, AbstractStackWalker_anchor_offset);
	}

	/**
	 * 设置AbstractStackWalker的anchor字段值
	 * 
	 * @param abstract_stack_walker
	 * @param anchor
	 */
	public static final void set_abstract_stack_walker_anchor(Object abstract_stack_walker, long anchor)
	{
		unsafe.write(abstract_stack_walker, AbstractStackWalker_anchor_offset, anchor);
	}

	public static final long abstract_stack_walker_cont_scope(Object abstract_stack_walker)
	{
		return unsafe.read_long(abstract_stack_walker, AbstractStackWalker_contScope_offset);
	}

	/**
	 * 设置AbstractStackWalker的contScope字段值
	 * 
	 * @param abstract_stack_walker
	 * @param contScope
	 */
	public static final void set_abstract_stack_walker_cont_scope(Object abstract_stack_walker, Object cont_scope)
	{
		unsafe.write(abstract_stack_walker, AbstractStackWalker_contScope_offset, cont_scope);
	}

	public static final long abstract_stack_walker_continuation(Object abstract_stack_walker)
	{
		return unsafe.read_long(abstract_stack_walker, AbstractStackWalker_continuation_offset);
	}

	/**
	 * 设置AbstractStackWalker的continuation字段值
	 * 
	 * @param abstract_stack_walker
	 * @param continuation
	 */
	public static final void set_abstract_stack_walker_continuation(Object abstract_stack_walker, Object continuation)
	{
		unsafe.write(abstract_stack_walker, AbstractStackWalker_continuation_offset, continuation);
	}

	/**
	 * 从指定StackWalker创建一个AbstractStackWalker派生类对象用于遍历。
	 * 在JDK中，AbstractStackWalker为遍历使用的一次性遍历器，而StackWalker储存了长期的遍历设置，每次遍历都会创建一个新的AbstractStackWalker对象。
	 * 
	 * @param stack_walker_clazz 必须是AbstractStackWalker的派生类
	 * @param walker
	 * @param thread
	 * @param mode
	 * @param max_depth
	 * @return
	 */
	public static final Object new_abstract_stack_walker(Class<?> stack_walker_clazz, StackWalker stack_walker, Thread thread, int mode, int max_depth)
	{
		// 初始化操作见AbstractStackWalker构造函数
		Object abstract_stack_walker = unsafe.allocate(stack_walker_clazz);
		set_abstract_stack_walker_thread(abstract_stack_walker, thread);
		set_abstract_stack_walker_mode(abstract_stack_walker, mode); // 直接使用传入的mode值，忽视StackWalker的mode属性
		set_abstract_stack_walker_walker(abstract_stack_walker, stack_walker);
		set_abstract_stack_walker_max_depth(abstract_stack_walker, max_depth);
		set_abstract_stack_walker_depth(abstract_stack_walker, 0);
		Object scope = stack_walker_cont_scope(stack_walker);
		if (scope == null && thread.isVirtual())
		{
			set_abstract_stack_walker_cont_scope(abstract_stack_walker, virtual_thread.continuation_scope());
			set_abstract_stack_walker_continuation(abstract_stack_walker, null);
		}
		else
		{
			set_abstract_stack_walker_cont_scope(abstract_stack_walker, scope);
			set_abstract_stack_walker_continuation(abstract_stack_walker, stack_walker_continuation(stack_walker));
		}
		return abstract_stack_walker;
	}

	@SuppressWarnings("unchecked")
	public static final Function<? super Stream<StackFrame>, ?> stack_frame_traverser_function(Object stack_frame_traverser)
	{
		return (Function<? super Stream<StackFrame>, ?>) unsafe.read_reference(stack_frame_traverser, StackFrameTraverser_function_offset);
	}

	/**
	 * 设置StackFrameTraverser的function字段值
	 * 
	 * @param stack_frame_traverser
	 * @param function
	 */
	public static final void set_stack_frame_traverser_function(Object stack_frame_traverser, Function<? super Stream<StackFrame>, ?> function)
	{
		unsafe.write(stack_frame_traverser, StackFrameTraverser_function_offset, function);
	}

	/**
	 * 创建单次栈回溯的缓冲对象。
	 * StackWalker仅在doStackWalk()回调中操作，不影响回溯操作。
	 * 
	 * @param walker
	 * @param thread
	 * @param mode
	 * @param max_depth
	 * @param function
	 * @return
	 */
	public static final Object new_stack_frame_traverser(StackWalker stack_walker, Thread thread, int mode, int max_depth, Function<? super Stream<StackFrame>, ?> function)
	{
		Object stack_frame_traverser = new_abstract_stack_walker(java_lang_StackStreamFactory_StackFrameTraverser, stack_walker, thread, mode, max_depth);
		set_stack_frame_traverser_function(stack_frame_traverser, function);
		return stack_frame_traverser;
	}

	/**
	 * AbstractStackWalker分配接收缓冲区
	 * 
	 * @param abstract_stack_walker
	 */
	public static final void abstract_stack_walker_init_frame_buffer(Object abstract_stack_walker)
	{
		try
		{
			AbstractStackWalker_initFrameBuffer.invoke(abstract_stack_walker);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("init frame buffer failed", ex);
		}
	}

	/**
	 * 执行栈回溯并将结果储存在AbstractStackWalker缓冲对象中。<br>
	 * 内部使用callStackWalk()获取第一批次栈帧，并根据需要使用fetchStackFrames()获取后续批次栈帧。<br>
	 * 由于OpenJDK的遍历逻辑已经足够简洁，无必要手动裁剪代码优化，直接调用原函数。<br>
	 * 
	 * @param abstract_stack_walker 单次栈帧遍历的缓冲对象
	 * @return
	 */
	@skip_unwind
	public static final Object abstract_stack_walker_walk(Object abstract_stack_walker)
	{
		try
		{
			return AbstractStackWalker_walk.invoke(abstract_stack_walker);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("walk abstract stack walker failed", ex);
		}
	}

	/**
	 * 初次栈回溯。
	 * 
	 * @param <_T>
	 * @param mode         遍历模式
	 * @param skipframes   起始要跳过的数量
	 * @param cont_scope
	 * @param continuation
	 * @param batch_size   填充大小
	 * @param start_index  要填充栈帧的frames起始索引
	 * @param frames
	 * @return 实际填充栈帧完成后的frames索引
	 */
	@Deprecated
	@skip_unwind
	public static final <_T> Object unwind(Object abstract_stack_walker, long mode, long skipframes, Object cont_scope, Object continuation, int batch_size, int start_index, _T[] frames)
	{
		try
		{
			// https://github.com/openjdk/jdk/blob/jdk-21%2B35/src/hotspot/share/prims/stackwalk.cpp#L456 调用了fetchFirstBatch()，它内部用到了abstract_stack_walker参数
			// 内部调用了AbstractStackWalker.doStackWalk()
			return AbstractStackWalker_callStackWalk.invoke(abstract_stack_walker, mode, skipframes, cont_scope, continuation, batch_size, start_index, frames);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("begin fetch stack frames failed", ex);
		}
	}

	public static final int frame_buffer_batch_count(Object frame_buffer)
	{
		try
		{
			return (int) FrameBuffer_curBatchFrameCount.invoke(frame_buffer);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("get frame buffer batch count failed", ex);
		}
	}

	public static final int frame_buffer_start_index(Object frame_buffer)
	{
		try
		{
			return (int) FrameBuffer_startIndex.invoke(frame_buffer);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("get frame buffer start index failed", ex);
		}
	}

	public static final Object[] frame_buffer_frames(Object frame_buffer)
	{
		try
		{
			return (Object[]) FrameBuffer_frames.invoke(frame_buffer);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("get frame buffer frames failed", ex);
		}
	}

	@Deprecated
	@skip_unwind
	public static final Object unwind(Object abstract_stack_walker, long skipframes)
	{
		abstract_stack_walker_init_frame_buffer(abstract_stack_walker);
		Object frame_buffer = abstract_stack_walker_frame_buffer(abstract_stack_walker);
		return unwind(abstract_stack_walker, abstract_stack_walker_mode(abstract_stack_walker), skipframes,
				abstract_stack_walker_cont_scope(abstract_stack_walker), abstract_stack_walker_continuation(abstract_stack_walker),
				frame_buffer_batch_count(frame_buffer),
				frame_buffer_start_index(frame_buffer),
				frame_buffer_frames(frame_buffer));
	}

	/**
	 * 初次栈回溯后如还需继续回溯，则调用此函数
	 * 
	 * @param <_T>
	 * @param mode
	 * @param anchor
	 * @param batchSize
	 * @param start_index
	 * @param frames
	 * @return
	 */
	@Deprecated
	@skip_unwind
	public static final <_T> int unwind(Object abstract_stack_walker, long mode, long anchor, int batchSize, int start_index, _T[] frames)
	{
		try
		{
			// https://github.com/openjdk/jdk/blob/jdk-21%2B35/src/java.base/share/native/libjava/StackStreamFactory.c#L75
			// 具体实现 https://github.com/openjdk/jdk/blob/jdk-21%2B35/src/hotspot/share/prims/stackwalk.cpp#L550
			return (int) AbstractStackWalker_fetchStackFrames.invoke(abstract_stack_walker, mode, anchor, batchSize, start_index, frames);
		}
		catch (Throwable ex)
		{
			throw new java.lang.InternalError("fetch stack frames failed", ex);
		}
	}

	@Deprecated
	@skip_unwind
	public static final Object unwind(Object abstract_stack_walker)
	{
		Object frame_buffer = abstract_stack_walker_frame_buffer(abstract_stack_walker);
		return unwind(abstract_stack_walker, abstract_stack_walker_mode(abstract_stack_walker),
				abstract_stack_walker_anchor(abstract_stack_walker),
				frame_buffer_batch_count(frame_buffer),
				frame_buffer_start_index(frame_buffer),
				frame_buffer_frames(frame_buffer));
	}

	@skip_unwind
	public static final Object unwind(StackWalker stack_walker, Thread thread, int mode, int max_depth, Function<? super Stream<StackFrame>, ?> function)
	{
		return abstract_stack_walker_walk(new_stack_frame_traverser(stack_walker, thread, mode, max_depth, function));
	}

	@skip_unwind
	public static final Object unwind(StackWalker stack_walker, Thread thread, int mode, Function<? super Stream<StackFrame>, ?> function)
	{
		return abstract_stack_walker_walk(new_stack_frame_traverser(stack_walker, thread, mode, Integer.MAX_VALUE, function));
	}

	/**
	 * 栈回溯跳过此函数的注解
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(
	{ ElementType.METHOD, ElementType.CONSTRUCTOR })
	public @interface skip_unwind
	{
	}

	/**
	 * 判断是否有@skip_unwind注解
	 * 
	 * @param frame
	 * @return
	 */
	public static final boolean is_unwind_skippable(StackWalker.StackFrame frame)
	{
		try
		{
			return reflection.find_declared_method(frame.getDeclaringClass(), frame.getMethodName(), frame.getMethodType().parameterArray())
					.isAnnotationPresent(skip_unwind.class);
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	public static final boolean is_unwind_skippable(Method m)
	{
		return m.isAnnotationPresent(skip_unwind.class);
	}

	public static final boolean is_unwind_skippable(Constructor<?> c)
	{
		return c.isAnnotationPresent(skip_unwind.class);
	}

	/**
	 * 栈回溯<br>
	 * 会跳过带有@skip_unwind注解的方法。<br>
	 * 
	 * @param stack_walker     配置对象，该对象是线程安全的
	 * @param thread           要回溯的线程
	 * @param mode             回溯模式
	 * @param max_depth        回溯最大深度，通常取最大值即可
	 * @param skip_frame_count 跳过的栈帧数量
	 * @return 得到的结果栈帧
	 * @since Java 9
	 */
	@skip_unwind
	public static final StackWalker.StackFrame unwind(StackWalker stack_walker, Thread thread, int mode, int max_depth, int skip_frame_count)
	{
		return (StackFrame) unwind(stack_walker, thread, mode, max_depth, stack ->
		{
			int skipped_frame_count = 0;
			// 跳过带有@skip_unwind注解的方法
			Iterator<StackWalker.StackFrame> iterator = stack.skip(1).iterator();// 跳过当前方法
			while (iterator.hasNext())
			{
				StackWalker.StackFrame frame = iterator.next();
				if (is_unwind_skippable(frame))
				{
					continue;
				}
				if (skipped_frame_count == skip_frame_count)
				{
					return frame;
				}
				++skipped_frame_count;
			}
			return null;
		});
	}

	@skip_unwind
	public static final StackWalker.StackFrame unwind(StackWalker stack_walker, Thread thread, int mode, int skip_frame_count)
	{
		return unwind(stack_walker, thread, mode, Integer.MAX_VALUE, skip_frame_count);
	}

	private static final StackWalker __stack_walker = new_stack_walker(EnumSet.of(StackWalker.Option.RETAIN_CLASS_REFERENCE));

	@skip_unwind
	public static final Object unwind(Thread thread, int mode, Function<? super Stream<StackFrame>, ?> function)
	{
		return unwind(__stack_walker, thread, mode, function);
	}

	@skip_unwind
	public static final StackWalker.StackFrame unwind(Thread thread, int mode, int skip_frame_count)
	{
		return unwind(__stack_walker, thread, mode, Integer.MAX_VALUE, skip_frame_count);
	}

	@skip_unwind
	public static final StackWalker.StackFrame unwind(int skip_frame_count)
	{
		return unwind(Thread.currentThread(), unwind_mode.DEFAULT_MODE, skip_frame_count);
	}

	@skip_unwind
	public static final Class<?> unwind_class(int skip_frame_count)
	{
		return unwind(skip_frame_count).getDeclaringClass();
	}

	/**
	 * 获取直接调用此函数的函数所属类
	 * 
	 * @return
	 */
	@skip_unwind
	public static final Class<?> get_caller_class()
	{
		return unwind_class(0);
	}
}