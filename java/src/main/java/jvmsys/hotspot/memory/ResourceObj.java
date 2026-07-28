package jvmsys.hotspot.memory;

import jvmsys.hotspot.vm_struct;

public abstract class ResourceObj extends vm_struct
{
	protected ResourceObj(String name, long address)
	{
		super(name, address);
	}

	@Override
	public final int allocation_type()
	{
		return ResourceObj;
	}
}
