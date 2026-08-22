package sys.jvm.hotspot.oops;

import sys.jvm.unsafe;
import sys.jvm.virtual_machine;
import sys.jvm.hotspot.utilities.align;
import sys.jvm.hotspot.utilities.globalDefinitions;
import sys.jvm.hotspot.utilities.globalDefinitions.BasicType;
import sys.jvm.type.cxx_type;

public class arrayOopDesc extends oopDesc
{
	public static final String type_name = "arrayOopDesc";
	public static final long size = sizeof(type_name);

	public arrayOopDesc(long address)
	{
		super(type_name, address);
	}

	public static int length_offset_in_bytes()
	{
		return oopDesc.base_offset_in_bytes();
	}

	public static int header_size_in_bytes()
	{
		return (int) (length_offset_in_bytes() + cxx_type._int.size());
	}

	public static boolean element_type_should_be_aligned(byte type)
	{
		if (unsafe.lp64)
		{
			if (type == BasicType.T_OBJECT || type == BasicType.T_ARRAY)
			{
				return !virtual_machine.UseCompressedOops;
			}
		}
		return type == BasicType.T_DOUBLE || type == BasicType.T_LONG;
	}

	// Returns the offset of the first element.
	public static int base_offset_in_bytes(byte type)
	{
		int hs = header_size_in_bytes();
		return (int) (element_type_should_be_aligned(type) ? align.align_up(hs, globalDefinitions.BytesPerLong) : hs);
	}
}
