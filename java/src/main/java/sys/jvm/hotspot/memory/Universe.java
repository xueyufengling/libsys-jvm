package sys.jvm.hotspot.memory;

import sys.jvm.unsafe;
import sys.jvm.hotspot.vm_struct;
import sys.jvm.hotspot.gc.shared.CollectedHeap;
import sys.jvm.memory.memory_object;

/**
 * JVM的堆
 */
public abstract class Universe
{
	private static final long _collectedHeap = vm_struct.entry.find("Universe", "_collectedHeap").address;// 静态，堆的基地址

	public static final long _collectedHeap()
	{
		return unsafe.read_ptr(_collectedHeap);
	}

	public static final CollectedHeap heap()
	{
		return memory_object.as_memory_object(CollectedHeap.class, _collectedHeap);
	}
}
