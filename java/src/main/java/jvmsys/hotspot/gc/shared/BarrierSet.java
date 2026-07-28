package jvmsys.hotspot.gc.shared;

import jvmsys.unsafe;
import jvmsys.hotspot.vm_struct;
import jvmsys.hotspot.memory.CHeapObj;
import jvmsys.hotspot.utilities.FakeRtti;
import jvmsys.memory.memory_object;

public class BarrierSet extends CHeapObj
{
	public static final String type_name = "BarrierSet";
	public static final long size = sizeof(type_name);

	private static final long _fake_rtti = vm_struct.entry.find(type_name, "_fake_rtti").address;

	private static final long _barrier_set = vm_struct.entry.find(type_name, "_barrier_set").address;

	protected BarrierSet(long address)
	{
		super(type_name, address);
	}

	public FakeRtti fake_rtti()
	{
		return super.read_memory_object(FakeRtti.class, _fake_rtti);
	}

	public static final BarrierSet barrier_set()
	{
		return memory_object.as_memory_object_ptr(BarrierSet.class, _barrier_set);
	}

	public static final void set_barrier_set(BarrierSet barrier_set)
	{
		unsafe.write_ptr(_barrier_set, barrier_set);
	}
}