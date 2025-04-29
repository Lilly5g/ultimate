package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.library;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.expressiontranslation.ExpressionTranslation;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.CPrimitives;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ExpressionResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.RValue;

public class StdboolLibraryModel implements ILibraryModel {
	private static final CPrimitive BOOL_TYPE = new CPrimitive(CPrimitives.BOOL);

	private final ExpressionTranslation mExpressionTranslation;

	public StdboolLibraryModel(final ExpressionTranslation expressionTranslation) {
		mExpressionTranslation = expressionTranslation;
	}

	@Override
	public Collection<TypeModel> getTypeModels() {
		return List.of(new TypeModel("bool", BOOL_TYPE));
	}

	@Override
	public Collection<ConstantModel> getConstantModels() {
		return List.of(
				new ConstantModel("false",
						loc -> new ExpressionResult(new RValue(
								mExpressionTranslation.constructLiteralForIntegerType(loc, BOOL_TYPE, BigInteger.ZERO),
								BOOL_TYPE))),
				new ConstantModel("true",
						loc -> new ExpressionResult(new RValue(
								mExpressionTranslation.constructLiteralForIntegerType(loc, BOOL_TYPE, BigInteger.ONE),
								BOOL_TYPE))));
	}

}
