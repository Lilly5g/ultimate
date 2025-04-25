package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.library;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.chandler.TypeSizes;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.CPrimitives;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ExpressionResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.RValue;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;

public class LimitsLibraryModel implements ILibraryModel {
	private final TypeSizes mTypeSizes;

	public LimitsLibraryModel(final TypeSizes typeSizes) {
		mTypeSizes = typeSizes;
	}

	private ExpressionResult getMinValue(final ILocation loc, final CPrimitives type) {
		final var cType = new CPrimitive(type);
		final BigInteger value = mTypeSizes.getMinValueOfPrimitiveType(cType);
		return new ExpressionResult(new RValue(mTypeSizes.constructLiteralForIntegerType(loc, cType, value), cType));
	}

	private ExpressionResult getMaxValue(final ILocation loc, final CPrimitives type) {
		final var cType = new CPrimitive(type);
		final BigInteger value = mTypeSizes.getMaxValueOfPrimitiveType(cType);
		return new ExpressionResult(new RValue(mTypeSizes.constructLiteralForIntegerType(loc, cType, value), cType));
	}

	@Override
	public Collection<ConstantModel> getConstantModels() {
		return List.of(new ConstantModel("CHAR_MIN", loc -> getMinValue(loc, CPrimitives.CHAR)),
				new ConstantModel("CHAR_MAX", loc -> getMaxValue(loc, CPrimitives.CHAR)),
				new ConstantModel("SCHAR_MIN", loc -> getMinValue(loc, CPrimitives.SCHAR)),
				new ConstantModel("SCHAR_MAX", loc -> getMaxValue(loc, CPrimitives.SCHAR)),
				new ConstantModel("SHRT_MIN", loc -> getMinValue(loc, CPrimitives.SHORT)),
				new ConstantModel("SHRT_MAX", loc -> getMaxValue(loc, CPrimitives.SHORT)),
				new ConstantModel("INT_MIN", loc -> getMinValue(loc, CPrimitives.INT)),
				new ConstantModel("INT_MAX", loc -> getMaxValue(loc, CPrimitives.INT)),
				new ConstantModel("LONG_MIN", loc -> getMinValue(loc, CPrimitives.LONG)),
				new ConstantModel("LONG_MAX", loc -> getMaxValue(loc, CPrimitives.LONG)),
				new ConstantModel("LLONG_MIN", loc -> getMinValue(loc, CPrimitives.LONGLONG)),
				new ConstantModel("LLONG_MAX", loc -> getMaxValue(loc, CPrimitives.LONGLONG)),
				new ConstantModel("UCHAR_MAX", loc -> getMaxValue(loc, CPrimitives.UCHAR)),
				new ConstantModel("USHRT_MAX", loc -> getMaxValue(loc, CPrimitives.USHORT)),
				new ConstantModel("UINT_MAX", loc -> getMaxValue(loc, CPrimitives.UINT)),
				new ConstantModel("ULONG_MAX", loc -> getMaxValue(loc, CPrimitives.ULONG)),
				new ConstantModel("ULLONG_MAX", loc -> getMaxValue(loc, CPrimitives.ULONGLONG)));
	}

}
