package de.uni_freiburg.informatik.ultimate.symexec;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.Accepts;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.CfgSmtToolkit;

//Idea, have this realted closely to NWAs.
// A path of the symb exec tree should be a nested word
// In the end we wanna check is accepted to see if we are still in the abstraction

public class SymbolicExecution<LETTER, STATE> {
	// protected final NestedWord<L> mTrace;
	// TODO transfer stuff to worker scripts

	NestedRun<LETTER, STATE> mCounterexample = null;

	public SymbolicExecution(final IUltimateServiceProvider services, final CfgSmtToolkit toolkit,
			final INestedWordAutomaton<LETTER, STATE> abstraction) {
		final SymExecTree<LETTER, STATE> symbolicExecution = new SymExecTree<>(services, toolkit,
				toolkit.getManagedScript(), abstraction, abstraction.getInitialStates());
		final NestedRun<LETTER, STATE> cex = symbolicExecution.getCounterexample();
		try {
			assert new Accepts<>(new AutomataLibraryServices(services), abstraction, cex.getWord(), false, false)
					.getResult();
		} catch (final AutomataLibraryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mCounterexample = cex;

	}

	// TODO create a programExecution, report result using CEGAR infrastructure
	// See trace check computeRcfgProgramExecution()
	public NestedRun<LETTER, STATE> returnFeasibleCounterexample() {
		return mCounterexample;
	}

	// TODO communication between Trace Absrraction and Symbolic execution
	// Both ways, if abstraction is refinet, we check which parts of the three beceme irrelevant
	// IF we are unsat on symbexec, create an infeasible counterexample and refine the abstraction
}
