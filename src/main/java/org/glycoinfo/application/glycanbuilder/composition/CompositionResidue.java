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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eurocarbdb.MolecularFramework.sugar.Anomer;
import org.eurocarbdb.MolecularFramework.sugar.BaseType;
import org.eurocarbdb.MolecularFramework.sugar.GlycoEdge;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.Linkage;
import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.MolecularFramework.sugar.Modification;
import org.eurocarbdb.MolecularFramework.sugar.ModificationType;
import org.eurocarbdb.MolecularFramework.sugar.Monosaccharide;
import org.eurocarbdb.MolecularFramework.sugar.Substituent;
import org.eurocarbdb.MolecularFramework.sugar.SubstituentType;
import org.eurocarbdb.MolecularFramework.sugar.Superclass;

/**
 * A residue a composition can be counted in, and what it is made of.
 *
 * <p>A composition names its members by class rather than by identity - "three hexoses", not "three
 * of this particular hexose" - so what is described here is the class: how many carbons, where the
 * anomeric centre and the ring are, and which modifications and substituents are part of being that
 * kind of sugar at all. Stereochemistry is deliberately absent, which is the whole point: a Hex is
 * every hexose at once.
 *
 * <p>The figures come from the generic monosaccharide table in
 * <a href="https://gitlab.com/glycoinfo/glycompconverter">glycompconverter</a>, which is where this
 * definition of a composition is maintained. They are written out here rather than read from a file
 * so that nothing has to be loaded before a composition can be built: this project has twice shipped
 * a fault whose cause was a dictionary that had not been read yet, and a constant cannot be empty at
 * the wrong moment.
 *
 * <p>Carries no reference to GlycanBuilder2. Along with {@link CompositionToWURCS} it is meant to be
 * liftable into a library of its own, so anything that knows about {@code Glycan} belongs in the
 * adapter above it, not here.
 */
public enum CompositionResidue {

	/** Pentose. */
	PEN("Pen", Superclass.PEN, 1, 6),

	/** Hexose. */
	HEX("Hex", Superclass.HEX, 1, 6),

	/** Hexosamine. */
	HEXN("HexN", Superclass.HEX, 1, 6, mods(), substs(amino(2))),

	/** N-acetyl hexosamine. */
	HEXNAC("HexNAc", Superclass.HEX, 1, 6, mods(), substs(nAcetyl(2))),

	/** Hexuronic acid. */
	HEXA("HexA", Superclass.HEX, 1, 6, mods(acid(6)), substs()),

	/** 6-deoxy hexose. */
	DHEX("dHex", Superclass.HEX, 1, 6, mods(deoxy(6)), substs()),

	/** Di-deoxy hexose - one deoxy at an unknown position besides the 6. */
	DDHEX("ddHex", Superclass.HEX, 1, 6, mods(deoxy(2), deoxy(6)), substs()),

	/** Heptose. */
	HEP("Hep", Superclass.HEP, 1, 6),

	/** KDO. */
	KDO("Kdo", Superclass.OCT, 2, 6, mods(acid(1), deoxy(3)), substs(), stereo(BaseType.DMAN)),

	/** KDN, a deoxynonulosonate. */
	KDN("Kdn", Superclass.NON, 2, 6, mods(acid(1), deoxy(3)), substs(), stereo(BaseType.DGRO, BaseType.DGAL)),

	/** N-acetyl neuraminic acid. */
	NEU5AC("Neu5Ac", Superclass.NON, 2, 6, mods(acid(1), deoxy(3)), substs(nAcetyl(5)), stereo(BaseType.DGRO, BaseType.DGAL)),

	/** N-glycolyl neuraminic acid. */
	NEU5GC("Neu5Gc", Superclass.NON, 2, 6, mods(acid(1), deoxy(3)), substs(nGlycolyl(5)), stereo(BaseType.DGRO, BaseType.DGAL)),

	/** Muramic acid. */
	MUR("Mur", Superclass.HEX, 1, 6, mods(), substs(amino(2), carboxyethyl(3)), stereo(BaseType.DGLC)),

	/** N-acetyl muramic acid. */
	MURNAC("MurNAc", Superclass.HEX, 1, 6, mods(), substs(nAcetyl(2), carboxyethyl(3)), stereo(BaseType.DGLC));

	private final String name;
	private final Superclass superclass;
	private final int anomericPosition;
	private final int ringSize;
	private final List<CoreModification> modifications;
	private final List<CoreSubstituent> substituents;
	private final List<BaseType> baseTypes;

	CompositionResidue(String name, Superclass superclass, int anomericPosition, int ringSize) {
		this(name, superclass, anomericPosition, ringSize, mods(), substs(), stereo());
	}

	CompositionResidue(String name, Superclass superclass, int anomericPosition, int ringSize,
			List<CoreModification> modifications, List<CoreSubstituent> substituents) {
		this(name, superclass, anomericPosition, ringSize, modifications, substituents, stereo());
	}

	CompositionResidue(String name, Superclass superclass, int anomericPosition, int ringSize,
			List<CoreModification> modifications, List<CoreSubstituent> substituents,
			List<BaseType> baseTypes) {
		this.baseTypes = baseTypes;
		this.name = name;
		this.superclass = superclass;
		this.anomericPosition = anomericPosition;
		this.ringSize = ringSize;
		this.modifications = modifications;
		this.substituents = substituents;
	}

	/** @return Returns the name a composition counts this by, e.g. "HexNAc". */
	public String residueName() {
		return name;
	}

	/**
	 * @param name A residue name, matched without regard to case.
	 * @return Returns the residue of that name, or null when a composition cannot be counted in it.
	 */
	public static CompositionResidue named(String name) {
		if (name == null) return null;
		for (CompositionResidue residue : values())
			if (residue.name.equalsIgnoreCase(name)) return residue;

		return null;
	}

	/**
	 * Builds one of these as a MolecularFramework monosaccharide.
	 *
	 * <p>The anomer is unknown and no stereochemistry is set, which is what makes it the class rather
	 * than a member of it. A ketose - anything whose anomeric centre is not carbon 1 - carries the
	 * keto modification there, as it would in any other encoding of the same sugar.
	 *
	 * @return Returns a new monosaccharide each call; the caller adds as many as were counted.
	 * @throws GlycoconjugateException If the parts do not make a monosaccharide.
	 */
	Monosaccharide asMonosaccharide() throws GlycoconjugateException {
		Monosaccharide sugar = new Monosaccharide(Anomer.Unknown, superclass);
		for (BaseType stereo : baseTypes) sugar.addBaseType(stereo);
		sugar.setRing(anomericPosition, anomericPosition + ringSize - 2);

		if (anomericPosition != 1) {
			sugar.addModification(new Modification(ModificationType.KETO, anomericPosition));
		}
		for (CoreModification modification : modifications) {
			sugar.addModification(new Modification(modification.type, modification.position));
		}

		return sugar;
	}

	/** @return Returns the substituents that are part of being this kind of sugar. */
	List<CoreSubstituent> coreSubstituents() {
		return substituents;
	}

	/**
	 * A modification the residue carries by definition, held as what it is rather than as a built
	 * {@code Modification} - building one throws, and an enum constant cannot.
	 */
	static final class CoreModification {
		private final ModificationType type;
		private final int position;

		CoreModification(ModificationType type, int position) {
			this.type = type;
			this.position = position;
		}
	}

	/** A substituent the residue carries by definition, and where it sits. */
	static final class CoreSubstituent {
		private final SubstituentType type;
		private final LinkageType linkage;
		private final int position;

		CoreSubstituent(SubstituentType type, LinkageType linkage, int position) {
			this.type = type;
			this.linkage = linkage;
			this.position = position;
		}

		Substituent asSubstituent() throws GlycoconjugateException {
			return new Substituent(type);
		}

		GlycoEdge asEdge() throws GlycoconjugateException {
			Linkage bond = new Linkage();
			bond.setParentLinkageType(linkage);
			bond.setChildLinkageType(LinkageType.NONMONOSACCHARID);
			bond.addParentLinkage(position);
			bond.addChildLinkage(1);

			GlycoEdge edge = new GlycoEdge();
			edge.addGlycosidicLinkage(bond);

			return edge;
		}
	}

	// The table above reads better with these than with constructors spelled out in full.

	/**
	 * The stereochemistry a residue has, for the few that are a named sugar rather than a class.
	 *
	 * <p>Most composition residues have none, which is what makes them classes - a Hex is every
	 * hexose. Neu5Ac and Kdo are not classes: they name one sugar, and leaving their configuration
	 * out writes a WURCS for a vaguer thing than the person meant.
	 */
	private static List<BaseType> stereo(BaseType... each) {
		return new ArrayList<BaseType>(Arrays.asList(each));
	}

	private static List<CoreModification> mods(CoreModification... each) {
		return new ArrayList<CoreModification>(Arrays.asList(each));
	}

	private static List<CoreSubstituent> substs(CoreSubstituent... each) {
		return new ArrayList<CoreSubstituent>(Arrays.asList(each));
	}

	private static CoreModification deoxy(int position) {
		return new CoreModification(ModificationType.DEOXY, position);
	}

	private static CoreModification acid(int position) {
		return new CoreModification(ModificationType.ACID, position);
	}

	/** An amino group replaces a hydrogen, so it is linked deoxy rather than through an oxygen. */
	private static CoreSubstituent amino(int position) {
		return new CoreSubstituent(SubstituentType.AMINO, LinkageType.DEOXY, position);
	}

	private static CoreSubstituent nAcetyl(int position) {
		return new CoreSubstituent(SubstituentType.N_ACETYL, LinkageType.DEOXY, position);
	}

	private static CoreSubstituent nGlycolyl(int position) {
		return new CoreSubstituent(SubstituentType.N_GLYCOLYL, LinkageType.DEOXY, position);
	}

	/** Muramic acid's lactyl ether, which hangs off an oxygen. */
	private static CoreSubstituent carboxyethyl(int position) {
		return new CoreSubstituent(SubstituentType.R_CARBOXYETHYL, LinkageType.H_AT_OH, position);
	}
}
