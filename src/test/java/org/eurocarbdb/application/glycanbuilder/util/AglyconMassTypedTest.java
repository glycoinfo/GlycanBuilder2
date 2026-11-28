package org.eurocarbdb.application.glycanbuilder.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The mass typed for an aglycon in the Mass dialog: a number, or one not known - Ser/Thr is one of
 * two masses, and was given one of them or refused with a NumberFormatException (#305).
 */
public class AglyconMassTypedTest {

	@Test
	public void emptyOrAQuestionMarkIsAMassNotKnown() {
		assertTrue(Double.isNaN(MassOptionsStructureDialog.aglyconMass("")));
		assertTrue(Double.isNaN(MassOptionsStructureDialog.aglyconMass("  ")));
		assertTrue(Double.isNaN(MassOptionsStructureDialog.aglyconMass("?")));
		assertTrue(Double.isNaN(MassOptionsStructureDialog.aglyconMass(null)));
	}

	@Test
	public void aNumberIsTheMass() {
		assertEquals(87.032, MassOptionsStructureDialog.aglyconMass("87.032"), 0.);
		assertEquals(0., MassOptionsStructureDialog.aglyconMass("0"), 0.);
	}

	@Test
	public void anythingElseIsRefused() {
		assertNull(MassOptionsStructureDialog.aglyconMass("87,032"));
		assertNull(MassOptionsStructureDialog.aglyconMass("Ser"));
		assertNull(MassOptionsStructureDialog.aglyconMass("NaN"));
		assertNull(MassOptionsStructureDialog.aglyconMass("Infinity"));
	}
}
