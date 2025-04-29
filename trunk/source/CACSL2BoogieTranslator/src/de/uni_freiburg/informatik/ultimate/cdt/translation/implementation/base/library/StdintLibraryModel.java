package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.library;

import java.util.Collection;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.CPrimitives;

public class StdintLibraryModel implements ILibraryModel {

	@Override
	public Collection<TypeModel> getTypeModels() {
		// TODO: These types depend on the settings, but they should have always the specified bits
		final var int8 = new CPrimitive(CPrimitives.SCHAR);
		final var int16 = new CPrimitive(CPrimitives.SHORT);
		final var int32 = new CPrimitive(CPrimitives.INT);
		final var int64 = new CPrimitive(CPrimitives.LONGLONG);

		final var uint8 = new CPrimitive(CPrimitives.UCHAR);
		final var uint16 = new CPrimitive(CPrimitives.USHORT);
		final var uint32 = new CPrimitive(CPrimitives.UINT);
		final var uint64 = new CPrimitive(CPrimitives.ULONGLONG);

		return List.of(
				// signed integer type with width of exactly 8, 16, 32 and 64 bits respectively
				new TypeModel("int8_t", int8), new TypeModel("int16_t", int16), new TypeModel("int32_t", int32),
				new TypeModel("int64_t", int64),

				// fastest signed integer type with width of at least 8, 16, 32 and 64 bits respectively
				new TypeModel("int_fast8_t", int8), new TypeModel("int_fast16_t", int16),
				new TypeModel("int_fast32_t", int32), new TypeModel("int_fast64_t", int64),

				// smallest signed integer type with width of at least 8, 16, 32 and 64 bits respectively
				new TypeModel("int_least8_t", int8), new TypeModel("int_least16_t", int16),
				new TypeModel("int_least32_t", int32), new TypeModel("int_least64_t", int64),

				// maximum width integer type
				new TypeModel("intmax_t", int64),

				// integer type capable of holding a pointer
				new TypeModel("intptr_t", int32),

				// unsigned integer type with width of exactly 8, 16, 32 and 64 bits respectively
				new TypeModel("uint8_t", uint8), new TypeModel("uint16_t", uint16), new TypeModel("uint32_t", uint32),
				new TypeModel("uint64_t", uint64),

				// fastest unsigned integer type with width of at least 8, 16, 32 and 64 bits respectively
				new TypeModel("uint_fast8_t", uint8), new TypeModel("uint_fast16_t", uint16),
				new TypeModel("uint_fast32_t", uint32), new TypeModel("uint_fast64_t", uint64),

				// smallest unsigned integer type with width of at least 8, 16, 32 and 64 bits respectively
				new TypeModel("uint_least8_t", uint8), new TypeModel("uint_least16_t", uint16),
				new TypeModel("uint_least32_t", uint32), new TypeModel("uint_least64_t", uint64),

				// maximum width unsigned integer type
				new TypeModel("uintmax_t", uint64),

				// unsigned integer type capable of holding a pointer
				new TypeModel("uintptr_t", uint32));
	}

	@Override
	public Collection<ConstantModel> getConstantModels() {
		// TODO: Add INTN_MIN etc.
		return List.of();
	}

}
