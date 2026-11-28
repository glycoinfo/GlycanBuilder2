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

import org.eurocarbdb.MolecularFramework.sugar.GlycoEdge;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.Linkage;
import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.MolecularFramework.sugar.Substituent;
import org.eurocarbdb.MolecularFramework.sugar.SubstituentType;
import org.eurocarbdb.MolecularFramework.sugar.UnderdeterminedSubTree;

/**
 * A substituent a composition can count on its own: "two sulfates", on residues and at positions it
 * does not say (#220).
 *
 * <p>These are the substituents
 * <a href="https://gitlab.com/glycoinfo/glycompconverter">glycompconverter</a> counts, each on an
 * oxygen, and written its way: a substituent whose parent may be any monosaccharide of the
 * composition, at a position not known. Pyruvate and phosphocholine are not among them, so a
 * composition counting either is refused, as it was, rather than written in a form GlyTouCan does not
 * hold. glycompconverter's methyl is left out too, since the composition dialog does not offer one.
 *
 * <p>Like {@link CompositionResidue}, carries no reference to GlycanBuilder2.
 */
public enum CompositionSubstituent {

	/** Sulfate. */
	S("S", SubstituentType.SULFATE),

	/** Phosphate. */
	P("P", SubstituentType.PHOSPHATE),

	/** O-acetyl. */
	AC("Ac", SubstituentType.ACETYL);

	private final String name;
	private final SubstituentType type;

	CompositionSubstituent(String name, SubstituentType type) {
		this.name = name;
		this.type = type;
	}

	/** @return Returns the name a composition counts this by, e.g. "S". */
	public String substituentName() {
		return name;
	}

	/**
	 * @param name A residue type name, matched exactly: "S", "P" and "Ac" are short enough that
	 *            matching without regard to case would take names that mean something else.
	 * @return Returns the substituent of that name, or null when a composition cannot count it.
	 */
	public static CompositionSubstituent named(String name) {
		for (CompositionSubstituent substituent : values())
			if (substituent.name.equals(name)) return substituent;
		return null;
	}

	/**
	 * @return Returns one of these, joined through an oxygen at a position not known to a parent not
	 *         yet named; the caller names every monosaccharide of the composition as a possible parent.
	 * @throws GlycoconjugateException If the parts do not make one.
	 */
	UnderdeterminedSubTree asSubTree() throws GlycoconjugateException {
		Linkage bond = new Linkage();
		bond.setParentLinkageType(LinkageType.H_AT_OH);
		bond.setChildLinkageType(LinkageType.NONMONOSACCHARID);
		bond.addParentLinkage(Linkage.UNKNOWN_POSITION);
		bond.addChildLinkage(1);

		GlycoEdge edge = new GlycoEdge();
		edge.addGlycosidicLinkage(bond);

		UnderdeterminedSubTree subTree = new UnderdeterminedSubTree();
		subTree.setConnection(edge);
		subTree.addNode(new Substituent(type));

		return subTree;
	}
}
