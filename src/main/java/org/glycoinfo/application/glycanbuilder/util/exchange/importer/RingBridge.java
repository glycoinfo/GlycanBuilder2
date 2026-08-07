package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.glycoinfo.WURCSFramework.wurcs.sequence2.BRIDGE;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.MSCORE;

/**
 * Tells a monosaccharide's ring apart from the bridges written beside it.
 *
 * <p>Both are written the same way - two positions and no MAP - and both start at the anomeric
 * carbon when the bridge is a 1,6-anhydro, so position alone does not separate them. What does is
 * the span: a ring closes four or five carbons along, on the carbon that makes it a furanose or a
 * pyranose, and anything reaching further is a bridge.</p>
 */
class RingBridge {

	/**
	 * The bridge that closes the ring.
	 * @param _core Core structure of the monosaccharide.
	 * @return Returns the ring, or null when the residue is written without one.
	 */
	static BRIDGE find(MSCORE _core) {
		int anomericPosition = _core.getAnomericPosition();
		if(anomericPosition < 1) return null;

		for(BRIDGE bridge : _core.getDivalentSubstituents()) {
			if(!bridge.getMAP().equals("")) continue;
			if(!bridge.getStartPositions().contains(anomericPosition)) continue;
			if(bridge.getEndPositions().isEmpty()) continue;

			int span = span(bridge, anomericPosition);
			if(span == 4 || span == 5) return bridge;
		}

		return null;
	}

	/** How many carbons the bridge covers, counted from the anomeric carbon. */
	static int span(BRIDGE _bridge, int _anomericPosition) {
		return _bridge.getEndPositions().get(0) - _anomericPosition + 1;
	}
}
