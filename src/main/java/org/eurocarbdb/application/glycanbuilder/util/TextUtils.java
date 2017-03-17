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

package org.eurocarbdb.application.glycanbuilder.util;

import java.util.*;
import java.io.*;

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;

/**
   Utility class containing methods to facilitate text processing.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class TextUtils {

    private TextUtils() {}

    /**
       Return <code>true</code> if the text represent an integer
       number.
     */
    static public boolean isInteger(String text) {
    if( text==null || text.length()==0 )
        return false;
    
    try {
        Integer.parseInt(text);
        return true;
    }
    catch(Exception e) {
        return false;
    }
    }

    /**
       Return <code>true</code> if the text represent a positive
       integer number.
     */
    static public boolean isPositiveInteger(String text) {
    if( text==null || text.length()==0 )
        return false;
    
    try {
        return (Integer.parseInt(text)>=0);
    }
    catch(Exception e) {
        return false;
    }
    }

    /**
       Return a string containing all the data read from a stream.
     */
    static public String consume(InputStream is) {
    try {
        BufferedInputStream bis = new BufferedInputStream(is);
        
        int read;
        StringBuilder str = new StringBuilder();
        while( (read = bis.read())!=-1 ) 
        str.append((char)read);

        return str.toString();
    }
    catch(Exception e) {
        LogUtils.report(e);
        return "";
    }
    }    

    /**
       Invert the order of the character in a string and return the
       result.
     */
    static public String invert(String str) {
    if( str==null || str.length()==0 )
        return str;

    StringBuilder ret = new StringBuilder();
    for( int i=str.length()-1; i>=0; i-- ) 
        ret.append(str.charAt(i));    
    
    return ret.toString();
    }

    /**
       Remove a character from a string and return the result.
     */
    static public String delete(String str, char c) {    
    if( str==null || str.length()==0 )
        return str;

    StringBuilder ret = new StringBuilder();
    for( int i=0; i<str.length(); i++ ) {
        if( str.charAt(i)!=c ) 
        ret.append(str.charAt(i));
    }
    
    return ret.toString();
    }

    /**
       Remove the instances of a certain character from the beginning
       and end of a string and return the result.
     */
    static public String squeeze(String str, char c)   {
    if( str==null || str.length()==0 )
        return str;

    int start, end;
    for( start = 0; start<str.length() && str.charAt(start)==c; start++);        
    if( start==str.length() )
        return "";
    for( end = str.length(); end>0 && str.charAt(end-1)==c; end--);            
    return str.substring(start,end);
    }
    

    /**
       Delete all repeated instances of a certain character in a
       string and return the result.
     */
    static public String squeezeAll(String str, char c) {
    if( str==null || str.length()==0 )
        return str;

    StringBuilder ret = new StringBuilder();

    char last_char = 0;
    for( int i=0; i<str.length(); i++ ) {
        if( i==0 || str.charAt(i)!=c || str.charAt(i)!=last_char ) 
        ret.append(str.charAt(i));
        last_char = str.charAt(i);
    }
    
    return ret.toString();
    }

    /**
       Remove spacing characters from the beginning and the end of a
       string and return the result.
     */
    static public String trim(String str) 
    {    
    if( str==null || str.length()==0 )
        return str;
    
    str = squeeze(str, ' ');
    str = squeeze(str, '\t');
    str = squeeze(str, '\n');
    str = squeeze(str, '\r');

    return str;
    }    

    /**
       Split a string in a list of tokens delimited by a specified
       character.
       @param delims the list of characters to be used as delimiters
       @return the list of tokens
     */
    static public LinkedList<String> tokenize(String str, String delims) {
    	LinkedList<String> out = new LinkedList<String>();
    	if( str==null || str.length()==0 || delims==null || delims.length()==0 )
    		return out;

    	StringBuilder token = new StringBuilder(str.length());           
    	for (int i = 0; i < str.length(); i++) {
    		if( delims.indexOf(str.charAt(i))!=-1 ) {
    			if( token.length()>0 ) { 
    				out.addLast(token.toString());
    				token = new StringBuilder(str.length());
    			}
    		}
    		else token.append(str.charAt(i));
    	}
    	if( token.length()>0 ) out.addLast(token.toString());

    	return out;
    }

    /**
       Split a string in a list of tokens delimited by a specified
       character. Keep together the parts of the string inside
       parenthesis.
       @param delims the list of characters to be used as delimiters
       @param open_par the character to be used as open parenthesis
       @param closed_par the character to be used as closed parenthesis
       @return the list of tokens
     */
    static public Vector<String> tokenize(String str, String delims, char open_par, char closed_par) {
    Vector<String> out = new Vector<String>();
    if( str==null || str.length()==0 || delims==null || delims.length()==0 )
        return out;

    StringBuilder token = new StringBuilder(str.length());           
    for (int i = 0; i < str.length(); i++) {
        if( str.charAt(i)==open_par ) {
        token.append(str.charAt(i));        
        for( ++i; i<str.length() && str.charAt(i)!=closed_par; i++ )
            token.append(str.charAt(i));        
        if( i<str.length() )            
            token.append(str.charAt(i));        
        }
        else {
        if( delims.indexOf(str.charAt(i))!=-1 ) {
            if( token.length()>0 ) { 
            out.addElement(token.toString());
            token = new StringBuilder(str.length());
            }
        }
        else {
            token.append(str.charAt(i));
        }
        }
    }
    if( token.length()>0 ) 
        out.addElement(token.toString());

    return out;
    }
    
    /**
       Split a string using the newline character as delimiter.
       @return the list of tokens in which the string was split
     */
    static public Vector<String> splitLines(String str) {

    Vector<String> v = new Vector<String>();
    if( str==null || str.length()==0 )
        return v;

    BufferedReader br = new BufferedReader(new StringReader(str));

    String line;
    try {
        while ((line = br.readLine()) != null) {
        v.addElement(line);
        }
    } catch (IOException ex) {
        LogUtils.report(ex);
    }
    return v;
    }
        
        
    /**
       Return the index of the closed parenthesis. Multiple open
       parenthesis are considered. 
       @return <code>null</code> if the string does not start with an
       open parenthesis.
    */
    static public int findEnclosed(String str) {
    return findEnclosed(str,0,'(',')');
    }

    /**
       Return the index of the closed parenthesis. Multiple open
       parenthesis are considered. 
       @param start the index from which to start the search
       @param open_par the character that represent an open parenthesis
       @param closed_par the character that represent a closed parenthesis
       @return <code>null</code> if the string does not start with an
       open parenthesis.
    */
    static public int findEnclosed(String str, int start, char open_par, char closed_par) {
    if( str==null || str.length()==0 )
        return -1;
    if( str.charAt(start)!=open_par )
        return -1;
    return findClosedParenthesis(str,start+1,open_par,closed_par);
    }
    
    /**
       Return the index of the first unmatched closed
       parenthesis. Multiple open parenthesis are considered.
       @return <code>null</code> if the string does not start with an
       open parenthesis.
    */
    static public int findClosedParenthesis(String str) {
    return findClosedParenthesis(str,0,'(',')');
    }

    /**
       Return the index of the first unmatched closed
       parenthesis. Multiple open parenthesis are considered.
       @param start_from the index from which to start the search
       @return <code>null</code> if the string does not start with an
       open parenthesis.
    */
    static public int findClosedParenthesis(String str, int start_from) {
    return findClosedParenthesis(str,start_from,'(',')');
    }

    /**
       Return the index of the first unmatched closed
       parenthesis. Multiple open parenthesis are considered.
       @param start_from the index from which to start the search
       @param open_par the character that represent an open parenthesis
       @param closed_par the character that represent a closed parenthesis      
       @return <code>null</code> if the string does not start with an
       open parenthesis.
    */
    static public int findClosedParenthesis(String str, int start_from, char open_par, char closed_par) {
    if( str==null || str.length()==0 )
        return -1;

    int nopen = 0;
    for (int i = start_from; i < str.length(); i++) {
        if( str.charAt(i)==closed_par ) {
        if( nopen==0 )            
            return i;
        nopen--;
        }
        else if( str.charAt(i)==open_par )
        nopen++;
    }
    return -1;
    }

    /**
       Return the index of the closed parenthesis. Multiple open
       parenthesis are considered. The string is read inverted from
       the end to the beginning.
       @param start the index from which to start the search
       @param open_par the character that represent an open parenthesis
       @param closed_par the character that represent a closed parenthesis
       @return <code>null</code> if the string does not start with an
       open parenthesis.
    */
    static public int findEnclosedInvert(String str, int start, char open_par, char closed_par) throws Exception {
    if( str==null || str.length()==0 )
        return -1;
    if( str.charAt(start)!=closed_par )
        return -1;
    return findClosedParenthesisInvert(str,start-1,open_par,closed_par);
    }

    /**
       Return the index of the first unmatched closed parenthesis. Multiple open
       parenthesis are considered. The string is read inverted from
       the end to the beginning.
       @param start_from the index from which to start the search
       @param open_par the character that represent an open parenthesis
       @param closed_par the character that represent a closed parenthesis
       @return <code>null</code> if the string does not start with an
       open parenthesis.
    */
    static public int findClosedParenthesisInvert(String str, int start_from, char open_par, char closed_par) {
    if( str==null || str.length()==0 )
        return -1;
    int nopen = 0;
    for (int i = start_from; i>=0; i--) {
        if( str.charAt(i)==open_par ) {
        if( nopen==0 )            
            return i;
        nopen--;
        }
        else if( str.charAt(i)==closed_par )
        nopen++;
    }
    return -1;
    }
 
    /**
       Remove the matching open and closed parenthesis at the begining
       and the end of the string. The process is repeated until there
       are no more matching parenthesis at the extremities of the
       string. 
     */
    static public String removeTrailingParentheses(String str) {

    if( str==null || str.length()==0 )
        return str;

    String ret = str;
    try {
        while( ret.startsWith("(") && ret.endsWith(")") && findClosedParenthesis(ret,1)==(ret.length()-1) )
        ret = ret.substring(1,ret.length()-1);
    }
    catch(Exception e) {
        LogUtils.report(e);
    }
    return ret;
    }

    /**
       Return the first index in the string of any one of the
       specified characters
     */
    static public int findFirstOf(String str, String chars) {
    if( str==null || str.length()==0 || chars==null || chars.length()==0 )
        return -1;

    for (int i = 0; i < str.length(); i++) {
        if( chars.indexOf(str.charAt(i))!=-1 ) 
        return i;
    }
    return -1;
    }

    /**
       Return the first index in the string of any one of the
       specified characters. Ignore the content inside parenthesis.
     */
    static public int findFirstOfWithParentheses(String str, String chars) throws Exception {
    if( str==null || str.length()==0 || chars==null || chars.length()==0 )
        return -1;

    for (int i = 0; i < str.length(); i++) {
        if( str.charAt(i)=='(' ) {
        i =  findClosedParenthesis(str,i+1);
        if( i==-1 ) 
            throw new Exception("Unmatched parenthesis in : " + str);        
        }
        if( chars.indexOf(str.charAt(i))!=-1 ) 
        return i;
    }
    return -1;
    }

    /**
       Return a string representation of an integer array. 
       @param delim the character to be used as delimiter between the
       array's elements
     */
    static public String toString(int[] v, char delim) {
    if( v==null )
        return "";

    StringBuilder strbuf = new StringBuilder();
    for( int i=0; i<v.length; i++ ) {
        if( i>0 )
        strbuf.append(delim);
        strbuf.append(v[i]);        
    }
    return strbuf.toString();
    }
    
    /**
       Return a string representation of an object array. 
       @param delim the character to be used as delimiter between the
       array's elements
     */
    static public String toString(Object[] v, char delim) {
    if( v==null )
        return "";

    StringBuilder strbuf = new StringBuilder();
    for( int i=0; i<v.length; i++ ) {
        if( i>0 )
        strbuf.append(delim);
        strbuf.append(v[i].toString());        
    }
    return strbuf.toString();
    }

    /**
       Return a string representation of a list. 
       @param delim the character to be used as delimiter between the
       list's elements
     */
    static public String toString(Collection<? extends Object> v, char delim) {
    if( v==null )
        return "";

    StringBuilder strbuf = new StringBuilder();
    for( Object o : v ) {
        if( strbuf.length()>0 )
        strbuf.append(delim);
        strbuf.append(o.toString());        
    }
    return strbuf.toString();
    }

    /**
       Return the unicode representation of a letter in the greek
       alphabet, where 'a' correspond to 'alpha', 'b' to 'beta' and so
       on
     */
    static public String toGreek(char c) {
    StringBuilder txt = new StringBuilder();
    if( Character.isLetter(c) )
        txt.appendCodePoint(945+c-'a');
    else
        txt.append(c);
    return txt.toString();
    }
}

