package de.uni_freiburg.informatik.ultimate.pea2boogie.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.lib.pea.CDD;
import de.uni_freiburg.informatik.ultimate.lib.pea.CounterTrace;
import de.uni_freiburg.informatik.ultimate.lib.pea.CounterTrace.DCPhase;
import de.uni_freiburg.informatik.ultimate.lib.pea.PhaseEventAutomata;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtUtils;
import de.uni_freiburg.informatik.ultimate.lib.srparse.pattern.PatternType;
import de.uni_freiburg.informatik.ultimate.lib.srparse.pattern.PatternType.ReqPeas;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.pea2boogie.CddToSmt;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;

public class RtInconsistencyPreCheck {

	public List<CtWithAttributes> ctAttributesList;
	public Map<String, List<CtWithAttributes>> variablesDict;
	public List<CtWithAttributes> chainLinkReqs;
	public ILogger mLogger;
	public List<List<ReqPeas>> mListRtInconsistenSet;

	public CddToSmt mCddToSmt;
	public Script mScript;

	public static final class CtWithAttributes {

		public boolean mTimed = false;
		public ReqPeas mReqPea;
		public CDD mFullExitCondition;
		public CDD[] mExitConditions;
		public boolean mChainLinkRequirement;
		public String mName;
		public DCPhase mMaxPhase;

		public CtWithAttributes(final boolean timed, final ReqPeas req, final CDD[] exitConditions,
				final CDD fullExitCondiotion, final boolean chainLinkRequirement, final String name,
				final DCPhase maxPhase) {
			mTimed = timed;
			mReqPea = req;
			mExitConditions = exitConditions;
			mFullExitCondition = fullExitCondiotion;
			mChainLinkRequirement = chainLinkRequirement;
			mName = name;
			mMaxPhase = maxPhase;
		}

	}

	public RtInconsistencyPreCheck(final ILogger logger) {
		mLogger = logger;
		// TODO Auto-generated constructor stub
	}

	public void getAttributes(final ReqPeas reqParent) {

		for (final Entry<CounterTrace, PhaseEventAutomata> req : reqParent.getCounterTrace2Pea()) {
			mLogger.info("newReq");
			final CtWithAttributes newly = new CtWithAttributes(false, reqParent, null, null, false, null, null);
			newly.mName = req.getValue().getName();
			final List<String> clocks = req.getValue().getClocks();
			if (clocks.size() > 0) {
				newly.mTimed = true;
			}
			final CounterTrace ct = req.getKey();
			final DCPhase[] phases = ct.getPhases();
			final DCPhase penultimate = phases[phases.length - 2];
			final CDD exitCondition = penultimate.getInvariant().negate();
			final CDD[] exitOptions = exitCondition.toDNF();
			newly.mExitConditions = exitOptions;
			newly.mFullExitCondition = exitCondition;
			if (exitOptions.length > 1) {
				newly.mChainLinkRequirement = true;
				chainLinkReqs.add(newly);
			} else {
				// if not chain link req, add to variables dict
				final Map<String, String> variables = req.getValue().getVariables();
				for (final Map.Entry<String, String> variable : variables.entrySet()) {
					if (!variablesDict.containsKey(variable.getKey())) {
						variablesDict.put(variable.getKey(), new ArrayList<>());
					}

				}
			}

			// Max Phase for less false positives
			if (penultimate.getBoundType() != 0) {
				newly.mMaxPhase = phases[phases.length - 2];
			} else {
				newly.mMaxPhase = phases[phases.length - 3];
			}

			if (!newly.mChainLinkRequirement) {
				for (final CDD exitcon : newly.mExitConditions) {
					final boolean hughi = true;
					final CDD[] exitAnds = exitcon.toCNF();
					for (final CDD exitVar : exitAnds) {
						for (final Entry<String, List<CtWithAttributes>> variable : variablesDict.entrySet()) {
							if (exitVar.toTexString().contains(variable.getKey())
									&& !variable.getValue().contains(newly)) {
								variablesDict.get(variable.getKey()).add(newly);
							}
						}
					}
				}
			}
		}
	}

	// Checks if two ExitConditions can be satisfied, returns FALSe if not
	// Checks also if MAxPhase can be satisfied aswell, to eliminate false positives
	// TODO: no maxphase
	public boolean satCheck(final CtWithAttributes req1, final CtWithAttributes req2) {
		final CDD comp = req1.mFullExitCondition.and(req2.mFullExitCondition);
		if (comp == CDD.FALSE) {
			final boolean help = true;
		}

		final Term smtTerm = mCddToSmt.toSmt(comp);
		final LBool result = SmtUtils.checkSatTerm(mScript, smtTerm);

		if (result == LBool.UNSAT) {
			mLogger.info("rti found");
			// cheeck if max phases are compatibel
			final CDD compMaxInvariant = req1.mMaxPhase.getInvariant().and(req2.mMaxPhase.getInvariant());
			if (!compMaxInvariant.equals(CDD.FALSE)) {
				if (req1.mMaxPhase.getInvariant() == req2.mMaxPhase.getInvariant()) {
					if ((req1.mMaxPhase.getBoundType() == 1 || req1.mMaxPhase.getBoundType() == 2)
							&& (req2.mMaxPhase.getBoundType() == -1 || req2.mMaxPhase.getBoundType() == -2)) {
						if (req1.mMaxPhase.getBound() > req2.mMaxPhase.getBound()) {
							mLogger.info("Ausgeschlossen wegen maxPhase falsche BoundTypes");
							return true;
						}
					} else if ((req2.mMaxPhase.getBoundType() == 1 || req2.mMaxPhase.getBoundType() == 2)
							&& (req1.mMaxPhase.getBoundType() == -1 || req1.mMaxPhase.getBoundType() == -2)) {
						if (req2.mMaxPhase.getBound() > req1.mMaxPhase.getBound()) {
							mLogger.info("Ausgeschlossen wegen maxPhase falsche BoundTypes");
							return true;
						}
					}

				}

				return false;
			} else {
				mLogger.info("Ausgeschlossen wegen maxPhase nicht parallel erreichbar");
			}

		}

		return true;
	}

	public void makePreCheck(final List<ReqPeas> reqPeas) {
		variablesDict = new HashMap();
		mListRtInconsistenSet = new ArrayList<>();

		chainLinkReqs = new ArrayList<>();
		for (final ReqPeas reqPea : reqPeas) {
			getAttributes(reqPea);
		}
		mLogger.info("sortieren abgeschlossen");

		// actual rti Check

		// for singles
		int counter = 1;
		for (final Entry<String, List<CtWithAttributes>> ct : variablesDict.entrySet()) {
			counter = 1;
			mLogger.info("Variable:" + ct.getKey());
			final List<CtWithAttributes> listi = ct.getValue();
			for (final CtWithAttributes req : ct.getValue()) {

				for (int i = counter; i < ct.getValue().size(); i++) {
					mLogger.info("counter:" + counter);
					final CtWithAttributes req2 = listi.get(i);
					mLogger.info("neuer Vergleich");
					mLogger.info(req.mName);
					mLogger.info(req2.mName);
					if (!satCheck(req, req2)) {
						final List<ReqPeas> rtInconsistentSet = new ArrayList<>();
						rtInconsistentSet.add(req.mReqPea);
						rtInconsistentSet.add(req2.mReqPea);
						mListRtInconsistenSet.add(rtInconsistentSet);

					}
				}
				counter++;

			}

		}

		for (final List<ReqPeas> rtSet : mListRtInconsistenSet) {
			mLogger.info("-------- RTIFOUND------------");
			for (final ReqPeas req : rtSet) {
				mLogger.info(req.getPattern());

			}
		}
		mLogger.info("PRE CHECK ABGESCHLOSSEN");

	}

	public List<Entry<PatternType<?>, PhaseEventAutomata>> makePreCheckList(final List<ReqPeas> reqPeas,
			final CddToSmt smtToCDD, final Script script) {
		mCddToSmt = smtToCDD;
		mScript = script;
		makePreCheck(reqPeas);
		final List<Entry<PatternType<?>, PhaseEventAutomata>> rtr = new ArrayList<>();
		for (final List<ReqPeas> rtSet : mListRtInconsistenSet) {
			for (final ReqPeas req : rtSet) {
				final PatternType<?> pattern = req.getPattern();
				for (final Entry<CounterTrace, PhaseEventAutomata> pea : req.getCounterTrace2Pea()) {
					final Pair<PatternType<?>, PhaseEventAutomata> reqi = new Pair<>(pattern, pea.getValue());
					if (!rtr.contains(reqi)) {
						rtr.add(reqi);
					}
				}

			}
		}
		return rtr;
	}

}
