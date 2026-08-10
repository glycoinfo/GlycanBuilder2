package org.glycoinfo.WURCSFramework.Glycan;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Configuration;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * That starting up writes the configuration where the user is allowed to write (#10).
 *
 * <p>The configuration was saved to the path it had been <i>looked for</i> at, which is normally a
 * bundled resource - so the write landed wherever that name resolved on disk: the working directory
 * of whatever launched the application. Started from the Windows start menu that is
 * {@code C:\WINDOWS\system32}, the write is denied, and the reporter's application did not start at
 * all: "java.io.FileNotFoundException: C:\WINDOWS\system32\config.xml (Access is denied)".</p>
 *
 * <p>{@code user.home} is redirected to a temporary directory for these tests, so they exercise the
 * real path-choosing code without touching the developer's own configuration.</p>
 */
public class ConfigurationLocationTest {

	private String realHome;
	private Path temporaryHome;

	@Before
	public void aHomeOfItsOwn() throws Exception {
		realHome = System.getProperty("user.home");
		temporaryHome = Files.createTempDirectory("glycanbuilder-home");
		System.setProperty("user.home", temporaryHome.toString());
	}

	@After
	public void putTheHomeBack() {
		System.setProperty("user.home", realHome);
	}

	/**
	 * The name that has to be asked for to reach the failing path: neither a file on disk nor a
	 * bundled resource, so opening fails and the workspace creates a configuration instead.
	 */
	private static final String NOT_THERE = "/no-such-glycanbuilder-config.xml";

	/** Starting up with nothing to read writes into the user's own directory, and starts. */
	@Test
	public void theConfigurationIsWrittenUnderTheUsersHome() {
		new BuilderWorkspace(NOT_THERE, true, new GlycanRendererAWT());

		assertTrue("nothing was written under the user's home",
				new File(BuilderWorkspace.getPersistentConfigFile()).isFile());
	}

	/**
	 * And nothing is written at the name it looked for.
	 *
	 * <p>This is the failure itself: the name the configuration is <i>read</i> from is a resource,
	 * and writing to it means writing to whatever that name resolves to on disk - the root of the
	 * drive here, {@code C:\WINDOWS\system32} for the reporter, neither of which an ordinary user
	 * may write to.</p>
	 */
	@Test
	public void nothingIsWrittenWhereTheResourceWasLookedFor() {
		new BuilderWorkspace(NOT_THERE, true, new GlycanRendererAWT());

		assertFalse("the resource path was written to as if it were a file",
				new File(NOT_THERE).isFile());
	}

	/** A configuration directory that does not exist yet is created rather than refused. */
	@Test
	public void savingCreatesTheDirectoryItNeeds() {
		File wanted = temporaryHome.resolve("not/made/yet/config.xml").toFile();

		assertTrue("the save reported failure", new Configuration().save(wanted.getPath()));
		assertTrue("no file appeared at " + wanted, wanted.isFile());
	}

	/**
	 * Asked for a configuration that is neither a file nor a bundled resource, opening says no.
	 *
	 * <p>It used to fall back to {@code src/main/resources/config.xml} - a path inside the build
	 * tree, absent from every distributed jar - so a first run reported a FileNotFoundException
	 * where it should simply have started with the defaults.</p>
	 */
	@Test
	public void openingSomethingThatIsNotThereIsAnOrderlyNo() {
		assertFalse(new Configuration().open(
				temporaryHome.resolve("no-such-config.xml").toString()));
	}
}
