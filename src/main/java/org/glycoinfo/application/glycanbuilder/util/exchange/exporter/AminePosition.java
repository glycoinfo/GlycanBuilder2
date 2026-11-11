package org.glycoinfo.application.glycanbuilder.util.exchange.exporter;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.glycoinfo.WURCSFramework.util.exchange.WURCSExchangeException;
import org.glycoinfo.application.glycanbuilder.dataset.NativeMonosaccharideDictionary;

/**
 * The carbon a group at position {@code N} is written on.
 *
 * <p>An amino sugar offers its nitrogen as a position of its own, {@code N}: GlcN, GalN, Neu and the
 * like list it among their linkage positions, and a sulfate drawn there is the N-sulfate of heparin's
 * GlcNS. WURCS has no position N. A group on the nitrogen is written on the carbon that carries it,
 * as part of the amine's MAP - {@code 2*NSO/3=O/3=O} - which is how the same sulfate drawn at 2 was
 * already written. Drawn at N it was not written at all: the exporter took N for a number and failed
 * (#222, #189). Position N is that carbon, then: 2 for GlcN, 5 for Neu.
 */
final class AminePosition {

	private AminePosition() {}

	/** The position as the exporter reads it: N becomes the amine's carbon, anything else is itself. */
	static char of(Residue _residue, char _position) throws WURCSExchangeException {
		if (_position != 'N') return _position;
		char carbon = carbonOf(_residue);
		if (carbon == '\0')
			throw new WURCSExchangeException(name(_residue) + " has a group on its nitrogen, position N, but "
					+ "which carbon carries that nitrogen is not recorded for it, so the group has no carbon to be written on.");
		return carbon;
	}

	/** As {@link #of}, leaving N as it is where the amine's carbon is not known. */
	static char orAsItIs(Residue _residue, char _position) {
		if (_position != 'N') return _position;
		char carbon = carbonOf(_residue);
		return (carbon == '\0') ? _position : carbon;
	}

	private static char carbonOf(Residue _residue) {
		if (_residue == null || _residue.getType() == null) return '\0';
		NativeMonosaccharideDictionary.Entry entry = NativeMonosaccharideDictionary.forResidueName(_residue.getTypeName());
		return (entry == null) ? '\0' : entry.getAminePosition();
	}

	private static String name(Residue _residue) {
		return (_residue == null) ? "A residue" : _residue.getTypeName();
	}
}
