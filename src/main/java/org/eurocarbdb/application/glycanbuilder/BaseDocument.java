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

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;

/**
   Base class for all document type objects.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public abstract class BaseDocument {    

    /**
       Listener for document events.
     */

    public interface DocumentChangeListener {   
    
    /**
       Called when the document is initialized.
     */
    public void documentInit(DocumentChangeEvent e);
    
    /**
       Called when some part of the document has changed.
     */
    public void documentChanged(DocumentChangeEvent e);
    }

    /**
       Base class for document events.
     */

    public static class DocumentChangeEvent extends java.util.EventObject {
    protected BaseDocument theDoc;
    
    /**
       Default constructor, set the event source to the changed
       document.
     */
    public DocumentChangeEvent(BaseDocument _theDoc) {
        super(_theDoc);

        theDoc = _theDoc;
    }
    }
    
    // undo/redo
    protected GlycanUndoManager theUndoManager = null;

    // file handling
    protected String filename = "";
    protected boolean was_saved = false;
    protected boolean has_changed = false;

    // events
    protected Vector<DocumentChangeListener> dc_listeners = new Vector<DocumentChangeListener>();

    //----------------
    
    /**
       Empty constructor.
       @see #init
     */

    public BaseDocument() {
    init();
    theUndoManager = new GlycanUndoManager(this);
    this.components=new HashMap<String,Component>();
    }

    /**
       Empty constructor. 
       @param undo_redo if <code>true</code> provides undo/redo
       facilities for this document
       @see #init
       @see GlycanUndoManager
     */
    public BaseDocument(boolean undo_redo) {
    init();
    this.components=new HashMap<String,Component>();
    if( undo_redo )
        theUndoManager = new GlycanUndoManager(this);
    }

    //---------------- DATA ACCESS -----------------
    
    /**
       Return the size of the document. Implementation dependent.
     */
    abstract public int size();

    /**
       Return <code>true</code> if the size of the document is 0.
     */
    public boolean isEmpty() {
    return (size()==0);
    }

    /**
       Return the name of the document. Implementation dependent.
     */
    abstract public String getName();

    /**
       Return the icon associated with the document. The default
       value is a default empty document icon.
     */
    public javax.swing.ImageIcon getIcon() {
    	try {
			return FileUtils.themeManager.getImageIcon("basedoc",ICON_SIZE.L2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LogUtils.report(e);
		}
		return null;
    }

    /**
       Return the path to the file from which the document has been
       loaded or where the document has been saved. Return
       "[untitled]" otherwise.
     */
    public String getFileName() {
    return filename;
    }

    /**
       Return an object representing the file associated with the document, or null otherwise.
       @see #getFileName
     */
    public File getFile() {
    if( filename!=null && filename.length()>0 && was_saved ) 
        return new File(filename);                        
        return null;
    }

    /**
       Return the list of file formats accepted by this document.
     */
    abstract public Collection<javax.swing.filechooser.FileFilter> getFileFormats();

    /**
       Return the list of file formats accepted by this document as a
       single filter.
     */
    abstract public javax.swing.filechooser.FileFilter getAllFileFormats();

    /**
       Return <code>true</code> if the document was saved to file.
     */
    public boolean wasSaved() {
    return was_saved;
    }

    /**
       Return <code>true</code> if the document has changed from the
       last initialization.
     */
    public boolean hasChanged() {
    return has_changed;
    } 

    /**
       Return the undo/redo manager.
     */
    public GlycanUndoManager getUndoManager() {
    return theUndoManager;
    }
    
    //---------------
    // initialization

    /**
       Reset <code>filename</code>, <code>saved</code> and
       <code>changed</code> flags.
     */
    public void resetStatus() {
    filename = "[untitled]";
    was_saved = false;
    has_changed = false;
    }

    /**
       Set the name of the file associated with the document. Set the
       <code>save</code> flag and reset the <code>changed</code> flag.
     */
    public void setFilename(String filename) {
    this.filename = filename;
    this.was_saved = true;
    this.has_changed = false;
    }
    
    /**
       Manually set the <code>changed</code> flag.
     */
    public void setChanged(boolean changed) {
    has_changed = changed;
    }

    /**
       Initialize the document and reset the status.      
     */
    public void init() {
    
    // source
    resetStatus();

    // init data
    initData();
    
    // fire event
    fireDocumentInit();
    } 

    /**
       Initialize the document by parsing it from a string.
       @see #fromString
     */
    public boolean init(String value) {
    try {
        // source
        resetStatus();
        
        // init data
        initData();
        fromString(value);
        
        // fire event
        fireDocumentInit();
        
        return true;
    }    
    catch( Exception e ) {
        LogUtils.report(e);
        return false;
    }
    }

    protected boolean fill(String value) {
    return fill(value,false);
    }

    protected boolean fill(String value, boolean initial_state) {
    try {
      
        //
        fromString(value);
        
        // fire event
        if( initial_state )
        fireDocumentRestored();
        else
        fireDocumentChanged();
        
        return true;
    }    
    catch( Exception e ) {
        LogUtils.report(e);
        return false;
    }
    }

    /**
       Empty the current document.
     */
    public void clear() {
    initData();
    fireDocumentChanged();
    }

    /**
       Implementation dependent part of the document initialization
       process.
     */
    abstract public void initData();  

    /**
       Read the document from a file.
       @param merge if <code>true</code> the content of the file is
       appended to the current document, when possible
       @param warning if <code>true</true> report when the file cannot
       be parsed
       @see #read
     */
    public boolean open(String filename, boolean merge, boolean warning) {
    	
    return open(new File(filename), merge, warning);
    }

    /**
       Read the document from a file.
       @param merge if <code>true</code> the content of the file is
       appended to the current document, when possible
       @param warning if <code>true</true> report when the file cannot
       be parsed
       @see #read
     */
    public boolean open(File file, boolean merge, boolean warning)
    {
    FileInputStream fis = null;
    try {
        fis = new FileInputStream(file);

        // read structure
        try {
        read(fis,merge);
        }
        catch(Exception e) {
        // What is already open is not the file's fault. This used to call init(), which cleared the
        // current document before returning false - so a malformed file took the work that was on
        // screen with it, and "Open additional document..." destroyed the document it was supposed
        // to be adding to. GlycanDocument.fromString parses into a list of its own before it touches
        // this document, so there is nothing half-read to tidy up after.
        System.err.println("Got exception: "+e.getMessage());
        if( warning )
            throw e;
        return false;
        }

        if( merge ) {
            // Opening a second file *into* this one leaves a document that is neither file: it is
            // what was here plus what arrived. So it keeps its own name - taking the merged file's
            // would make a later Save write over a file the user did not edit - and it counts as
            // changed, because it is (#178). It used to do neither: setFilename cleared the changed
            // flag as a side effect, so the document came out marked saved, the asterisk never
            // appeared, Save stayed disabled, and closing threw the merge away without asking.
            //
            // Changed rather than initialized: fireDocumentInit clears the flag itself, which is
            // right for a document that has just become a file's contents and wrong for one that has
            // just stopped being them.
            fireDocumentChanged();

            return true;
        }

        setFilename(file.getAbsolutePath());
        fireDocumentInit();
        return true;
    }
    catch( Exception e ) {
        LogUtils.report(e);
        return false;
    }
    finally {
        // Closed either way. It used to be left to the garbage collector, so a failed open held the
        // file open for as long as the collector took to notice - which on Windows is the difference
        // between being able to delete or replace it and not.
        if( fis!=null ) {
            try {
                fis.close();
            }
            catch( Exception cannotClose ) {
                LogUtils.report(cannotClose);
            }
        }
    }
    }

    protected void read(InputStream is, boolean merge) throws Exception {
    	System.err.println("in read");
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    fromString(consume(br),merge);
    }

    //ファイルを読み取る処理
    protected String consume(BufferedReader br) throws Exception {
    StringBuilder ret = new StringBuilder();

    int ch;
    while( (ch = br.read())!=-1 ) 
        ret.appendCodePoint(ch);
    
    return ret.toString();
    }

    /**
       Save the document to a file.
       @see #write
     */
    public boolean save(String filename) {

    // The document is told it has been saved only once it has been. setFilename sets was_saved and
    // clears has_changed as a side effect, and calling it first meant that an unwritable destination,
    // a full disk or a serialization failure returned false while leaving the document marked saved
    // and clean - so the asterisk went away, Save went grey, and the next close let the work go
    // without asking. The write is what decides; the bookkeeping follows it.
    File tmpfile = null;

    try{
        // write to tmp file, so nothing touches the destination until a whole document exists
        tmpfile = File.createTempFile("gwb",null);

        FileOutputStream out = new FileOutputStream(tmpfile);
        try {
            write(out);
        }
        finally {
            out.close();
        }

        // copy to dest file
        FileUtils.copy(tmpfile,new File(filename));

        setFilename(filename);
        fireDocumentInit();
        return true;
    }
    catch( Exception e ) {
        LogUtils.report(e);
        return false;
    }
    finally {
        // Ours, and gone either way. It used to be left behind on every failing path.
        if( tmpfile!=null ) tmpfile.delete();
    }
    }
    
    /**
     */
    public void write(OutputStream os) throws Exception {
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
    
    String str = toString();
    bw.write(str,0,str.length());
    bw.newLine();
    bw.close();
    }


    //---------------
    // events

    /**
       Register a new listener of document change events.
     */
    public void addDocumentChangeListener(DocumentChangeListener l) {
    if( l!=null && !dc_listeners.contains(l) )
        dc_listeners.add(l);
    }

    /**
       Remove an existing listener of document change events.
     */
    public void removeDocumentChangeListener(DocumentChangeListener l) {
    if( l!=null )
        dc_listeners.remove(l);
    }

    /**
       Send a document init event to all listeners and reset the
       <code>changed</code> flag.
     */
    public void fireDocumentInit() {
    has_changed = false;
    for( DocumentChangeListener dcl : dc_listeners ) 
        dcl.documentInit(new DocumentChangeEvent(this));    
    }

    /**
       Send a document init event to all listeners with a different
       source and reset the <code>changed</code> flag.
     */
    public void fireDocumentInit(BaseDocument source) {
    has_changed = false;
    for( DocumentChangeListener dcl : dc_listeners ) 
        dcl.documentInit(new DocumentChangeEvent(source));    
    }
    
    /**
       Send a document changed event and reset the
       <code>changed</code> flag.
     */
    public void fireDocumentRestored() {
    has_changed = false;
    for( DocumentChangeListener dcl : dc_listeners ) 
        dcl.documentChanged(new DocumentChangeEvent(this));    
    }

    /**
       Send a document changed event and set the <code>changed</code>
       flag.
     */
    public void fireDocumentChanged() {
    	has_changed = true;
    	for( DocumentChangeListener dcl : dc_listeners ) 
    		dcl.documentChanged(new DocumentChangeEvent(this)); 
    }

    /**
       Send a document changed event with a different source and set
       the <code>changed</code> flag.
     */
    public void fireDocumentChanged(BaseDocument source) {
    has_changed = true;
    for( DocumentChangeListener dcl : dc_listeners ) 
        dcl.documentChanged(new DocumentChangeEvent(source));    
    }


    //--------------- 
    // serialization
    

    /**
       Parse the document from a string. Implementation dependent.
       @throws Exception on parsing errors
     */
    public void fromString(String str) throws Exception {
    fromString(str,false);
    }

    /**
       Parse the document from a string. Implementation dependent.
       @param merge if <code>true</code> append the content of the
       string to the document, when possible
       @throws Exception on parsing errors
     */
    abstract public void fromString(String str, boolean merge) throws Exception;

    /**
     * Some components are causing jittering motion when they are painted from scratch, 
     * seems to only occur when calling setIcon().  As a temporary solution components
     * associated with documents can be registered here - so they aren't redrawn from
     * scratch.
     */
    HashMap<String,Component> components;
    public void registerComponent(String id,Component component){
    	components.put(id,component);
    }
    
    public Component getRegisteredComponent(String id){
    	return components.get(id);
    }
}
