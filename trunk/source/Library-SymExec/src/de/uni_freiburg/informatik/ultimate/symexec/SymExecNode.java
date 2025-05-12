package de.uni_freiburg.informatik.ultimate.symexec;

import java.util.HashMap;
import java.util.LinkedHashSet;

import de.uni_freiburg.informatik.ultimate.logic.Term;

public class SymExecNode<LETTER, STATE> {

	final STATE mState; // TODO Dont copy the sate, save a reference to a central map of all states
	LETTER mLetter; // word from parent to this state
	final HashMap<String, Term> mSymbolicValue;
	final Term mPathConstraint;
	final SymExecNode<LETTER, STATE> mParent;
	final LinkedHashSet<SymExecNode<LETTER, STATE>> mChildren = new LinkedHashSet<>();
	TransitionType mTransitionType;

	public enum TransitionType {
		Internal, Call, Return
	}

	public SymExecNode(final SymExecNode<LETTER, STATE> parent, final STATE state, final LETTER transition,
			final TransitionType transitiontype, final HashMap<String, Term> symbolicValue, final Term pathConstraint) {
		mParent = parent; // null if initial
		mState = state;
		mSymbolicValue = symbolicValue;
		mPathConstraint = pathConstraint;
		mLetter = transition;
		mTransitionType = transitiontype;
	}

	public STATE getState() {
		return mState;
	}

	public LETTER getLetter() {
		return mLetter;
	}

	public TransitionType getTransitionType() {
		return mTransitionType;
	}

	public void addChild(final SymExecNode<LETTER, STATE> child) {
		assert !child.equals(this);
		assert !child.equals(mParent);
		mChildren.add(child);
	}

	public LinkedHashSet<SymExecNode<LETTER, STATE>> getChildren() {
		return mChildren;
	}

	public SymExecNode<LETTER, STATE> getParent() {
		return mParent;
	}

	public Term getPathCondition() {
		return mPathConstraint;
	}

	public HashMap<String, Term> getSymbolicValues() {
		return mSymbolicValue;
	}

	@Override
	public String toString() {
		final StringBuilder str = new StringBuilder("State: ").append(mState).append("\n");
		str.append("SymbolicValue: ").append(mSymbolicValue).append("\n");
		str.append("PathConstraint: ").append(mPathConstraint);
		return str.toString();
	}
}
