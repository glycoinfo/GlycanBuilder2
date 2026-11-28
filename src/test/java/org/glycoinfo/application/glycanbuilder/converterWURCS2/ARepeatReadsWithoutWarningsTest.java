package org.glycoinfo.application.glycanbuilder.converterWURCS2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * G67119YN is read without WURCSFramework's warning about it, 24 times over (#228).
 *
 * <p>"Leaf edge is not connected to any sequence" is logged while a WURCS is ordered canonically,
 * for a repeat whose closing bond has a position not known. G67119YN is read whole and the validator
 * finds nothing wrong with it, so the warning told its reader nothing, and logback's default
 * configuration gave no way to stop it.
 */
public class ARepeatReadsWithoutWarningsTest {

	/** G67119YN, an N-glycan with two polysialic acids and a polyfucose, as #228 gives it. */
	private static final String G67119YN = "WURCS=2.0/5,13,15/[a2122h-1x_1-5_2*NCC/3=O][a1122h-1x_1-5]"
			+ "[a2112h-1x_1-5][Aad21122h-2x_2-6_5*NCC/3=O][a1221m-1x_1-5]/1-1-1-2-2-1-3-2-1-3-4-4-5/"
			+ "a?-b1_b?-c1_c?-d1_d?-e1_d?-h1_e?-f1_f?-g1_h?-i1_i?-j1_a?-m?_g?-k?_j?-l?_k2-k?~n_l2-l?~n_m1-m?~n";

	private static Logger comparator() {
		return (Logger) org.slf4j.LoggerFactory.getLogger(QuietWURCSFrameworkLogs.COMPARATOR);
	}

	/** Read and written back, it logs nothing at WARN. */
	@Test
	public void itIsReadAndWrittenWithoutTheWarning() throws Exception {
		new BuilderWorkspace(new GlycanRendererAWT()).setAutoSave(false);
		ListAppender<ILoggingEvent> heard = new ListAppender<ILoggingEvent>();
		heard.start();
		comparator().addAppender(heard);
		try {
			WURCS2Parser parser = new WURCS2Parser();
			Glycan read = parser.readGlycan(G67119YN, new MassOptions());
			parser.toWURCS(read);
		} finally {
			comparator().detachAppender(heard);
		}

		List<String> warned = new ArrayList<String>();
		for (ILoggingEvent event : heard.list)
			if (event.getLevel().isGreaterOrEqual(Level.WARN)) warned.add(event.getFormattedMessage());
		assertTrue("warned: " + warned, warned.isEmpty());
	}

	/** A level the program set for that logger itself is kept. */
	@Test
	public void aLevelSetByTheProgramIsKept() {
		Logger logger = comparator();
		Level before = logger.getLevel();
		try {
			logger.setLevel(Level.DEBUG);
			QuietWURCSFrameworkLogs.quiet();
			assertEquals(Level.DEBUG, logger.getLevel());

			logger.setLevel(null);
			QuietWURCSFrameworkLogs.quiet();
			assertEquals(Level.ERROR, logger.getLevel());
		} finally {
			logger.setLevel(before);
		}
	}
}
