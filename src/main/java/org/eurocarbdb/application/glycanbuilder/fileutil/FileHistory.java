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

package org.eurocarbdb.application.glycanbuilder.fileutil;

import java.io.*;
import java.util.*;

import javax.swing.*;

import org.eurocarbdb.application.glycanbuilder.Configuration;
import org.eurocarbdb.application.glycanbuilder.FileUtils;

/**
   Manage the recent file history. Other classes can register to
   listen for changes to the history. The history mantains only the 8
   most recent files.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class FileHistory {
    
    // classes    

    /**
       Listener for events raised when the recent files history is
       changed.
     */
    public interface Listener {

    /**
       Called when the file history has changed.
     */
    public void fileHistoryChanged();
    }


    // constants 

    static private final int MAX_ITEM_LEN = 50;      
    static private final String FILE_SEPARATOR_STR = System.getProperty("file.separator");

    // members

    protected String recent_folder;
    protected LinkedList<String> recent_files;    
    protected HashMap<String,String> file_types;
    protected Vector<FileHistory.Listener> listeners;

    // methods

    /**
       Default constructor.
     */
    public FileHistory() {
    recent_folder = null;
    recent_files = new LinkedList<String>();
    file_types = new HashMap<String,String>();
    listeners = new Vector<FileHistory.Listener>();
    }    

    /**
       Register a new listener for changes to the recent files history
     */
    public void addHistoryChangedListener(FileHistory.Listener l) {
    if( l!=null )
        listeners.add(l);
    }

    
    /**
       Deregister a listener for changes to the recent files history
     */
    public void removeHistoryChangedListener(FileHistory.Listener l) {
    if( l!=null )
        listeners.remove(l);
    }

    /**
       Clear the history
     */
    public void clear() {
    recent_folder = null;
    recent_files.clear();
    file_types.clear();
    }

    
    /**
       Add a new file to the history.
       @param filename the path to the recently accessed file
       @see #add(File,String)
     */
    public void add(String filename) {
    if( filename!=null ) 
        add(new File(filename),"");        
    }

    /**
       Add a new file to the history.
       @param filename the path to the recently accessed file
       @param type the type of the recently accessed file
       @see #add(File,String)
     */
    public void add(String filename, String type) {
    if( filename!=null )
        add(new File(filename),type);
    }

    /**
       Add a new file to the history.
       @param file the path to the recently accessed file
       @see #add(File,String)
     */
    public void add(File file) {
    add(file,"");
    }

    /**
       Add a new file to the history. Update the most recent
       folder. Send an event to the registered listeners.
       @param file the path to the recently accessed file
       @param type the type of the recently accessed file
     */
    public void add(File file, String type) {
    if( file!=null ) {  
        // update folder
        recent_folder = file.getParentFile().getAbsolutePath();

        // update list
        String file_path = file.getAbsolutePath();
        recent_files.remove(file_path);
        recent_files.addFirst(file_path);
        file_types.put(file_path,type);        
        if( recent_files.size()>8 )
        file_types.remove(recent_files.removeLast());        
        
        // fire event
        for(Iterator<FileHistory.Listener> i=listeners.iterator(); i.hasNext(); ) 
        i.next().fileHistoryChanged();
    }
    }

    /**
       Remove a file from the history. Send an event to the registered listeners.
       @param filename the path to the file to be removed from the history
     */
    public void remove(String filename) {
    if( filename!=null )
        remove(new File(filename));
    }

    /**
       Remove a file from the history. Send an event to the registered listeners.
       @param file the path to the file to be removed from the history
     */
    public void remove(File file) {
    if( file!=null ) {  
        // update list
        String file_path = file.getAbsolutePath();
        recent_files.remove(file_path);
        file_types.remove(file_path);

        // fire event
        for(Iterator<FileHistory.Listener> i=listeners.iterator(); i.hasNext(); ) 
        i.next().fileHistoryChanged();
    }
    }

    /**
       Return the path to the folder containing the most recently
       accessed file. If no file has been added to the history yet the
       root dir of the application is returned.
     */
    public File getRecentFolder() {
    if( recent_folder==null )
        return new File(System.getProperty("user.home"));
    return new File(recent_folder);
    }

    /**
       Return the list of the recently accessed files.
     */
    public List<String> getRecentFiles() {
    return recent_files;
    }
    
    /**
       Return an iterator over the list of the recently accessed files.
     */
    public Iterator<String> iterator() {
    return recent_files.iterator();
    }
   
    
    /**
       Return the type associated to one of the file in the history
     */
    public String getFileType(String pathname) {
    return file_types.get(pathname);
    }

    /**
       Create an abbreviated form of a long file path to be used in
       menus.
     */
    static public String getAbbreviatedName(String pathname) {

    final char FILE_SEPARATOR = FILE_SEPARATOR_STR.charAt(0);
    final int pathnameLen = pathname.length();

    // if the path is a subdir of running dir remove cwd
    String cwd_path = FileUtils.getRootDir();
    if( pathname.startsWith(cwd_path) )
        return pathname.substring(cwd_path.length()+1);

    // if the pathame is short enough: return whole pathname
    if (pathnameLen <= MAX_ITEM_LEN) {
        return pathname;           
    }
    
    // if we have only one directory: return whole pathname
    if (pathname.indexOf(FILE_SEPARATOR_STR) == pathname.lastIndexOf(FILE_SEPARATOR_STR)) {
        return pathname;
    }

    // abbreviate pathanme: Windows OS like solution
    final int ABBREVIATED_PREFIX_LEN = (FILE_SEPARATOR_STR.length()==1) ?5 :7; // e.g.: /.../ or C:\...\        
    final int MAX_PATHNAME_LEN = MAX_ITEM_LEN - ABBREVIATED_PREFIX_LEN;         

    int firstFileSeparatorIndex = 0;
    for (int i=pathnameLen-1; i>=(pathnameLen-MAX_PATHNAME_LEN); i--) {
            if (pathname.charAt(i) == FILE_SEPARATOR) {
        firstFileSeparatorIndex = i;
            }
    }
    if (firstFileSeparatorIndex > 0) {
            return pathname.substring(0, ABBREVIATED_PREFIX_LEN-4) + "..." + pathname.substring(firstFileSeparatorIndex, pathnameLen);
    }
        
    return pathname.substring(0, ABBREVIATED_PREFIX_LEN-4) + "..." + FILE_SEPARATOR_STR + ".." + pathname.substring(pathnameLen-MAX_PATHNAME_LEN, pathnameLen);    
    }

    public void store(Configuration config) {
    for( int c=0; c<8; c++ ) {
        config.put("FileHistory", "file_path" + c, "");
        config.put("FileHistory", "file_type" + c, "");
    }

    int c=0;
    for( String file_path : recent_files) {
        config.put("FileHistory", "file_path" + c, file_path);
        config.put("FileHistory", "file_type" + c, getFileType(file_path));
        c++;
    }
    }

    public void retrieve(Configuration config) {    
    clear();
    
    for( int c=0; c<8; c++ ) {
        String file_path = config.get("FileHistory", "file_path" + c);
        String file_type = config.get("FileHistory", "file_type" + c);
        if( file_path!=null && file_path.length()>0 ) {
        recent_files.addLast(file_path);
        file_types.put(file_path,file_type);
        }
    }
    }
    
}