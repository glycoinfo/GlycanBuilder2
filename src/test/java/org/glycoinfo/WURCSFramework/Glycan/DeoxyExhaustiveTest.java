package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Every deoxy this application can be handed, on every neutral monosaccharide it knows, checked against
 * WURCSFramework rather than against this application's own arithmetic (#258, #263).
 *
 * <h3>The cases</h3>
 *
 * <p>The bases are every saccharide type in the two dictionaries, written alone by this application's
 * own WURCS writer, kept when they are neutral sugars with no substituent: a skeleton made only of
 * {@code a}, the stereo digits, {@code x}, {@code h}, {@code d} and {@code m}. That restriction is what
 * lets the hydroxyls be counted independently below.
 *
 * <p>On each base, every carbon that can lose its oxygen does, alone and in pairs: an internal
 * stereocentre becomes {@code d}, a terminal {@code h} becomes {@code m}. The anomeric carbon, the
 * carbon that closes the ring, and anything already deoxy are left. The base itself, with no deoxy
 * added, is a case too, and is the control: if the oracles disagree with this application on a residue
 * it already knows, it is the oracle that is wrong and every other result is unreadable.
 *
 * <p>Each of those in three anomers; with and without a 3-O-methyl where carbon 3 still has its
 * hydroxyl; and in two places, alone on a free reducing end and as the non-reducing end of a
 * {@code β1-4} link to glucose.
 *
 * <h3>What is checked, and against what</h3>
 *
 * <ul>
 * <li>No residue named {@code d} or {@code m} is built. <i>Structural.</i>
 * <li>The mass is {@code WURCSMassCalculator.calcMassWURCS} of the input. <i>WURCSFramework.</i>
 * <li>Permethylated and peracetylated, it is that mass plus one group per free hydroxyl, the hydroxyls
 * counted from the skeleton code - one per {@code h}, stereo digit, {@code x} or anomeric {@code a},
 * none for {@code d} or {@code m}, the ring carbon, a linked position or the methylated one. <i>The
 * WURCS definition, not this application's dictionary.</i>
 * <li>Written back to WURCS it is the input as WURCSFramework normalizes it. <i>WURCSFramework.</i>
 * <li>Written to GWS and read back, it is the same text and the same mass. <i>Itself.</i>
 * </ul>
 *
 * <p>All failures are collected and reported together, not stopped at the first, so that what fails
 * can be grouped by cause.
 */
public class DeoxyExhaustiveTest {

	private static final double CH2 = 14.01565006414;       // a methyl group in place of a hydrogen
	private static final double C2H2O = 42.01056468637;     // an acetyl group in place of a hydrogen
	private static final String NEUTRAL = "a1234xhdm";
	/** Glucose as the parent, with the hydroxyls it keeps when its 4 carries the case: 1, 2, 3, 6. */
	private static final String GLC = "a2122h-1b_1-5";
	private static final int GLC_FREE_HYDROXYLS = 4;

	@BeforeClass
	public static void loadDictionaries() {
		new BuilderWorkspace(new GlycanRendererAWT());
	}

	@Test
	public void everyDeoxyOnEveryNeutralMonosaccharide() throws Exception {
		List<Base> bases = bases();
		assertTrue("expected dozens of neutral monosaccharides, found " + bases.size(), bases.size() > 20);

		Map<String, List<String>> failuresByCheck = new LinkedHashMap<String, List<String>>();
		int cases = 0, controls = 0;
		for (Base base : bases) {
			for (Set<Integer> deoxies : deoxyChoices(base)) {
				for (char anomer : new char[] { 'a', 'b', 'x' }) {
					for (boolean methyl : new boolean[] { false, true }) {
						if (methyl && !hasHydroxyl(base, deoxies, 3)) continue;
						for (boolean onGlucose : new boolean[] { false, true }) {
							Case c = new Case(base, deoxies, anomer, methyl, onGlucose);
							// a WURCS lists each distinct residue once, so glucose on glucose is a
							// different header, not a different case
							if (onGlucose && c.ms.equals(GLC)) continue;
							cases++;
							if (deoxies.isEmpty()) controls++;
							check(c, failuresByCheck);
						}
					}
				}
			}
		}

		// Every failure is either one of the known, older causes - each classified where it is recorded,
		// see causeOf - or it is a failure of this change. The known ones are counted exactly: a count
		// that moves means one of those causes was fixed or has spread, and either way KNOWN_COUNTS must
		// be looked at again rather than updated to whatever came out.
		Map<String, Integer> known = new java.util.TreeMap<String, Integer>();
		List<String> unexplained = new ArrayList<String>();
		for (Map.Entry<String, List<String>> e : failuresByCheck.entrySet())
			for (String one : e.getValue()) {
				String cause = one.substring(0, one.indexOf(" || "));
				if (cause.equals(UNEXPLAINED)) unexplained.add(e.getKey() + ": " + one);
				else known.put(cause, (known.containsKey(cause) ? known.get(cause) : 0) + 1);
			}

		StringBuilder report = new StringBuilder();
		report.append(cases).append(" cases (").append(controls).append(" controls) on ").append(bases.size())
				.append(" neutral monosaccharides\n");
		int failed = 0;
		for (Map.Entry<String, List<String>> e : failuresByCheck.entrySet()) {
			failed += e.getValue().size();
			report.append("\n").append(e.getKey()).append(": ").append(e.getValue().size()).append('\n');
			for (String one : e.getValue().subList(0, Math.min(8, e.getValue().size())))
				report.append("    ").append(one).append('\n');
		}
		report.append("\nknown causes: ").append(known).append('\n');
		System.out.println(report);
		writeFullReport(failuresByCheck, report.toString().split("\n")[0]);

		assertTrue(unexplained.size() + " failures no known cause explains, of " + failed + "\n"
				+ String.join("\n", unexplained.subList(0, Math.min(20, unexplained.size()))),
				unexplained.isEmpty());
		assertTrue("the known failures moved - read the comment on KNOWN before changing the counts\n"
				+ "expected " + KNOWN_COUNTS + "\nfound    " + known, known.equals(KNOWN_COUNTS));
	}

	// ------------------------------------------------------------------------------------------------

	/** One input: a base, the carbons made deoxy, an anomer, a methyl or not, alone or on glucose. */
	private static final class Case {
		final Base base;
		final Set<Integer> deoxies;
		final char anomer;
		final boolean methyl;
		final boolean onGlucose;
		final String skeleton;
		final String ms;
		final String wurcs;
		/** What the reader made of it, once read; for classifying a failure. */
		Glycan readAs;

		Case(Base base, Set<Integer> deoxies, char anomer, boolean methyl, boolean onGlucose) {
			this.base = base;
			this.deoxies = deoxies;
			this.anomer = anomer;
			this.methyl = methyl;
			this.onGlucose = onGlucose;

			char[] carbons = base.skeleton.toCharArray();
			for (int position : deoxies)
				carbons[position - 1] = (position == carbons.length) ? 'm' : 'd';
			this.skeleton = new String(carbons);
			this.ms = this.skeleton + "-" + base.anomericPosition + anomer + "_" + base.ring
					+ (methyl ? "_3*OC" : "");
			this.wurcs = onGlucose
					? "WURCS=2.0/2,2,1/[" + GLC + "][" + this.ms + "]/1-2/a4-b" + base.anomericPosition
					: "WURCS=2.0/1,1,0/[" + this.ms + "]/1/";
		}

		/** Free hydroxyls, counted from the WURCS definition of each carbon. */
		int freeHydroxyls() {
			int count = 0;
			for (int position = 1; position <= this.skeleton.length(); position++) {
				char carbon = this.skeleton.charAt(position - 1);
				if (carbon == 'd' || carbon == 'm') continue;
				if (position == this.base.ringEnd) continue;
				if (position == this.base.anomericPosition && this.onGlucose) continue;
				if (position == 3 && this.methyl) continue;
				if ("1234xha".indexOf(carbon) >= 0) count++;
			}
			return count + (this.onGlucose ? GLC_FREE_HYDROXYLS : 0);
		}

		@Override
		public String toString() {
			return this.base.name + " " + this.wurcs;
		}
	}

	private static void check(Case c, Map<String, List<String>> failures) {
		c.readAs = null;
		Glycan read;
		try {
			read = new WURCS2Parser().readGlycan(c.wurcs, new MassOptions());
		} catch (Exception e) {
			fail(failures, "does not read", c, String.valueOf(e.getMessage()));
			return;
		}
		c.readAs = read;

		// 1. no residue named d or m
		for (Residue r : read.getAllResidues())
			if (r.getTypeName().equals("d") || r.getTypeName().equals("m"))
				fail(failures, "a deoxy built as a child residue", c, r.getTypeName());

		// 2. the mass, against WURCSFramework
		double expected;
		try {
			expected = WURCSMassCalculator.calcMassWURCS(new WURCSFactory(c.wurcs).getArray()).doubleValue();
		} catch (Exception e) {
			fail(failures, "WURCSFramework cannot weigh it (oracle)", c, String.valueOf(e.getMessage()));
			return;
		}
		double underivatized = read.computeMass();
		if (Math.abs(underivatized - expected) > 1e-3)
			fail(failures, "mass differs from WURCSFramework", c,
					String.format("%.4f, WURCSFramework %.4f, read as %s", underivatized, expected, readAs(read)));

		// 3. derivatized, against hydroxyls counted from the skeleton
		int hydroxyls = c.freeHydroxyls();
		weighDerivatized(c, MassOptions.PERMETHYLATED, expected + hydroxyls * CH2, hydroxyls, failures);
		weighDerivatized(c, MassOptions.PERACETYLATED, expected + hydroxyls * C2H2O, hydroxyls, failures);

		// 4. back to WURCS, against WURCSFramework's normal form of the input
		try {
			String normal = new WURCSFactory(c.wurcs).getWURCS();
			org.eurocarbdb.application.glycanbuilder.logutility.LogUtils.clearLastError();
			String written = new WURCS2Parser().writeGlycan(read);
			if (written.isEmpty())
				fail(failures, "not written to WURCS", c,
						String.valueOf(org.eurocarbdb.application.glycanbuilder.logutility.LogUtils.getLastError()));
			else if (!written.equals(normal))
				fail(failures, "WURCS written back differs", c, written + "  expected " + normal);
		} catch (Exception e) {
			fail(failures, "WURCSFramework cannot normalize it (oracle)", c, String.valueOf(e.getMessage()));
		}

		// 5. GWS and back
		try {
			String gws = read.toString();
			Glycan reopened = Glycan.fromString(gws);
			if (reopened == null)
				fail(failures, "GWS does not read back", c, gws);
			else {
				if (!gws.equals(reopened.toString()))
					fail(failures, "GWS does not come back as the same text", c, gws + "  ->  " + reopened);
				if (Math.abs(reopened.computeMass() - expected) > 1e-3)
					fail(failures, "mass changes across GWS", c,
							String.format("%.4f, expected %.4f", reopened.computeMass(), expected));
			}
		} catch (Exception e) {
			fail(failures, "GWS round trip throws", c, String.valueOf(e.getMessage()));
		}
	}

	private static void weighDerivatized(Case c, String derivatization, double expected, int hydroxyls,
			Map<String, List<String>> failures) {
		try {
			Glycan read = new WURCS2Parser().readGlycan(c.wurcs, new MassOptions());
			read.getMassOptions().setDerivatization(derivatization);
			double mass = read.computeMass();
			if (Math.abs(mass - expected) > 1e-3)
				fail(failures, derivatization + " mass differs from the hydroxyl count", c,
						String.format("%.4f, expected %.4f (%d OH)", mass, expected, hydroxyls));
		} catch (Exception e) {
			fail(failures, derivatization + " throws", c, String.valueOf(e.getMessage()));
		}
	}

	/** What the reader made of the case: each residue that is not the reducing end, with its deoxies. */
	private static String readAs(Glycan read) {
		StringBuilder made = new StringBuilder();
		for (Residue r : read.getAllResidues()) {
			if (r.isReducingEnd()) continue;
			if (made.length() > 0) made.append(' ');
			made.append(r.getTypeName());
			if (r.hasDeoxies()) made.append(r.getDeoxies());
		}
		return made.toString();
	}

	/** Every failure, not the first eight of each kind, for whoever has to read them. */
	private static void writeFullReport(Map<String, List<String>> failures, String heading) {
		try (java.io.PrintWriter out = new java.io.PrintWriter("target/deoxy-exhaustive-report.txt", "UTF-8")) {
			out.println(heading);
			for (Map.Entry<String, List<String>> e : failures.entrySet()) {
				out.println();
				out.println(e.getKey() + ": " + e.getValue().size());
				for (String one : e.getValue()) out.println("    " + one);
			}
		} catch (Exception cannotWrite) {
			// the summary is in the assertion message
		}
	}

	private static void fail(Map<String, List<String>> failures, String check, Case c, String detail) {
		String key = (c.deoxies.isEmpty() ? "[control] " : "") + check;
		List<String> list = failures.get(key);
		if (list == null) failures.put(key, list = new ArrayList<String>());
		list.add(causeOf(check, c, detail) + " || " + c + "  :  " + detail);
	}

	private static final String UNEXPLAINED = "unexplained";

	/**
	 * The known, older cause of a failure, or {@link #UNEXPLAINED}. Each is narrow on purpose - the
	 * check that failed, and the condition that makes it that cause - so that a new failure is not
	 * waved through by a cause that merely sounds like it.
	 */
	private static String causeOf(String check, Case c, String detail) {
		// A deoxy on a monosaccharide whose stereo is not fully known - the generic Pen, Hex - is a
		// skeleton such as adxxh, which the WURCS reader's converter refuses: #265.
		if (check.equals("does not read") && detail.contains("could not handled")
				&& c.base.skeleton.indexOf('x') >= 0)
			return "K1 #265 the reader refuses an unknown stereocentre";
		// The same refusal for a skeleton the deoxies have left with no stereocentre at all - addh,
		// a tetrose with both of its taken.
		if (check.equals("does not read") && detail.contains("could not handled") && !hasStereocentre(c.skeleton))
			return "K1 #265 the reader refuses an unknown stereocentre";

		// The reader names the residue by a type with no stereo recorded - eryPen, lyxHex, Rho - and
		// the writer cannot build its skeleton: #275, as #261 is for araHex.
		if (check.equals("not written to WURCS") && detail.contains("has no stereo recorded for it"))
			return "K2 #275 the type has no stereo recorded";

		// A deoxy ketose is named by its remaining stereo, an aldose's name - eryHex for a 3-deoxy
		// hexulose - with its anomeric carbon at 2. Nothing then marks carbon 2 as a carbonyl, and the
		// writer fails looking it up (ResidueAnalyzer.ResidueToSkeletonCode, the TreeMap.get at line 94):
		// the same failure #275 records for the keto-octoses. It failed the same way on 1.40.0.
		if (check.equals("not written to WURCS") && detail.contains("TreeMap.get") && c.base.anomericPosition == 2)
			return "K5 #275 a deoxy ketose loses its carbonyl";

		// The reader names the residue by a monosaccharide the dictionaries do not have - galHep,
		// altHep, xylHep, ribHep for a deoxy heptose, groTet for a tetrose with one stereocentre left -
		// so it is made by createUnknown: no composition, and not a saccharide.
		boolean weighing = check.startsWith("mass ") || check.startsWith(MassOptions.PERMETHYLATED)
				|| check.startsWith(MassOptions.PERACETYLATED);
		boolean writing = check.equals("not written to WURCS") && detail.contains("has neither a configuration nor a ring");
		if ((weighing || writing) && c.readAs != null && readsAsATypeNoDictionaryHas(c.readAs))
			return "K3 #282 the reader names a monosaccharide no dictionary has";

		// L-gro-D-manHep and D-gro-D-manHep record 5 methylation and 3 acetylation sites; a
		// heptopyranose has six hydroxyls (1, 2, 3, 4, 6, 7). Derivatized, they are wrong with or
		// without a deoxy.
		boolean derivatized = check.startsWith(MassOptions.PERMETHYLATED) || check.startsWith(MassOptions.PERACETYLATED);
		if (derivatized && (c.base.name.equals("L-gro-D-manHep") || c.base.name.equals("D-gro-D-manHep")))
			return "K4 #283 the heptose rows record too few derivatization sites";

		return UNEXPLAINED;
	}

	/**
	 * The known failures, counted exactly. Each moves when its cause is fixed, and then this is
	 * corrected deliberately - not to whatever the run produced.
	 */
	private static final Map<String, Integer> KNOWN_COUNTS = new java.util.TreeMap<String, Integer>();
	static {
		// 204 until Thr and Ery could be written (#285) and so became bases
		KNOWN_COUNTS.put("K1 #265 the reader refuses an unknown stereocentre", 216);
		// 1,404 before #261, when no configurational-prefix name could be written at all
		KNOWN_COUNTS.put("K2 #275 the type has no stereo recorded", 150);
		// 480, all heptoses, until the tetrose rows were corrected (#285): the deoxy tetroses then read
		// and fail the same way, as groTet - 90 cases on each of Thr, Ery, thrTet and eryTet
		KNOWN_COUNTS.put("K3 #282 the reader names a monosaccharide no dictionary has", 5 * 96 + 4 * 90);
		KNOWN_COUNTS.put("K4 #283 the heptose rows record too few derivatization sites", 2 * 24 + 2 * 24);
		KNOWN_COUNTS.put("K5 #275 a deoxy ketose loses its carbonyl", 216);
	}

	private static boolean hasStereocentre(String skeleton) {
		for (char carbon : skeleton.toCharArray())
			if ("1234x".indexOf(carbon) >= 0) return true;
		return false;
	}

	/** Whether any residue but the reducing end was read as a type neither dictionary holds. */
	private static boolean readsAsATypeNoDictionaryHas(Glycan read) {
		for (Residue r : read.getAllResidues()) {
			if (r.isReducingEnd()) continue;
			String name = r.getTypeName();
			if (!ResidueDictionary.hasResidueType(name) && !NonSymbolicResidueDictionary.hasResidueType(name))
				return true;
		}
		return false;
	}

	// ------------------------------------------------------------------------------------------------

	/** A neutral monosaccharide as this application writes it alone. */
	private static final class Base {
		final String name;
		final String skeleton;
		final int anomericPosition;
		final String ring;
		final int ringEnd;

		Base(String name, String skeleton, int anomericPosition, String ring, int ringEnd) {
			this.name = name;
			this.skeleton = skeleton;
			this.anomericPosition = anomericPosition;
			this.ring = ring;
			this.ringEnd = ringEnd;
		}
	}

	private static List<Base> bases() {
		List<ResidueType> types = new ArrayList<ResidueType>(ResidueDictionary.allResidues());
		types.addAll(NonSymbolicResidueDictionary.getResidues());

		Map<String, Base> byMs = new LinkedHashMap<String, Base>();
		for (ResidueType type : types) {
			if (!type.isSaccharide() || "Bridge".equals(type.getSuperclass())) continue;
			String written;
			try {
				Residue root = ResidueDictionary.createReducingEnd("freeEnd");
				root.addChild(ResidueDictionary.newResidue(type.getName()));
				written = new WURCS2Parser().writeGlycan(new Glycan(root, false, new MassOptions()));
			} catch (Exception cannotBeDrawn) {
				continue;
			}
			if (!written.startsWith("WURCS=2.0/1,1,0/[")) continue;

			String ms = written.substring(written.indexOf('[') + 1, written.indexOf(']'));
			String[] parts = ms.split("_");
			if (parts.length != 2 || parts[1].contains("*")) continue;        // a ring and nothing else
			int dash = parts[0].indexOf('-');
			if (dash < 0) continue;                                            // open chain
			String skeleton = parts[0].substring(0, dash);
			String anomer = parts[0].substring(dash + 1);
			if (!anomer.matches("[1-9][abx]")) continue;
			boolean neutral = true;
			for (char carbon : skeleton.toCharArray()) neutral &= NEUTRAL.indexOf(carbon) >= 0;
			if (!neutral) continue;
			String[] ring = parts[1].split("-");
			if (ring.length != 2 || !ring[1].matches("[0-9]+")) continue;

			byMs.put(skeleton + "-" + anomer.charAt(0) + "_" + parts[1], new Base(type.getName(), skeleton,
					anomer.charAt(0) - '0', parts[1], Integer.parseInt(ring[1])));
		}
		return new ArrayList<Base>(byMs.values());
	}

	/** Every carbon that can lose its oxygen, taken none, one and two at a time. */
	private static List<Set<Integer>> deoxyChoices(Base base) {
		List<Integer> candidates = new ArrayList<Integer>();
		int n = base.skeleton.length();
		for (int position = 2; position <= n; position++) {
			if (position == base.anomericPosition || position == base.ringEnd) continue;
			char carbon = base.skeleton.charAt(position - 1);
			boolean internal = position < n && "1234x".indexOf(carbon) >= 0;
			boolean terminal = position == n && carbon == 'h';
			if (internal || terminal) candidates.add(position);
		}

		List<Set<Integer>> choices = new ArrayList<Set<Integer>>();
		choices.add(new LinkedHashSet<Integer>());
		for (int i = 0; i < candidates.size(); i++) {
			Set<Integer> one = new LinkedHashSet<Integer>();
			one.add(candidates.get(i));
			choices.add(one);
			for (int j = i + 1; j < candidates.size(); j++) {
				Set<Integer> two = new LinkedHashSet<Integer>(one);
				two.add(candidates.get(j));
				choices.add(two);
			}
		}
		return choices;
	}

	/** Whether this carbon still carries a hydroxyl a methyl could replace. */
	private static boolean hasHydroxyl(Base base, Set<Integer> deoxies, int position) {
		if (position > base.skeleton.length() || deoxies.contains(position)) return false;
		if (position == base.anomericPosition || position == base.ringEnd) return false;
		return "1234x".indexOf(base.skeleton.charAt(position - 1)) >= 0;
	}
}
