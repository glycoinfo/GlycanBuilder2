package org.eurocarbdb.application.glycanbuilder.test;

import org.eurocarbdb.application.glycanbuilder.massutil.MassUtils;
import org.junit.Assert;
import org.junit.Test;


public class MassTest {
	@Test
	public void testIsotopeMassCalculation(){
		Assert.assertEquals(15.023475096, MassUtils.methyl.getMass(), 0);
		Assert.assertEquals(16.026829934, MassUtils.heavyMethyl.getMass(), 0);
	}
}
