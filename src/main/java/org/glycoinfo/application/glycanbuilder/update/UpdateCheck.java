package org.glycoinfo.application.glycanbuilder.update;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Asks GitHub what the newest release is, and works out whether it is newer than this one.
 *
 * <p>Nothing here runs on its own. It is asked from Help ▸ Check for Updates, so the application
 * makes no outbound call unless a user asks it to - which keeps startup untouched, and leaves the
 * question of whether to talk to GitHub with the person in front of the machine.
 *
 * <p><b>Why the redirect rather than the API.</b> {@code api.github.com} allows 60 unauthenticated
 * requests an hour <em>per address</em> - measured - and a research institute behind one NAT is one
 * address. {@code github.com/.../releases/latest} answers 302 to the tag's own page, gives the same
 * answer, and is not rate-limited that way:
 *
 * <pre>
 * GET https://github.com/glycoinfo/GlycanBuilder2/releases/latest
 * → 302  Location: https://github.com/glycoinfo/GlycanBuilder2/releases/tag/v1.34.3
 * </pre>
 *
 * <p>It also excludes pre-releases, which fits how this project releases: the workflow publishes as
 * a pre-release and a person marks it Latest once the installers are built and checked. Until then
 * no one is pointed at a release that is not finished.
 */
public final class UpdateCheck {

	/** Where the newest finished release is announced. */
	private static final String LATEST_RELEASE = "https://github.com/glycoinfo/GlycanBuilder2/releases/latest";

	/** Short: a person is waiting, and no answer is better than a long wait. */
	private static final int TIMEOUT_MS = 8000;

	private UpdateCheck() {
	}

	/** What the check found. */
	public static final class Result {
		/** The version running now, or {@link ApplicationVersion#UNKNOWN}. */
		public final String installed;

		/** The newest released version, or null when it could not be found out. */
		public final String latest;

		/** Where a person on this platform should go to get it. */
		public final String downloadPage;

		/** Why the check could not answer, or null when it did. */
		public final String problem;

		Result(String installed, String latest, String downloadPage, String problem) {
			this.installed = installed;
			this.latest = latest;
			this.downloadPage = downloadPage;
			this.problem = problem;
		}

		/** @return Whether a newer release exists. False when either version is unknown. */
		public boolean anUpdateIsAvailable() {
			return latest != null && isNewer(latest, installed);
		}
	}

	/**
	 * Asks GitHub. Blocks, so call it off the event thread.
	 *
	 * @return Returns what was found, including when nothing was: a check that cannot reach GitHub
	 *         reports that rather than claiming the application is up to date.
	 */
	public static Result run() {
		String installed = ApplicationVersion.get();
		String downloadPage = DownloadPage.forThisPlatform();

		try {
			String latest = fetchLatestVersion();
			if (latest == null) {
				return new Result(installed, null, downloadPage,
						"GitHub did not say which release is the newest.");
			}

			return new Result(installed, latest, downloadPage, null);
		} catch (IOException couldNotAsk) {
			return new Result(installed, null, downloadPage,
					"Could not reach GitHub (" + couldNotAsk.getMessage() + ").");
		}
	}

	/**
	 * @return Returns the newest released version without its leading "v", or null if the answer
	 *         cannot be read.
	 */
	static String fetchLatestVersion() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE).openConnection();
		try {
			// The answer wanted is the redirect itself, so it must not be followed.
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("HEAD");
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			connection.setRequestProperty("User-Agent", "GlycanBuilder2/" + ApplicationVersion.get());

			int status = connection.getResponseCode();
			if (status != HttpURLConnection.HTTP_MOVED_TEMP
					&& status != HttpURLConnection.HTTP_MOVED_PERM
					&& status != 307 && status != 308) {
				return null;
			}

			return versionFromTagUrl(connection.getHeaderField("Location"));
		} finally {
			connection.disconnect();
		}
	}

	/** {@code .../releases/tag/v1.34.3} → {@code 1.34.3}. */
	static String versionFromTagUrl(String location) {
		if (location == null) return null;

		int lastSlash = location.lastIndexOf('/');
		if (lastSlash < 0 || lastSlash == location.length() - 1) return null;

		String tag = location.substring(lastSlash + 1).trim();
		if (tag.startsWith("v") || tag.startsWith("V")) tag = tag.substring(1);

		return tag.isEmpty() ? null : tag;
	}

	/**
	 * Whether {@code candidate} is a later version than {@code installed}.
	 *
	 * <p>Compared part by numeric part, so 1.34.10 is later than 1.34.9 where a string comparison
	 * would say otherwise. A part that is not a number, or a version that is not known at all, makes
	 * the answer false: saying nothing is better than announcing an update that may not be one.
	 */
	static boolean isNewer(String candidate, String installed) {
		if (candidate == null || installed == null
				|| ApplicationVersion.UNKNOWN.equals(installed)) {
			return false;
		}

		String[] theirs = candidate.split("\\.");
		String[] ours = installed.split("\\.");
		int parts = Math.max(theirs.length, ours.length);
		for (int i = 0; i < parts; i++) {
			int their = partAt(theirs, i);
			int our = partAt(ours, i);
			if (their < 0 || our < 0) return false;
			if (their != our) return their > our;
		}

		return false;
	}

	private static int partAt(String[] parts, int index) {
		if (index >= parts.length) return 0;

		try {
			return Integer.parseInt(parts[index].trim());
		} catch (NumberFormatException notANumber) {
			return -1;
		}
	}

	/**
	 * Where a person should go for the newest build, which is not the same place on every platform.
	 */
	public static final class DownloadPage {

		/** Windows is published through the Microsoft Store, and nowhere else. */
		public static final String WINDOWS_STORE = "https://apps.microsoft.com/detail/9pp6bsnx71jl";

		/** macOS builds are offered from the project's own download page. */
		public static final String MACOS = "https://glycanbuilder.glyconavi.org/en#download";

		/** Linux packages are the release's own assets - the .deb and the two .rpm. */
		public static final String RELEASES = "https://github.com/glycoinfo/GlycanBuilder2/releases/latest";

		private DownloadPage() {
		}

		/**
		 * @return Returns the page to open on this platform.
		 *
		 * <p>Windows goes to the Store because that is the only way it is published - the release
		 * carries no Windows installer at all, the workflow excludes it deliberately, and sending a
		 * Store user anywhere else would be wrong twice over. Anything unrecognised - a jar run
		 * directly, a build from source - gets the releases page, which lists what there is.
		 */
		public static String forThisPlatform() {
			String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
			if (os.contains("win")) return WINDOWS_STORE;
			if (os.contains("mac") || os.contains("darwin")) return MACOS;

			return RELEASES;
		}
	}
}
