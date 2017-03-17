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

import java.awt.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import org.eurocarbdb.application.glycanbuilder.FragmentCollection.SAXHandler;
import org.eurocarbdb.application.glycanbuilder.fileutil.ExtensionFileFilter;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.util.SAXUtils;
import org.eurocarbdb.application.glycanbuilder.util.XMLUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.helpers.AttributesImpl;

/**
   An instance of the BaseDocument class that stores glycan structure
   with the list of computed fragments.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class FragmentDocument extends BaseDocument implements SAXUtils.SAXWriter {    


    protected LinkedList<Glycan> theStructures;
    protected LinkedList<FragmentCollection> theFragments;
    
    protected LinkedList<Double>        theFragmentMZs;
    protected LinkedList<FragmentGroup> theFragmentGroups;
   

    protected LinkedList<StructuresChangeListener> sc_listeners = new LinkedList<StructuresChangeListener>();

    /**
       Listener for changes in the number and identity of the
       structures contained in the document. For changes in the list
       of fragments see {@link BaseDocument.DocumentChangeListener}
     */

    public interface StructuresChangeListener {
    public void structuresChanged(StructuresChangeEvent e);
    }

    /**
       Basic event object used to notify the listeners of a change in
       the number or identity of the structures contained in the
       document.
     */
    public static class StructuresChangeEvent {

    protected FragmentDocument source;

    /**
       Default constructor
       @param _source the document that originated the event
     */
    public StructuresChangeEvent(FragmentDocument _source) {
        source = _source;
    }

    /**
       Return the document that originated the event
     */
    public FragmentDocument getSource() {
        return source;
    }
    }
     

    //----------------
    
    /**
       Empty constructor
     */
    public FragmentDocument() {
    super();
    }

    //---------------- DATA ACCESS -----------------
    
    public int size() {
    return theFragments.size();
    }   

    /**
       Return the number of glycan structures in the document
     */
    public int getNoStructures() {
    return theStructures.size();
    }

    /**
       Return the number of different mass/charge values corresponding
       to the fragments in the document.
     */
    public int getNoPeaks() {
    return theFragmentMZs.size();
    }

    public String getName() {
    return "Fragments";
    }

    public javax.swing.ImageIcon getIcon() {
    return FileUtils.themeManager.getImageIcon("fragmentdoc");
    }

    public Collection<javax.swing.filechooser.FileFilter> getFileFormats() {
    LinkedList<javax.swing.filechooser.FileFilter> filters = new LinkedList<javax.swing.filechooser.FileFilter>();
    
    filters.add(new ExtensionFileFilter("gwf", "GlycoWorkbench fragments file"));
    return filters;
    }

    public javax.swing.filechooser.FileFilter getAllFileFormats() {
    return new ExtensionFileFilter("gwf", "Fragments files");
    }

    //-----------
    // data
    
    /**
       Copy the content of another document
     */
    public void copy(FragmentDocument src) {
    setData(src,true);
    }

    private void setData(FragmentDocument src, boolean fire) {
    if( src!=null ) {
        // clear
        initData();
       
        // copy 
        for( int i=0; i<src.getNoStructures(); i++ )
        addFragments(src.getStructure(i),src.getFragments(i));
        
        // fire events
        if( fire ) {
        fireStructuresChanged();
        fireDocumentChanged();
        }
    }
    }

    /**
       Return the list of structures contained in the document
     */
    public Collection<Glycan> getStructures() {
    return theStructures;
    }

    /**
       Return a specific structure contained in the document
       @param s_ind the index of the structure, from 0 to {@link
       #getNoStructures}
     */
    public Glycan getStructure(int s_ind) {
    return theStructures.get(s_ind);
    }

    /**
       Return the list of fragments for a specific structure contained
       in the document
       @param s_ind the index of the structure, from 0 to {@link
       #getNoStructures}
     */
    public FragmentCollection getFragments(int s_ind) {
    return theFragments.get(s_ind);
    }
    
    /**
       Return a specific mass/charge value from the ones contained in
       the document
       @param p_ind the index of the mass/charge value, from 0 to
       {@link #getNoPeaks}
     */
    public Double getFragmentMZ(int p_ind) {
    return theFragmentMZs.get(p_ind);
    }
    
    /**
       Return the fragments associated with a specific mass/charge
       value from the ones contained in the document
       @param p_ind the index of the mass/charge value, from 0 to
       {@link #getNoPeaks}
     */
    public FragmentGroup getFragmentGroup(int p_ind) {
    return theFragmentGroups.get(p_ind);
    }

    /**
       Return the fragments associated with a specific mass/charge
       value and structure from the ones contained in the document
       @param p_ind the index of the mass/charge value, from 0 to
       {@link #getNoPeaks}
       @param s_ind the index of the structure, from 0 to {@link
       #getNoStructures}
     */
    public LinkedList<Glycan> getFragments(int p_ind, int s_ind) { 
    return theFragmentGroups.get(p_ind).getFragments(s_ind);
    }

    /**
       Add a glycan structure and the corresponding list of fragments
       to the document. Send a {@link
       FragmentDocument.StructuresChangeEvent} and a {@link
       BaseDocument.DocumentChangeEvent} to the listeners
       @return <code>true</code> if the operation was successful
     */
    public boolean addFragments(Glycan _structure, FragmentCollection _fragments) {
    return addFragments(_structure,_fragments,true);
    }

    /**
       Add a glycan structure and the corresponding list of fragments
       to the document. 
       @param fire if <code>true</code> send a {@link
       FragmentDocument.StructuresChangeEvent} and a {@link
       BaseDocument.DocumentChangeEvent} to the listeners
       @return <code>true</code> if the operation was successful
     */
    public boolean addFragments(Glycan _structure, FragmentCollection _fragments, boolean fire) {
    if( _structure==null )
        return false;
    if( _fragments==null )
        _fragments = new FragmentCollection();

    Glycan toadds = _structure.clone();
    FragmentCollection toaddf = _fragments.clone();

    theStructures.add(toadds);
    theFragments.add(toaddf);    
    for( FragmentEntry fe : toaddf.getFragments() ) 
        addFragment(theStructures.size()-1,fe);

    if( fire ) {
        fireStructuresChanged();
        fireDocumentChanged();
    }
    return true;
    }        

    /**
       Add a collection of fragments to a specific glycan structure.
       Send a {@link BaseDocument.DocumentChangeEvent} to the
       listeners
       @return <code>true</code> if the operation was successful
     */
    public boolean addFragments(int s_ind, Collection<FragmentEntry> toadd) {
    if( s_ind<0 || s_ind>=size() )
        return false;
    
    boolean added = false;
    for( FragmentEntry fe : toadd ) {
        added |= theFragments.get(s_ind).addFragment(fe);
        if( added )
        addFragment(s_ind,fe);
    }

    if( added )
        fireDocumentChanged();
    return added;
    }

    private void addFragment(int s_ind, FragmentEntry fe) {
    // search position
    int p_ind = 0;
    for( ; p_ind<theFragmentMZs.size() && theFragmentMZs.get(p_ind)<fe.mz_ratio; p_ind++);

    // if empty or non existing mz valu add
    if( p_ind==theFragmentMZs.size() || theFragmentMZs.get(p_ind)>(fe.mz_ratio+0.000001) ) {
        theFragmentMZs.add(p_ind, fe.mz_ratio);
        theFragmentGroups.add(p_ind, new FragmentGroup());
    }

    // make sure every fragment group is the same size
    for( FragmentGroup fg : theFragmentGroups )
        fg.assertSize(s_ind);
     
    // add entry to fragment group    
    theFragmentGroups.get(p_ind).addFragment(s_ind,fe);
    }
    
    /**
       Remove a structure and all its corresponding fragments. Send a
       {@link FragmentDocument.StructuresChangeEvent} and a
       {@link BaseDocument.DocumentChangeEvent} to the listeners
       @param s_ind the index of the structure, from 0 to {@link
       #getNoStructures}
       @return <code>true</code> if the operation was successful
    */
    public boolean removeFragments(int s_ind) {
    if( s_ind<0 || s_ind>=size() )
        return false;
    
    theStructures.remove(s_ind);
    theFragments.remove(s_ind);
    for( int p_ind=0; p_ind<theFragmentMZs.size(); p_ind++ ) {
        theFragmentGroups.get(p_ind).removeFragments(s_ind);
        if( theFragmentGroups.get(p_ind).isEmpty() ) {
        theFragmentGroups.remove(p_ind);
        theFragmentMZs.remove(p_ind);
        --p_ind;
        }
    }

    fireStructuresChanged();    
    fireDocumentChanged();
    return true;
    }   

    /**
       Remove all the fragments at the specified mass/charge values
       from all the structures. Send a {@link
       BaseDocument.DocumentChangeEvent} to the listeners
       @param p_inds the list of indexes of the mass/charge values,
       each one from 0 to {@link #getNoPeaks}
       @return <code>true</code> if the operation was successful
    */
    public boolean clearFragmentsFor(int[] p_inds) {
    LinkedList<Double> toremove_mzs = new LinkedList<Double>();
    for( int i=0; i<p_inds.length; i++ )
        toremove_mzs.add(theFragmentMZs.get(p_inds[i]));

    LinkedList<FragmentGroup> toremove_groups = new LinkedList<FragmentGroup>();
    for( int i=0; i<p_inds.length; i++ ) {
        FragmentGroup fg = theFragmentGroups.get(p_inds[i]);        
        for( int s_ind=0; s_ind<theStructures.size(); s_ind++ )
        theFragments.get(s_ind).removeFragments(fg.getFragmentEntries(s_ind));
        toremove_groups.add(fg);
    }

    theFragmentMZs.removeAll(toremove_mzs);
    theFragmentGroups.removeAll(toremove_groups);
    
    fireDocumentChanged();
    return true;
    }
             


    /**
       Remove a collection of fragments from the list corresponding to
       a particular a structure. Send a {@link
       FragmentDocument.StructuresChangeEvent} and a
       {@link BaseDocument.DocumentChangeEvent} to the listeners
       @param s_ind the index of the structure, from 0 to {@link
       #getNoStructures}        
       @param toremove the list of fragment entries to be removed
       @return <code>true</code> if the operation was successful
    */

    public boolean removeFragments(int s_ind, Collection<FragmentEntry> toremove) {
    if( s_ind<0 || s_ind>=size() )
        return false;
    
    boolean removed = false;
    for( FragmentEntry fe : toremove ) {
        removed |= theFragments.get(s_ind).removeFragment(fe);
        if( removed ) {
        int p_ind = removeFragment(s_ind,fe);
        if( p_ind!=-1 ) {
            theFragmentGroups.remove(p_ind);
            theFragmentMZs.remove(p_ind);
        }
        }
    }

    if( removed )
        fireDocumentChanged();
    return removed;
    }
     
    private int removeFragment(int s_ind, FragmentEntry fe) {
    // search position
    int p_ind = 0;
    for( ; p_ind<theFragmentMZs.size() && theFragmentMZs.get(p_ind)<fe.mz_ratio; p_ind++);

    // if empty or non existing mz value return -1
    if( p_ind==theFragmentMZs.size() || theFragmentMZs.get(p_ind)>(fe.mz_ratio+0.000001) ) 
        return -1;
     
    // remove entry from fragment group
    theFragmentGroups.get(p_ind).removeFragment(s_ind,fe);
    return p_ind;
    }

    //---------------
    // initialization
   

    public void initData() {
    theStructures = new LinkedList<Glycan>();
    theFragments = new LinkedList<FragmentCollection>();
    theFragmentMZs = new LinkedList<Double>();
    theFragmentGroups = new LinkedList<FragmentGroup>();
    }
    


    //---------------
    // events
    
    /**
       Register a new listener for changes in the structure list
     */
    public void addStructuresChangeListener(StructuresChangeListener l) {
    if( l!=null )
        sc_listeners.add(l);
    }

    /**
       Deregister an existing listener for changes in the structure list
     */
    public void removeStructuresChangeListener(StructuresChangeListener l) {
    if( l!=null )
        sc_listeners.remove(l);
    }
        

    /**
       Send a structure list change event to the listeners
     */
    public void fireStructuresChanged() {
    if( sc_listeners!=null ) {
        for( StructuresChangeListener scl : sc_listeners ) 
        scl.structuresChanged(new StructuresChangeEvent(this));    
    }
    }

    /**
       Send a document init event to all listeners and reset the
       <code>changed</code> flag. Also send a structure list change
       event to the corresponding listeners
     */
    public void fireDocumentInit() {
    super.fireDocumentInit();
    fireStructuresChanged();
    }

    /**
       Send a document init event to all listeners with a different
       source and reset the <code>changed</code> flag. Also send a
       structure list change event to the corresponding listeners if
       the <code>source</code> is this document
     */
    public void fireDocumentInit(BaseDocument source) {
    super.fireDocumentInit(source);
    if( source==this )
        fireStructuresChanged();
    }

    //--------------- 
    // serialization
    
    protected void read(InputStream is, boolean merge) throws Exception {
    /*Document document = XMLUtils.read(bis);
    if( document==null )
        throw new Exception("Cannot read from string");
    
    Node root_node = XMLUtils.assertChild(document,"Fragments");
    fromXML(root_node,merge);*/
    SAXUtils.read(is,new SAXHandler(this,merge));    
    }

    public void fromString(String str, boolean merge) throws Exception {
    read(new ByteArrayInputStream(str.getBytes()),merge);        
    }

    public String toString() {
    try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        write(bos);
        return bos.toString();
    }
    catch(Exception e) {
        LogUtils.report(e);
        return "";
    }
    }    

    public void write(OutputStream os) throws Exception {
    /*
    Document document = XMLUtils.newDocument();
    if( document==null )
        return "";

    Element root_node = toXML(document);
    if( root_node == null )
        return "";

    document.appendChild(root_node);    
    XMLUtils.write(os,document);*/
    SAXUtils.write(os,this);
    }

    /**
       Create a new document from its XML representation as part of a
       DOM tree.
     */
    public void fromXML(Node root_node, boolean merge) throws Exception {
    // clear
    if( !merge ) {
        resetStatus();
        initData(); 
    }
    else
        setChanged(true);

    // read data
    LinkedList<Glycan> structures = new LinkedList<Glycan>();
    LinkedList<FragmentCollection> fragments = new LinkedList<FragmentCollection>();

    // read structures
    LinkedList<Node> s_nodes = XMLUtils.findAllChildren(root_node, "Glycan");
    for( Node s_node : s_nodes) 
        structures.add(Glycan.fromXML(s_node,new MassOptions()));

    // read fragments
    LinkedList<Node> fc_nodes = XMLUtils.findAllChildren(root_node, "FragmentCollection");
    for( Node fc_node : fc_nodes) 
        fragments.add(FragmentCollection.fromXML(fc_node));

    if( structures.size()!=fragments.size() )
        throw new Exception("Invalid number of fragments");

    // add all
    for( int i=0; i<structures.size(); i++ ) 
        addFragments(structures.get(i),fragments.get(i),false);    
    }

    /**
       Create an XML representation of this object to be part of a DOM
       tree.
    */
    public Element toXML(Document document) {
    if( document==null )
        return null;
    
    // create root node
    Element root_node = document.createElement("Fragments");
    if( root_node==null )
        return null;

    // add structures
    for(Glycan s : theStructures ) 
        root_node.appendChild(s.toXML(document));    

    // add fragments
    for(FragmentCollection fc : theFragments ) 
        root_node.appendChild(fc.toXML(document));    

    return root_node;
    }

    /**
       Default SAX handler to read a representation of this object
       from an XML stream.
     */    
    public static class SAXHandler extends SAXUtils.ObjectTreeHandler {
    
    private FragmentDocument theDocument;
    private boolean merge;
    
    /**
       Construct a new handler. 
       @param _doc recipient for the data parsed from the XML
       @param _merge if <code>true</code> append the new data to
       the existing document.
    */
    public SAXHandler(FragmentDocument _doc, boolean _merge) {
        theDocument = _doc;
        merge = _merge;
    }

    public boolean isElement(String namespaceURI, String localName, String qName) {
        return qName.equals(getNodeElementName());
    }


    /**
       Return the element tag recognized by this handler
     */
    public static String getNodeElementName() {
        return "Fragments";
    }

    protected SAXUtils.ObjectTreeHandler getHandler(String namespaceURI, String localName, String qName) {
        if( qName.equals(Glycan.SAXHandler.getNodeElementName()) )
        return new Glycan.SAXHandler(new MassOptions());
        if( qName.equals(FragmentCollection.SAXHandler.getNodeElementName()) )
        return new FragmentCollection.SAXHandler();
        return null;
    }

    protected Object finalizeContent(String namespaceURI, String localName, String qName) throws SAXException{
        // clear
        if( !merge ) {
        theDocument.resetStatus();
        theDocument.initData(); 
        }
        else
        theDocument.setChanged(true);
        
        // read data
        LinkedList<Object> structures = getSubObjects(Glycan.SAXHandler.getNodeElementName());
        LinkedList<Object> fragments = getSubObjects(FragmentCollection.SAXHandler.getNodeElementName());
        if( structures.size()!=fragments.size() )
        throw new SAXException(createMessage("Invalid number of fragments"));

        // add all
        for( int i=0; i<structures.size(); i++ ) 
        theDocument.addFragments((Glycan)structures.get(i),(FragmentCollection)fragments.get(i),false);    

        return (object = theDocument);
    }
    }

    public void write(TransformerHandler th) throws SAXException {
    th.startElement("","","Fragments",new AttributesImpl());

    for(Glycan s : theStructures ) 
        s.write(th);
    for(FragmentCollection fc : theFragments ) 
        fc.write(th);

    th.endElement("","","Fragments");
    }

}
