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

import java.util.Map;

import org.eurocarbdb.MolecularFramework.sugar.GlycoEdge;
import org.eurocarbdb.MolecularFramework.sugar.Monosaccharide;
import org.eurocarbdb.MolecularFramework.sugar.Substituent;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.glycoinfo.GlycanFormatconverter.util.exchange.SugarToWURCSGraph.SugarToWURCSGraph;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;

/**
 * Writes a monosaccharide composition as composition WURCS.
 *
 * <p>A composition is a bag of residues with no linkages between them, which is exactly what is
 * built here: each counted residue becomes that many unconnected nodes in a {@code Sugar}. The
 * encoding is then the same one every other WURCS goes through -
 * {@link SugarToWURCSGraph} and {@link WURCSFactory} - and that matters more than it looks.
 *
 * <p><b>Nothing here decides the order residues appear in.</b> Composition WURCS is canonical, and
 * for Hex₅HexNAc₄Neu5Ac₂dHex₁ the canonical order is Neu5Ac, dHex, HexNAc, Hex - which is neither
 * alphabetical, nor by count, nor the order they were counted in. {@code WURCSFactory} normalizes,
 * and leaving it to do so is what keeps the output canonical: a WURCS that is correct but not
 * canonical looks right and fails to match anything in GlyTouCan, which is the worst of both.
 *
 * <p>This is the approach <a href="https://gitlab.com/glycoinfo/glycompconverter">glycompconverter</a>
 * takes, and this is a reimplementation of the part of it a composition needs, against libraries
 * GlycanBuilder2 already depends on.
 *
 * <p>Depends on no part of GlycanBuilder2. Along with {@link CompositionResidue} it is meant to be
 * liftable into a library of its own; whatever knows about {@code Glycan} belongs above it.
 */
public final class CompositionToWURCS {

	private CompositionToWURCS() {
	}

	/**
	 * @param counted How many of each residue, in any order. Zero and negative counts are ignored.
	 * @return Returns composition WURCS, or null when nothing was counted - a composition of nothing
	 *         is not a structure, and an empty string would be mistaken for one.
	 * @throws Exception If the counts do not make a structure the encoder accepts.
	 */
	public static String encode(Map<CompositionResidue, Integer> counted) throws Exception {
		Sugar composition = asSugar(counted);
		if (composition.getNodes().isEmpty()) return null;

		SugarToWURCSGraph toGraph = new SugarToWURCSGraph();
		toGraph.start(composition);

		return new WURCSFactory(toGraph.getGraph()).getWURCS();
	}

	/**
	 * The composition as a MolecularFramework structure: every residue a node, and no edges between
	 * them.
	 *
	 * <p>The absence of edges is the composition. A residue's own substituents are attached to it -
	 * an N-acetyl is part of being a HexNAc - but nothing joins one residue to another, because a
	 * composition does not say what joins them.
	 *
	 * @param counted How many of each residue.
	 * @return Returns the structure.
	 * @throws Exception If the parts do not make one.
	 */
	static Sugar asSugar(Map<CompositionResidue, Integer> counted) throws Exception {
		Sugar composition = new Sugar();

		for (Map.Entry<CompositionResidue, Integer> each : counted.entrySet()) {
			CompositionResidue residue = each.getKey();
			int howMany = (each.getValue() == null) ? 0 : each.getValue();

			for (int made = 0; made < howMany; made++) {
				Monosaccharide sugar = residue.asMonosaccharide();
				composition.addNode(sugar);

				for (CompositionResidue.CoreSubstituent carried : residue.coreSubstituents()) {
					Substituent substituent = carried.asSubstituent();
					GlycoEdge edge = carried.asEdge();
					composition.addNode(sugar, edge, substituent);
				}
			}
		}

		return composition;
	}
}
