/*
*   EuroCarbDB, a framework for carbohydrate bioinformatics
*
*   This copyrighted material is made available to anyone wishing to use, modify,
*   copy, or redistribute it subject to the terms and conditions of the GNU
*   Lesser General Public License, as published by the Free Software Foundation.
*   A copy of this license accompanies this distribution in the file LICENSE.txt.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
*   or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
*   for more details.
*/

package org.glycoinfo.application.glycanbuilder.composition;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;

/**
 * Reads a composition drawn in GlycanBuilder2 and writes it as composition WURCS.
 *
 * <p>The whole of what this class knows about GlycanBuilder2 is here: how to walk a {@link Glycan}
 * whose {@code isComposition()} is true and count what it holds.
 * {@link CompositionToWURCS} and {@link CompositionResidue} know nothing of it, which is what would
 * let those two be lifted into a library of their own without bringing the builder with them.
 */
public final class GlycanComposition {

	private GlycanComposition() {
	}

	/**
	 * Thrown when a composition holds a residue that a composition cannot be written in.
	 *
	 * <p>Named rather than swallowed: dropping the residue would write a WURCS for a different
	 * composition than the one on the screen, which is worse than not writing one.
	 */
	@SuppressWarnings("serial")
	public static class CannotBeWrittenException extends Exception {

		/** @param residueName The residue that has no place in a composition. */
		public CannotBeWrittenException(String residueName) {
			super("A composition cannot be written in '" + residueName + "'");
		}
	}

	/**
	 * @param composition A structure whose {@code isComposition()} is true.
	 * @return Returns composition WURCS, or null when the composition is empty.
	 * @throws CannotBeWrittenException If it holds a residue a composition cannot be written in.
	 * @throws Exception If the encoding itself fails.
	 */
	public static String toWURCS(Glycan composition) throws Exception {
		return CompositionToWURCS.encode(count(composition));
	}

	/**
	 * @param composition The structure to read.
	 * @return Returns how many of each residue it holds.
	 * @throws CannotBeWrittenException If it holds a residue a composition cannot be written in.
	 */
	static Map<CompositionResidue, Integer> count(Glycan composition)
			throws CannotBeWrittenException {
		Map<CompositionResidue, Integer> counted = new LinkedHashMap<CompositionResidue, Integer>();
		if (composition == null) return counted;

		for (Residue residue : composition.getAllResidues()) {
			if (residue.getType() == null) continue;
			// The reducing end and the bracket hold the composition rather than belong to it.
			if (residue.isReducingEnd() || residue.isBracket()) continue;

			String name = residue.getType().getName();
			CompositionResidue counts = CompositionResidue.named(name);
			if (counts == null) throw new CannotBeWrittenException(name);

			Integer already = counted.get(counts);
			counted.put(counts, (already == null) ? 1 : already + 1);
		}

		return counted;
	}
}
