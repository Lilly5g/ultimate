package de.uni_freiburg.informatik.ultimate.symexec;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.IAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.transitions.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.transitions.OutgoingReturnTransition;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssignmentStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.HavocStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.LeftHandSide;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VariableLHS;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.CfgSmtToolkit;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.LocalProgramVar;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.ProgramNonOldVar;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.ProgramVarUtils;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.ManagedScript;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.PureSubstitution;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtUtils;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtUtils.SimplificationTechnique;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Return;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence;
import de.uni_freiburg.informatik.ultimate.symexec.SymExecNode.TransitionType;

public class SymExecTree<LETTER, STATE> implements IAutomaton<LETTER, STATE> {
	private final ManagedScript mMgdScript;
	INestedWordAutomaton<LETTER, STATE> mAbstraction;
	Set<STATE> mRoots;
	Object mLock;
	CfgSmtToolkit mToolKit;
	NestedRun<LETTER, STATE> mCounterexample = null;
	final Map<Term, Term> mSubstitutionMappingConstants = new HashMap<>();
	HashMap<TermVariable, String> mBaseTermVar2Identifier = new HashMap<>();
	IUltimateServiceProvider mServices;

	// Edges between nodes will be a nested word of length 1
	// a path through the tree a nested word of length n
	public SymExecTree(final IUltimateServiceProvider services, final CfgSmtToolkit cfgToolKit,
			final ManagedScript mgdScript, final INestedWordAutomaton<LETTER, STATE> abstraction,
			final Set<STATE> roots) {
		mServices = services;
		mToolKit = cfgToolKit;
		mMgdScript = mgdScript;
		mAbstraction = abstraction;
		mRoots = roots;
		iterate();

	}

	/**
	 * as long as there are children, explore
	 *
	 */

	void iterate() {
		final ArrayDeque<SymExecNode<LETTER, STATE>> currentChildren = createRootNodes(mRoots);
		while (!currentChildren.isEmpty()) {
			final SymExecNode<LETTER, STATE> currentNode = currentChildren.pop();

			// TODO dont terminate after first violation
			if (buildTree(currentNode)) {
				return;
			}
			for (final SymExecNode<LETTER, STATE> child : currentNode.getChildren()) {
				currentChildren.push(child);
			}

		}
		System.out.println("YEEEHAAAAAAAAAAAA");
	}

	private ArrayDeque<SymExecNode<LETTER, STATE>> createRootNodes(final Set<STATE> initialStates) {
		final ArrayDeque<SymExecNode<LETTER, STATE>> roots = new ArrayDeque<>();
		for (final STATE initialState : initialStates) {
			roots.add(
					new SymExecNode<>(null, initialState, null, null, new HashMap<>(), mMgdScript.term(null, "true")));
		}
		return roots;
	}

	private SymExecNode<LETTER, STATE> createChildNode(final SymExecNode<LETTER, STATE> parent, final STATE newNode,
			final LETTER incomingTransition, final TransitionType incomingTransitionType,
			final HashMap<String, Term> symbolicValue, final Term pathCondition) {
		return new SymExecNode<>(parent, newNode, incomingTransition, incomingTransitionType, symbolicValue,
				pathCondition);

	}

	private boolean buildTree(final SymExecNode<LETTER, STATE> rootOfSubtree) {
		final STATE state = rootOfSubtree.getState();
		if (mAbstraction.isFinal(state)) {
			caluclateCounterexampleRun(rootOfSubtree);
			System.out.println("FoundPathTOError " + rootOfSubtree);
			return true;
		}
		for (final OutgoingInternalTransition<LETTER, STATE> transition : mAbstraction.internalSuccessors(state)) {
			System.out.println(transition);
			getChildrenFromInternalTransition(rootOfSubtree, transition);
		}
		for (final OutgoingCallTransition<LETTER, STATE> transition : mAbstraction.callSuccessors(state)) {
			System.out.println(transition);
			getChildrenFromCallTransition(rootOfSubtree, transition);
		}
		for (final OutgoingReturnTransition<LETTER, STATE> transition : mAbstraction.returnSuccessors(state)) {
			System.out.println(transition);
			getChildrenFromReturnTransition(rootOfSubtree, transition);
		}
		return false;
	}

	private void getChildrenFromCallTransition(final SymExecNode<LETTER, STATE> rootOfSubtree,
			final OutgoingCallTransition<LETTER, STATE> transition) {
		assert transition.getLetter() instanceof Call;
		// I guess we treat a call as asignment hence update the symb state

		final STATE succ = transition.getSucc();

		final UnmodifiableTransFormula transformula = ((Call) transition.getLetter()).getTransformula();
		if (transformula.getFormula().equals(mMgdScript.term(null, "true"))) {
			final SymExecNode<LETTER, STATE> child = createChildNode(rootOfSubtree, succ, transition.getLetter(),
					TransitionType.Call, rootOfSubtree.getSymbolicValues(), rootOfSubtree.getPathCondition());
			rootOfSubtree.addChild(child);
			return;
		}
		assert transformula.getAuxVars().isEmpty();

		final HashMap<String, Term> symbolicValues = (HashMap<String, Term>) rootOfSubtree.getSymbolicValues().clone();

		final HashMap<String, Term> newSymbolicValuesUpdate = new HashMap<>();

		final Map<Term, Term> substitutionMapping = getConstants(transformula);

		final ApplicationTerm callTerm = (ApplicationTerm) transformula.getFormula();
		if (callTerm.getFunction().toString().equals("and")) {
			for (final Term callParam : callTerm.getParameters()) {
				// final HashMap<String, Term> relevantSymVals =
				// getRevelevantSymVals(symbolicValues, callParam, transformula, substitutionMapping);
				// newSymbolicValuesUpdate.putAll(relevantSymVals);
			}
		}

		assert checkSymbolicStateContainsOnlyOutvars();
		final SymExecNode<LETTER, STATE> child = createChildNode(rootOfSubtree, succ, transition.getLetter(),
				TransitionType.Call, newSymbolicValuesUpdate, rootOfSubtree.getPathCondition());
		rootOfSubtree.addChild(child);
	}

	private void getChildrenFromReturnTransition(final SymExecNode<LETTER, STATE> rootOfSubtree,
			final OutgoingReturnTransition<LETTER, STATE> transition) {
		assert transition.getLetter() instanceof Return;

		final STATE succ = transition.getSucc();
		final UnmodifiableTransFormula transformula = ((Return) transition.getLetter()).getTransformula();

		if (transformula.getFormula().equals(mMgdScript.term(null, "true"))) {
			final SymExecNode<LETTER, STATE> child = createChildNode(rootOfSubtree, succ, transition.getLetter(),
					TransitionType.Return, rootOfSubtree.getSymbolicValues(), rootOfSubtree.getPathCondition());
			rootOfSubtree.addChild(child);
			return;
		}

		assert transformula.getAuxVars().isEmpty();

		final HashMap<String, Term> symbolicValues = (HashMap<String, Term>) rootOfSubtree.getSymbolicValues().clone();

		final HashMap<String, Term> newSymbolicValuesUpdate = new HashMap<>();

		final Map<Term, Term> substitutionMapping = getConstants(transformula);

		// final HashMap<String, Term> relevantSymVals =
		// getRevelevantSymVals(symbolicValues, transformula.getFormula(), transformula, substitutionMapping);

		// newSymbolicValuesUpdate.putAll(relevantSymVals);

		assert checkSymbolicStateContainsOnlyOutvars();
		final SymExecNode<LETTER, STATE> child = createChildNode(rootOfSubtree, succ, transition.getLetter(),
				TransitionType.Return, newSymbolicValuesUpdate, rootOfSubtree.getPathCondition());
		rootOfSubtree.addChild(child);
	}

	private void getChildrenFromInternalTransition(final SymExecNode<LETTER, STATE> rootOfSubtree,
			final OutgoingInternalTransition<LETTER, STATE> transition) {

		assert transition.getLetter() instanceof StatementSequence;

		final STATE succ = transition.getSucc();
		final StatementSequence statementSequence = ((StatementSequence) transition.getLetter());
		final UnmodifiableTransFormula transformula = statementSequence.getTransformula();
		System.out.println("----------------");
		System.out.println("Old states: ");
		System.out.println(rootOfSubtree.getSymbolicValues());
		System.out.println(rootOfSubtree.getPathCondition());

		if (transformula.getFormula().equals(mMgdScript.term(null, "true"))) {
			final SymExecNode<LETTER, STATE> child = createChildNode(rootOfSubtree, succ, transition.getLetter(),
					TransitionType.Internal, rootOfSubtree.getSymbolicValues(), rootOfSubtree.getPathCondition());
			rootOfSubtree.addChild(child);
			return;
		}

		Term newPathCondition = mMgdScript.term(null, "true");

		final HashMap<String, Term> symbolicValues = (HashMap<String, Term>) rootOfSubtree.getSymbolicValues().clone();
		final HashMap<TermVariable, Term> auxVars = new HashMap<>();
		mSubstitutionMappingConstants.putAll(getConstants(transformula));
		final HashMap<String, Term> newSymbolicValuesUpdate = new HashMap<>(symbolicValues);

		for (final Statement stmt : statementSequence.getStatements()) {
			Term formula = stmt.getSMTFormula();// transformula.getStmtToTermMap().get(stmt);
			if (formula == null) {
				// continue;
			} else {
				for (final Entry<IProgramVar, TermVariable> inVar : transformula.getInVars().entrySet()) {
					if (inVar.getKey() instanceof LocalProgramVar) {
						final String identifier = ((LocalProgramVar) inVar.getKey()).getIdentifier();
						final TermVariable baseTv = mMgdScript.getBaseTermVariable(inVar.getValue());
						mBaseTermVar2Identifier.put(baseTv, identifier);
					} else if (inVar.getKey() instanceof ProgramNonOldVar) {
						final String identifier = ((ProgramNonOldVar) inVar.getKey()).getIdentifier();
						final TermVariable baseTv = mMgdScript.getBaseTermVariable(inVar.getValue());
						mBaseTermVar2Identifier.put(baseTv, identifier);
					} else {
						throw new AssertionError("Unexpected IProgramVar");
					}

				}

				for (final Entry<IProgramVar, TermVariable> outVar : transformula.getOutVars().entrySet()) {
					if (outVar.getKey() instanceof LocalProgramVar) {
						final String identifier = ((LocalProgramVar) outVar.getKey()).getIdentifier();
						final TermVariable baseTv = mMgdScript.getBaseTermVariable(outVar.getValue());
						mBaseTermVar2Identifier.put(baseTv, identifier);
					} else if (outVar.getKey() instanceof ProgramNonOldVar) {
						final String identifier = ((ProgramNonOldVar) outVar.getKey()).getIdentifier();
						final TermVariable baseTv = mMgdScript.getBaseTermVariable(outVar.getValue());
						mBaseTermVar2Identifier.put(baseTv, identifier);
					} else {
						throw new AssertionError("Unexpected IProgramVar");
					}
				}

			}
			System.out.println("stmt " + stmt.getClass());
			System.out.println("Statement Formula " + formula);
			if (stmt instanceof AssignmentStatement) {

				final AssignmentStatement assignment = (AssignmentStatement) stmt;
				final LeftHandSide[] lhs = assignment.getLhs();
				final de.uni_freiburg.informatik.ultimate.boogie.ast.Expression[] rhs = assignment.getRhs();

				for (final LeftHandSide assignedObject : lhs) {
					if (assignedObject instanceof VariableLHS) {
						final VariableLHS variable = (VariableLHS) assignedObject;
						if (newSymbolicValuesUpdate.containsKey(variable.getIdentifier())) {
							for (final de.uni_freiburg.informatik.ultimate.boogie.ast.Expression expression : rhs) {
								final Term newRhsTerm = updateSymbolicValue(newSymbolicValuesUpdate, expression);
								newSymbolicValuesUpdate.put(variable.getIdentifier(), newRhsTerm);
							}
						} else {
							for (final de.uni_freiburg.informatik.ultimate.boogie.ast.Expression expression : rhs) {
								Term rhsTerm = expression.getSMTFormula();
								if (rhsTerm == null) {
									final IdentifierExpression identifierExp = (IdentifierExpression) expression;
									rhsTerm = newSymbolicValuesUpdate.get(identifierExp.getIdentifier());
								}
								newSymbolicValuesUpdate.put(variable.getIdentifier(), rhsTerm);
							}
						}
					} else {
						throw new AssertionError("ArrayLHS and StructLHS not yet supported in Symbolic Execution");
					}
				}

			} else if (stmt instanceof HavocStatement) {
				final HavocStatement havoc = (HavocStatement) stmt;

				for (final Entry<IProgramVar, TermVariable> outVar : transformula.getOutVars().entrySet()) {
					for (final VariableLHS var : havoc.getIdentifiers()) {
						if (((LocalProgramVar) outVar.getKey()).getIdentifier().equals(var.getIdentifier())) {
							// TODO probably need sth elste then the boogie stmt var as argument
							// TODO ensure argument is fresh
							newSymbolicValuesUpdate.put(var.getIdentifier(), var.getSMTFormula());
						}
					}
				}

				// continue;

			} else if (stmt instanceof AssumeStatement) {

				// TODO normalize variable names
				// TODO Substitute variable in new PathCond by their symbolic state
				// TODO checksat and if we explore

				final HashMap<Term, Term> substitutionMappingSymValues = new HashMap<>();

				for (final TermVariable baseTvInFormula : formula.getFreeVars()) {
					if (newSymbolicValuesUpdate.containsKey(
							mBaseTermVar2Identifier.get(mMgdScript.getBaseTermVariable(baseTvInFormula)))) {
						substitutionMappingSymValues.put(baseTvInFormula, newSymbolicValuesUpdate
								.get(mBaseTermVar2Identifier.get(mMgdScript.getBaseTermVariable(baseTvInFormula))));
					} else {
						assert false;
					}

				}

				for (final TermVariable baseTvInFormula : formula.getFreeVars()) {
					if (!mSubstitutionMappingConstants.containsKey(baseTvInFormula)) {
						final Term newConstant =
								SmtUtils.termVariable2constant(mMgdScript.getScript(), baseTvInFormula, true);
						mSubstitutionMappingConstants.put(baseTvInFormula, newConstant);
					}
				}

				System.out.println("--------");
				System.out.println(substitutionMappingSymValues);
				System.out.println(mSubstitutionMappingConstants);
				System.out.println("--------");
				if (substitutionMappingSymValues.containsValue(null)) {
					assert false;
				}

				formula = PureSubstitution.apply(mMgdScript.getScript(), substitutionMappingSymValues, formula);

				formula = PureSubstitution.apply(mMgdScript.getScript(), mSubstitutionMappingConstants, formula);
				// The old pathCondition consists of constants already, but that doesnt matter.
				formula = SmtUtils.simplify(mMgdScript, formula, mServices, SimplificationTechnique.SIMPLIFY_DDA2);
				newPathCondition = constructNewPathCondition(rootOfSubtree.getPathCondition(), formula);
			}
		}

		// after replacing everything with their respective constant
		// substitute for the checksat in path condition with sym values
		// path condition must not contain any auxvars anymore
		System.out.println("new states: ");
		System.out.println(newSymbolicValuesUpdate);
		System.out.println(newPathCondition);
		System.out.println("----------------");
		if (newPathCondition.equals(mMgdScript.term(null, "true")) || exploreSuccessor(newPathCondition)) {
			assert checkSymbolicStateContainsOnlyOutvars();
			final SymExecNode<LETTER, STATE> child = createChildNode(rootOfSubtree, succ, transition.getLetter(),
					TransitionType.Internal, newSymbolicValuesUpdate, newPathCondition);
			rootOfSubtree.addChild(child);
		}

	}

	private Term updateSymbolicValue(final HashMap<String, Term> newSymbolicValuesUpdate, final Expression newValue) {

		final HashMap<Term, Term> substitutionMappingSymValues = new HashMap<>();

		for (final TermVariable baseTvInFormula : newValue.getSMTFormula().getFreeVars()) {
			substitutionMappingSymValues.put(baseTvInFormula, newSymbolicValuesUpdate
					.get(mBaseTermVar2Identifier.get(mMgdScript.getBaseTermVariable(baseTvInFormula))));
		}

		for (final TermVariable baseTvInFormula : newValue.getSMTFormula().getFreeVars()) {
			if (!mSubstitutionMappingConstants.containsKey(baseTvInFormula)) {
				final Term newConstant = SmtUtils.termVariable2constant(mMgdScript.getScript(), baseTvInFormula, true);
				mSubstitutionMappingConstants.put(baseTvInFormula, newConstant);
			}
		}

		Term newSymValueAsTerm = newValue.getSMTFormula();
		newSymValueAsTerm =
				PureSubstitution.apply(mMgdScript.getScript(), substitutionMappingSymValues, newSymValueAsTerm);
		newSymValueAsTerm =
				PureSubstitution.apply(mMgdScript.getScript(), mSubstitutionMappingConstants, newSymValueAsTerm);
		return newSymValueAsTerm;
	}

	private Map<Term, Term> getConstants(final UnmodifiableTransFormula transformula) {
		final Map<Term, Term> substitutionMapping = new HashMap<>();
		for (final Entry<IProgramVar, TermVariable> entry : transformula.getInVars().entrySet()) {
			final TermVariable inTermVar = entry.getValue();
			assert !substitutionMapping.containsKey(inTermVar);
			substitutionMapping.put(mMgdScript.getBaseTermVariable(inTermVar), entry.getKey().getDefaultConstant());
		}
		for (final Entry<IProgramVar, TermVariable> entry : transformula.getOutVars().entrySet()) {
			final IProgramVar outVar = entry.getKey();
			final TermVariable outTermVar = entry.getValue();
			System.out.println(mMgdScript.getBaseTermVariable(outTermVar));
			System.out.println(entry.getKey().getDefaultConstant());
			if (transformula.getInVars().get(outVar) == outTermVar) {
				// is handled above
				continue;
			}
			substitutionMapping.put(mMgdScript.getBaseTermVariable(outTermVar), entry.getKey().getDefaultConstant());
		}
		for (final TermVariable auxVarTv : transformula.getAuxVars()) {
			final Term auxVarConst = ProgramVarUtils.getAuxVarConstant(mMgdScript, auxVarTv);
			substitutionMapping.put(mMgdScript.getBaseTermVariable(auxVarTv), auxVarConst);
			assert false;
		}
		return substitutionMapping;
	}

	/*
	 * TODO dont create that many maps, just have one per transformula
	 *
	 * Iterate over all old symbolic entries, check which occur as invar, check which occur in formula. Then update
	 *
	 *
	 * stmtFormula does not contain constants yet! symbolicValues does
	 */
	private void getRevelevantSymVals(final HashMap<IProgramVar, Term> symbolicValues, final Term stmtFormula,
			final UnmodifiableTransFormula unmodifiableTransFormula, final Map<Term, Term> substitutionMapping) {

		assert false;

	}

	/*
	 * Some statements might contain auxvars we replace auxvars with a fresh constant, otherwise the script doesnt know
	 * them.
	 */
	private HashMap<TermVariable, Term> getRevelevantAuxVars(final HashMap<TermVariable, Term> auxVars,
			final Term stmtFormula, final UnmodifiableTransFormula unmodifiableTransFormula,
			final Map<Term, Term> substitutionMapping) {
		final HashMap<TermVariable, Term> updatedAuxVars = new HashMap<>();
		if (unmodifiableTransFormula.getAuxVars().isEmpty()) {
			return updatedAuxVars;
		}
		final Term oneHandSideOfAssignment = getAssignedValueAux(stmtFormula, auxVars.keySet());

		int count = 0;
		for (final TermVariable tv : unmodifiableTransFormula.getAuxVars()) {
			for (final TermVariable freeVar : stmtFormula.getFreeVars()) {
				if (freeVar.equals(tv)) {
					Term updated = SmtUtils.and(mMgdScript.getScript(), auxVars.get(tv), oneHandSideOfAssignment);
					updated = PureSubstitution.apply(mMgdScript.getScript(), substitutionMapping, updated);
					updatedAuxVars.put(tv, updated);
					count += 1;
				}
			}

		}
		assert count <= 1; // only one auxvar changes value per assignment
		return updatedAuxVars;
	}

	// We assume, one side of the input formula is a TermVariable the other an expression
	private Term getAssignedValueAux(final Term stmtFormula, final Set<TermVariable> auxVars) {
		assert stmtFormula instanceof ApplicationTerm;
		final Term[] params = ((ApplicationTerm) stmtFormula).getParameters();
		assert params.length == 2;

		if (params[0] instanceof TermVariable) {
			if (params[1] instanceof TermVariable) {
				if (auxVars.contains(params[0])) {
					return params[1];
				}
				assert auxVars.contains(params[1]);
				return params[0];
			}
			assert params[1] instanceof ApplicationTerm;
			return params[1];
		}
		assert params[1] instanceof TermVariable;
		assert params[0] instanceof ApplicationTerm;
		return params[0];

	}

	private boolean checkSymbolicStateContainsOnlyOutvars() {
		// TODO
		return true;
	}

	private Term constructNewPathCondition(final Term pathCondition, final Term branchcondition) {

		Term newPathCondition = SmtUtils.and(mMgdScript.getScript(), pathCondition, branchcondition);
		newPathCondition =
				SmtUtils.simplify(mMgdScript, newPathCondition, mServices, SimplificationTechnique.SIMPLIFY_DDA2);
		return newPathCondition;
	}

	/*
	 * Check a PathCondition for satisfiability
	 */
	private boolean exploreSuccessor(final Term pathCondition) {
		mMgdScript.push(mLock, 1);
		mMgdScript.assertTerm(mLock, pathCondition);
		final LBool sat = mMgdScript.checkSat(mLock);
		mMgdScript.pop(mLock, 1);
		// TODO can we somehow leave SAT result on assertion stack?
		// Maybe with some sort of DFS combined with stack level == tree depth
		if (sat.equals(LBool.UNSAT)) {
			System.out.println("UNSAT somehow doublecheck via abstraction");
		}
		return sat.equals(LBool.SAT);
	}

	private void caluclateCounterexampleRun(final SymExecNode<LETTER, STATE> leaf) {
		SymExecNode<LETTER, STATE> currentNode = leaf;
		NestedRun<LETTER, STATE> cex = new NestedRun<>(leaf.getState());
		while (currentNode.getParent() != null) {

			switch (currentNode.getTransitionType()) {
			case Internal:
				cex = new NestedRun<>(currentNode.getParent().getState(), currentNode.getLetter(),
						NestedWord.INTERNAL_POSITION, currentNode.getState()).concatenate(cex);
				break;
			case Call:
				cex = new NestedRun<>(currentNode.getParent().getState(), currentNode.getLetter(),
						NestedWord.PLUS_INFINITY, currentNode.getState()).concatenate(cex);
				break;
			case Return:
				cex = new NestedRun<>(currentNode.getParent().getState(), currentNode.getLetter(),
						NestedWord.MINUS_INFINITY, currentNode.getState()).concatenate(cex);
				break;
			default:
				throw new AssertionError("Unknown Transition Type");
			}

			currentNode = currentNode.getParent();
		}
		mCounterexample = cex;
		System.out.println(cex);
	}

	public NestedRun<LETTER, STATE> getCounterexample() {
		return mCounterexample;
	}

	@Override
	public Set getAlphabet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String sizeInformation() {
		// TODO Auto-generated method stub
		return "TODO sizeInformation for SymExecTree";
	}

	@Override
	public IElement transformToUltimateModel(final AutomataLibraryServices services)
			throws AutomataOperationCanceledException {
		throw new AssertionError("Cannot transform Symbolic Execution Tree to Ultimate Model");
	}

}
