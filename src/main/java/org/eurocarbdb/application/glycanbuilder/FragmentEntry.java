/*
*   EuroCarbDB, a framework for carbohydrate bioinformatics
*
*   Copyright (c) 2006-2009, Eurocarb project, or third-party contributors as
*   indicated by the @author tags or express copyright attribution
*   statements applied by the authors.  
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
*
*   Last commit: $Rev$ by $Author$ on $Date::             $  
*/

package org.eurocarbdb.application.glycanbuilder;

import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.util.SAXUtils;
import org.eurocarbdb.application.glycanbuilder.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.helpers.AttributesImpl;

/**
   This class holds computed information about a fragment structure to
   improve the performance of the various views. The fragment
   structure could be also an intact molecule, and this class is used
   for compatibility also for profiling.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class FragmentEntry implements Comparable<FragmentEntry>, SAXUtils.SAXWriter {


	// ---

	/** The fragment */
	public Glycan   fragment;

	/** The fragment type */
	public String   name;

	/** The mass of the fragment given its mass settings */
	public Double   mass;

	/** The mass-to-charge ratio of the fragment given its mass
    settings */
	public Double   mz_ratio;

	/** A string representation of the fragment structure */
	public String   structure;

	/** The likelyhood of the fragment structure (for future use)*/
	public Double   score;

	/** The object that generated this fragment **/
	public FragmentSource source; 

	/**
       Empty constructor.
	 */
	public FragmentEntry() {
		fragment = null;
		name = "";
		mass = 0.;
		mz_ratio = 0.;
		structure = "";
		score = 0.;
		source = null;
	}   

	/**
       Create a new fragment entry from a fragment structure.  Compute
       all values from the fragment it-self
	 */
	public FragmentEntry(Glycan _fragment, String _name) {
		fragment = (_fragment!=null) ?_fragment.clone() :null;
		name = (_name!=null) ?_name :"";
		mass = (fragment!=null) ?fragment.computeMass() :0.;
		mz_ratio = (fragment!=null) ?fragment.computeMZ() :0.;
		structure = (fragment!=null) ?fragment.toString() :"";
		score = 0.;
		source = null;
	}

	/**
       Create a new fragment entry from a fragment structure with a
       supplied scoring.  Compute all values from the fragment it-self
	 */
	public FragmentEntry(Glycan _fragment, String _name, double _score) {
		fragment = (_fragment!=null) ?_fragment.clone() :null;
		name = (_name!=null) ?_name :"";
		mass = (fragment!=null) ?fragment.computeMass() :0.;
		mz_ratio = (fragment!=null) ?fragment.computeMZ() :0.;
		structure = (fragment!=null) ?fragment.toString() :"";
		score = _score;
		source = null;
	}

	/**
       Create a new fragment entry filling the information with the supplied values
       @param _fragment the fragment structure
       @param _name the fragment type
       @param _mass the fragment mass
       @param _mz_ratio the fragment mass-to-charge ratio
       @param _structure a string representation of the fragment
	 */
	public FragmentEntry(Glycan _fragment, String _name, double _mass, double _mz_ratio, String _structure) {
		fragment = (_fragment!=null) ?_fragment.clone() :null;
		name = (_name!=null) ?_name :"";
		mass = _mass;
		mz_ratio = _mz_ratio;
		structure = (_structure!=null) ?_structure :"";
		score = 0.;
		source = null;
	}

	/**
       Create a new fragment entry filling the information with the supplied values
       @param _fragment the fragment structure
       @param _name the fragment type
       @param _mass the fragment mass
       @param _mz_ratio the fragment mass-to-charge ratio
       @param _structure a string representation of the fragment
       @param _score the likelyhood of the fragment
	 */
	public FragmentEntry(Glycan _fragment, String _name, double _mass, double _mz_ratio, String _structure, double _score) {
		fragment = (_fragment!=null) ?_fragment.clone() :null;
		name = (_name!=null) ?_name :"";
		mass = _mass;
		mz_ratio = _mz_ratio;
		structure = (_structure!=null) ?_structure :"";
		score = _score;
		source = null;
	}

	/**
       Create a copy of the current object.
	 */
	public FragmentEntry clone() {
		FragmentEntry ret = new FragmentEntry();
		ret.fragment = (fragment!=null) ?fragment.clone() :null;
		ret.name = name;
		ret.mass = mass;
		ret.mz_ratio = mz_ratio;
		ret.structure = structure;
		ret.score = score;
		ret.source = source;

		return ret;
	}

	/**
       Compare two fragment entries. The entries are ordered: first by
       score (descending), then by mass-to-charge ratio (ascending)
       and finally by structure.
       @see Glycan#compareTo
	 */
	public int compareTo(FragmentEntry fe) {
		if( fe==null ) return 1;
		if( this.score>fe.score ) return -1;
		if( this.score<fe.score ) return 1;
		if( this.mz_ratio<fe.mz_ratio ) return -1;
		if( this.mz_ratio>fe.mz_ratio ) return 1;
		if( this.fragment==null && fe.fragment==null ) return 0;
		if( this.fragment==null && fe.fragment!=null ) return -1;
		return this.fragment.compareTo(fe.fragment);
	}

	/**
       Return <code>true</code> if this entry contains an empty
       structure.
       @see Glycan#isEmpty
	 */
	public boolean isEmpty() {
		return fragment==null || fragment.isEmpty();
	}

	/**
       Return the fragment structure.
	 */
	public Glycan getFragment() {
		return fragment;
	}

	/**
       Return the fragment mass.
	 */
	public Double getMass() {
		return mass;
	}

	/**
       Return the fragment mass-to-charge ratio.
	 */
	public Double getMZ() {
		return mz_ratio;
	}

	/**
       Return the fragment name.
	 */
	public String getName() {
		return name;
	}

	/**
       Return the string representation of the fragment structure.
	 */
	public String getStructure() {
		return structure;
	}

	/**
       Return the fragment likelyhood.
	 */
	public Double getScore() {
		return score;
	}

	/**
       Set the fragment likelyhood.
	 */
	public void setScore(double d) {
		score = d;
	}

	/**
       Return the fragment generator.
	 */
	public FragmentSource getSource() {
		return source;
	}

	/**
       Set the fragment generator.
	 */
	public void setSource(FragmentSource s) {
		source = s ;
	}

	/**
       Return the charges associated with the fragment molecule.
       @see MassOptions#ION_CLOUD
	 */
	public IonCloud getCharges() {
		return fragment.getMassOptions().ION_CLOUD;
	}

	/**
       Return the neutral exchanges associated with the fragment
       molecule.
       @see MassOptions#NEUTRAL_EXCHANGES
	 */
	public IonCloud getNeutralExchanges() {
		return fragment.getMassOptions().NEUTRAL_EXCHANGES;
	}

	/**
       Create a copy of this fragment entry and add the specified
       charges to it.
	 */
	public FragmentEntry and(IonCloud charges) {
		FragmentEntry ret = this.clone();
		ret.setCharges(charges);
		return ret;
	}

	/**
       Create a copy of this fragment entry and add the specified
       charges and neutral exchanges to it.
	 */
	public FragmentEntry and(IonCloud charges, IonCloud exchanges) {
		FragmentEntry ret = this.clone();
		ret.setCharges(charges,exchanges);
		return ret;
	}

	/**
       Set the charges associated with the fragment molecule, and
       update the mass-to-charge value.
       @see MassOptions#ION_CLOUD     
	 */
	public void setCharges(IonCloud charges) {
		setCharges(charges,new IonCloud(),true);
	}

	/**
       Set the charges and neutral exchanges associated with the
       fragment molecule, and update the mass-to-charge value.
       @see MassOptions#ION_CLOUD     
	 */
	public void setCharges(IonCloud charges, IonCloud exchanges) {
		setCharges(charges,exchanges,true);
	}

	/**
       Set the charges and neutral exchanges associated with the
       fragment molecule. If <code>update_mz</code> is
       <code>true</code> update the mass-to-charge value.
       @see MassOptions#ION_CLOUD     
	 */

	public void setCharges(IonCloud charges, IonCloud exchanges, boolean update_mz) {
		if( fragment!=null ) {
			fragment.getMassOptions().ION_CLOUD = charges;
			fragment.getMassOptions().NEUTRAL_EXCHANGES = exchanges;
			structure = fragment.toString();
			if( update_mz )
				mz_ratio = charges.computeMZ(exchanges.getIonsMass() + mass);
		}
	}

	/**
       Update the mass of the fragment.
	 */
	public void updateMass() {
		IonCloud charges = fragment.getMassOptions().ION_CLOUD;
		IonCloud exchanges = fragment.getMassOptions().NEUTRAL_EXCHANGES;
		mass = fragment.computeMass();
		mz_ratio = charges.computeMZ(exchanges.getIonsMass() + mass);
	}

	/**
       Return <code>true</code> if the two fragment entries contain
       the same structure and have the same type.
	 */
	public boolean equals(Object other) {
		if( !(other instanceof FragmentEntry) )
			return false;

		FragmentEntry fe = (FragmentEntry)other;
		if( this.fragment==null )
			return fe.fragment==null;
		return (Math.abs(this.mass-fe.mass)<0.005 && this.fragment.equalsStructure(fe.fragment) && this.name.equals(fe.name));
	}

	/**
       Generate an hash code for this entry using the mass, the
       structure and the type of the fragment.
	 */
	public int hashCode() {
		int ret = 0;
		ret += mass.hashCode();
		ret += fragment.hashCode();
		ret += name.hashCode();
		return ret;
	}

	// serialization

	/**
       Generate a string representation of this entry using the type, the mass, the
       score and the structure of the fragment.
	 */
	public String toString() {
		return ("" + name + " " + mass + " " + score + " " + structure);
	}

	/**
       Create a new object from its XML representation as part of a
       DOM tree.
	 */
	static public FragmentEntry fromXML(Node fe_node) throws Exception {
		FragmentEntry ret = new FragmentEntry();

		ret.structure = XMLUtils.getAttribute(fe_node,"fragment");
		ret.name = XMLUtils.getAttribute(fe_node,"name");
		ret.mass = Double.valueOf(XMLUtils.getAttribute(fe_node,"mass"));
		ret.mz_ratio = Double.valueOf(XMLUtils.getAttribute(fe_node,"mz_ratio"));
		ret.fragment = (ret.structure.length()>0) ?Glycan.fromString(ret.structure,new MassOptions()) :null;
		if( XMLUtils.getAttribute(fe_node,"score")!=null )
			ret.score = Double.valueOf(XMLUtils.getAttribute(fe_node,"score"));

		return ret;
	}

	/**
       Create an XML representation of this object to
       be part of a DOM tree.
	 */
	public Element toXML(Document document) {
		if( document==null )
			return null;

		// create root node
		Element fe_node = document.createElement("FragmentEntry");
		if( fe_node==null )
			return null;

		fe_node.setAttribute("fragment",(fragment!=null) ?fragment.toString() :"");
		fe_node.setAttribute("name",name);
		fe_node.setAttribute("mass",mass.toString());
		fe_node.setAttribute("mz_ratio",mz_ratio.toString());
		fe_node.setAttribute("score",score.toString());

		return fe_node;
	}

	/**
       Default SAX handler to read a representation of this object
       from an XML stream.
	 */ 
	public static class SAXHandler extends SAXUtils.ObjectTreeHandler {

		public boolean isElement(String namespaceURI, String localName, String qName) {
			return qName.equals(getNodeElementName());
		}

		/**
       Return the element tag recognized by this handler
		 */
		public static String getNodeElementName() {
			return "FragmentEntry";
		}

		protected void initContent(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
			super.initContent(namespaceURI,localName,qName,atts);

			FragmentEntry ret = new FragmentEntry();    
			ret.structure = stringAttribute(atts,"fragment","");
			ret.name = stringAttribute(atts,"name","");
			ret.mass = doubleAttribute(atts,"mass",0.);
			ret.mz_ratio = doubleAttribute(atts,"mz_ratio",0.);
			ret.score = doubleAttribute(atts,"score",0.);
			ret.fragment = (ret.structure.length()>0) ?Glycan.fromString(ret.structure,new MassOptions()) :null;

			object = ret;
		}
	}

	/**
       Write a representation of this object into an XML stream using
       a SAX handler.
	 */
	public void write(javax.xml.transform.sax.TransformerHandler th) throws SAXException  {
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","","fragment","CDATA",(fragment!=null) ?fragment.toString() :"");    
		atts.addAttribute("","","name","CDATA",name);
		atts.addAttribute("","","mass","CDATA",mass.toString());
		atts.addAttribute("","","mz_ratio","CDATA",mz_ratio.toString());
		atts.addAttribute("","","score","CDATA",score.toString());

		th.startElement("","","FragmentEntry",atts);
		th.endElement("","","FragmentEntry");
	}



}
