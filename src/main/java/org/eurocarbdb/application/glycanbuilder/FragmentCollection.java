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

import java.io.*;
import java.util.*;

import org.eurocarbdb.application.glycanbuilder.util.SAXUtils;
import org.eurocarbdb.application.glycanbuilder.util.XMLUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.helpers.AttributesImpl;

/**
   This class holds an ordered set of fragment entries.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class FragmentCollection implements SAXUtils.SAXWriter {    

   

    // -----

    private LinkedList<FragmentEntry> fragments;

    /**
       Empty constructor.
     */
    public FragmentCollection() {
    fragments = new LinkedList<FragmentEntry>();
    }

    /**
       Create a complete copy of this object. All the fragment entries
       are copied.
     */

    public FragmentCollection clone() {
    FragmentCollection ret = new FragmentCollection();
    for( Iterator<FragmentEntry> i=fragments.iterator(); i.hasNext(); ) 
        ret.fragments.add(i.next().clone());
    return ret;
    }

    /**
       Return the underlying collection of fragment entries.
     */
    public Collection<FragmentEntry> getFragments() {
    return fragments;
    }    

    /**
       Return an iterator over the underlying collection of fragment
       entries.
     */
    public Iterator<FragmentEntry> iterator() {
    return fragments.iterator();
    }

    /**
       Return the entry at the specified position.
     */
    public FragmentEntry get(int ind) {
    return fragments.get(ind);
    }
    
    /**
       Return the number of entries in the collection.
     */
    public int size() {
    return fragments.size();
    }

    /**
       Add a new fragment entry to the collection. The entry is
       generated from the supplied information. Duplicated entries are
       discarded.
       @return <code>true</code> if the operation was successful
     */
    public boolean addFragment(Glycan _fragment, String _name) {
    if( _fragment==null )
        return false;
    if( _fragment.getRoot()==null )
        return false;
    
    Double   _mass = _fragment.computeMass();
    Double   _mz   = _fragment.computeMZ();
    String   _structure = _fragment.toString();
    return addFragment(new FragmentEntry(_fragment,_name,_mass,_mz,_structure));    
    }    

    /**
       Add a fragment entry to the collection. Duplicated entries are
       discarded.
       @return <code>true</code> if the operation was successful
     */
    public boolean addFragment(FragmentEntry toadd) {
    // sorted insertion
    for( int i=0; i<fragments.size(); i++ ) {        
        FragmentEntry fe = fragments.get(i);
        int comp = toadd.compareTo(fe);
        if( comp==0 )
        return false;
        if( comp<0 ) {
        fragments.add(i, toadd);
        return true;
        }
        
        /*if( fe.mass.equals(toadd.mass) && fe.structure.equalsStructure(toadd.structure) ) {   
        if( fe.name.length()>toadd.name.length() ) {
            fragments.setElementAt(toadd,i);
            return true;
        }
        return false;        
        }
        if( fe.mass>toadd.mass ) {
        fragments.insertElementAt(toadd,i);
        return true;
        }*/
    }
    
    fragments.add(toadd);
    return true;
    }    

    /**
       Remove the specified fragments from the collection.
       @return <code>true</code> if the operation was successful
     */
    public boolean removeFragments(Collection<FragmentEntry> fec) {
    if( fec==null )
        return false;
    
    return fragments.removeAll(fec);
    }    

    /**
       Remove the fragment entry from the collection.
       @return <code>true</code> if the operation was successful
     */
    public boolean removeFragment(FragmentEntry _fe) {
    if( _fe==null )
        return false;

    for( int i=0; i<fragments.size(); i++ ) {
        if( fragments.get(i).equals(_fe) ) {
        fragments.remove(i);
        return true;
        }
    }
    return false;
    }    

    /**
       Merge two fragment collections.
     */
    public boolean addFragments(FragmentCollection _fc) {
    if( _fc==null )
        return false;
    return addFragments(_fc.getFragments());
    }

    /**
       Add all the specified fragments to the collection.
     */
    public boolean addFragments(Collection<FragmentEntry> _fragments) {
    if( fragments==null )
        return false;

    boolean added = false;
    for( FragmentEntry fe : _fragments) 
        added |= addFragment(fe.clone());
    return added;
    }
    
    /*public void updateMasses(MassOptions mass_opt) {
    FragmentCollection updated = new FragmentCollection();       
    for( Iterator<FragmentEntry> i=fragments.iterator(); i.hasNext(); ) {
        FragmentEntry e = i.next();
        updated.addFragment(e.fragment,e.name,mass_opt);
    }
    fragments = updated.fragments;
    }
    */

    //---------------
    // serialization

    /**
       Create a new object from its XML representation as part of a
       DOM tree.
    */
    static public FragmentCollection fromXML(Node root_node) throws Exception {
    
    FragmentCollection ret = new FragmentCollection();

    // read fragments
    LinkedList<Node> fe_nodes = XMLUtils.findAllChildren(root_node, "FragmentEntry");
    for( Node fe_node : fe_nodes) 
        ret.addFragment(FragmentEntry.fromXML(fe_node));

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
    Element root_node = document.createElement("FragmentCollection");
    if( root_node==null )
        return null;

    // add fragments
    for(FragmentEntry fe : fragments ) 
        root_node.appendChild(fe.toXML(document));    

    return root_node;
    }

    /**
       Default SAX handler to read a representation of this object
       from an XML stream.
     */
    public static class SAXHandler extends SAXUtils.ObjectTreeHandler {
     
    public boolean isElement(String namespaceURI, String localName, String qName) {
        return qName.equals(getNodeElementName());
    }

    public static String getNodeElementName() {
        return "FragmentCollection";
    }

    protected SAXUtils.ObjectTreeHandler getHandler(String namespaceURI, String localName, String qName)  throws SAXException {
        if( qName.equals(FragmentEntry.SAXHandler.getNodeElementName()) )
        return new FragmentEntry.SAXHandler();
        return null;
    }

    protected Object finalizeContent(String namespaceURI, String localName, String qName) throws SAXException {
        FragmentCollection ret = new FragmentCollection();

        for( Object o : getSubObjects(FragmentEntry.SAXHandler.getNodeElementName()) ) 
        ret.addFragment((FragmentEntry)o);

        return (object = ret);
    }
    }

    /**
       Write a representation of this object into an XML stream using
       a SAX handler.
     */
    public void write(TransformerHandler th) throws SAXException {
    th.startElement("","","FragmentCollection",new AttributesImpl());
    for(FragmentEntry fe : fragments ) 
        fe.write(th);
    th.endElement("","","FragmentCollection");
    }

 
}