package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.library;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.CPrimitives;

public class StdboolLibraryModel implements ILibraryModel {
	private static final CPrimitive BOOL_TYPE = new CPrimitive(CPrimitives.BOOL);

	private final FunctionModelHelper mHelper;

	public StdboolLibraryModel(final FunctionModelHelper helper) {
		mHelper = helper;
	}

	@Override
	public Collection<TypeModel> getTypeModels() {
		return List.of(new TypeModel("bool", BOOL_TYPE));
	}

	@Override
	public Collection<ConstantModel> getConstantModels() {
		return List.of(
				new ConstantModel("false", loc -> mHelper.constructIntegerLiteral(loc, BigInteger.ZERO, BOOL_TYPE)),
				new ConstantModel("true", loc -> mHelper.constructIntegerLiteral(loc, BigInteger.ONE, BOOL_TYPE)));
	}
}
