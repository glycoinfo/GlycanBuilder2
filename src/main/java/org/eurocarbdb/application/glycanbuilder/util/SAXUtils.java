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

import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*; 

/**
   Utility class with functions to facilitate the parsing of XML files
   using the SAX architecture

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class SAXUtils {

    /**
       Extension of a SAX handler that parses an XML representation
       into an object tree. The nested elements represent members of
       the current object. This default implementation discard all the
       content of the element and should be extended by other classes.
     */
    public static class ObjectTreeHandler extends DefaultHandler {
    
    // singletons
    protected Locator theLocator;
    protected LinkedList<ObjectTreeHandler> handlers_stack;
  
    // content
    protected Object object;
    protected StringBuilder text;
    protected HashMap<String,LinkedList<Object>> sub_objects;

    // construction
    
    /**
       Default constructor.
     */
    public ObjectTreeHandler() {
        theLocator = null;
        handlers_stack = new LinkedList<ObjectTreeHandler>();

        object = null;
        text = new StringBuilder();
        sub_objects  = new HashMap<String,LinkedList<Object>>();
    }

    // methods
    
    /**
       Return the text parsed from the element.
     */
    public String getText() {
        return text.toString();
    }

    /**
       Return the object parsed from the XML stream
    */
    public Object getObject() {
        return object;
    }

    /**
       Return <code>true</code> if this handler should be used to
       parse the current XML element.
       @see DefaultHandler#startElement
     */
    protected boolean isElement(String namespaceURI, String localName, String qName) {
        return false;
    }
    
    /**
       Return the handler that should be used to parse a nested
       XML element.
       @see DefaultHandler#startElement
     */
    protected ObjectTreeHandler getHandler(String namespaceURI, String localName, String qName) throws SAXException {
        return null;
    }

    /**
       Initialize the content of this handler
       @see DefaultHandler#startElement
     */
    protected void initContent(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException  {
        object = null;
        text = new StringBuilder();
        sub_objects  = new HashMap<String,LinkedList<Object>>();
    }

    /**
       Add a string to the text content of this handler
       @see DefaultHandler#characters
     */
    protected void addText(char[] buf, int start, int length) throws SAXException {
        for( int i=0; i<length; i++ )
        text.append(buf[start+i]);
    }

    /**
       Add a white space to the text content of this handler
       @see DefaultHandler#characters
     */
    protected void addWhiteSpace() throws SAXException{
        text.append(' ');
    }
    
    /**
       Add a sub object with a given name
     */
    protected void addObject(String name, Object o) {
        if( o!=null ) {
        LinkedList<Object> v = sub_objects.get(name);
        if( v==null )
            sub_objects.put(name,v = new LinkedList<Object>());
        v.add(o);
        }
    }
    
    /**
       Return the sub object with the specified name. 
       @param assert_non_null <code>true</code> if the method
       should throw an exception if the object is not found
       @return <code>null</code> if an object with that name is
       not found
       @throws Exception if assert_non_null is <code>true</code>
       and no object with the specified name is found
     */
    protected Object getSubObject(String name, boolean assert_non_null) throws SAXException {
        LinkedList<Object> v = sub_objects.get(name);
        if( v==null ) {
        if( assert_non_null )
            throw new SAXException(createMessage("Cannot find object with name " + name));        
        return null;
        }

        if( v.size()>1 )
        throw new SAXException(createMessage("Multiple objects with name " + name));        
        
        return v.get(0);
    }

    /**
       Return the sub object with the specified name. 
       @param default_value the value to be returned if an object
       with the specified name is not found
     */
    protected Object getSubObject(String name, Object default_value) throws SAXException {
        LinkedList<Object> v = sub_objects.get(name);
        if( v==null ) 
        return default_value;
        if( v.size()>1 )
        throw new SAXException(createMessage("Multiple objects with name " + name));        
        return v.get(0);
    }

    /**
       Return the list of all objects parsed from the nested
       elements.
     */
    protected LinkedList<Object> getSubObjects(String name) {
        LinkedList<Object> v = sub_objects.get(name);
        if( v==null ) 
        return new LinkedList<Object>();
        return v;
    }

    /**
       Finalize the content of this handler
       @see DefaultHandler#endElement
     */
    protected Object finalizeContent(String namespaceURI, String localName, String qName) throws SAXException {
        return object;
    }


    // helpers
    
    protected String stringAttribute(Attributes atts, String name, String default_value) {
        if( atts==null || name==null )
        return default_value;
        if( atts.getValue(name)==null )
        return default_value;
        return atts.getValue(name);
    }

    protected String stringAttribute(Attributes atts, String name) {
        return stringAttribute(atts,name,null);
    }

    protected Double doubleAttribute(Attributes atts, String name, Double default_value) {
        if( atts==null || name==null )
        return default_value;
        if( atts.getValue(name)==null )
        return default_value;
        return Double.valueOf(atts.getValue(name));
    }

    protected Double doubleAttribute(Attributes atts, String name) {
        return doubleAttribute(atts,name,null);
    }

    protected Integer integerAttribute(Attributes atts, String name, Integer default_value) {
        if( atts==null || name==null )
        return default_value;
        if( atts.getValue(name)==null )
        return default_value;
        return Integer.valueOf(atts.getValue(name));
    }

    protected Integer integerAttribute(Attributes atts, String name) {
        return integerAttribute(atts,name,null);
    }

    protected Boolean booleanAttribute(Attributes atts, String name, Boolean default_value) {
        if( atts==null || name==null )
        return default_value;
        if( atts.getValue(name)==null )
        return default_value;
        return Boolean.valueOf(atts.getValue(name));
    }

    protected Boolean booleanAttribute(Attributes atts, String name) {
        return booleanAttribute(atts,name,null);
    }

    protected String createMessage(String message) {
        if( theLocator==null )
        return message;
        return (message + " at line " + theLocator.getLineNumber() + " column " + theLocator.getColumnNumber());
    }
    
    protected String createMessage(Exception e) {
        if( e==null )
        return createMessage("Exception");
        return createMessage(e.getMessage());
    }
        

    
    // events     
    public void startDocument() throws SAXException {
        if( handlers_stack.size()!=0 )
        throw new SAXException(createMessage("Unexpected start of document"));
        pushHandler(new DocumentHandler(this));
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if( handlers_stack.size()==0 ) 
        new SAXException(createMessage("Unexpected start of element"));

        // get handler for this element
        ObjectTreeHandler handler = currentHandler().getHandler(namespaceURI,localName,qName);
        if( handler==null )
        handler = new ObjectTreeHandler();

        // init content
        handler.initContent(namespaceURI,localName,qName,atts);
        
        // push into stack
        pushHandler(handler);
    }
    
    public void characters(char[] ch, int start, int length) throws SAXException {
        if( handlers_stack.size()==0 ) 
        new SAXException(createMessage("Unexpected character content"));
        
        currentHandler().addText(ch,start,length);
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if( handlers_stack.size()==0 ) 
        new SAXException(createMessage("Unexpected character content"));
        currentHandler().addWhiteSpace();
    }
    
    public void endElement(String namespaceURI, String localName, String qName)  throws SAXException  {
        if( handlers_stack.size()==0 ) 
        new SAXException(createMessage("Unexpected end of element"));
        
        // build object
        Object sub_object = currentHandler().finalizeContent(namespaceURI,localName,qName);
        
        // remove this handler from stack
        popHandler();
        
        // add sub object the content of the parent
        if( sub_object!=null )
        currentHandler().addObject(qName,sub_object);    
    }
    
    public void endDocument() throws SAXException {
        if( handlers_stack.size()==0 ) 
        new SAXException(createMessage("Unexpected end of document"));
        popHandler();    
    }
    
    public void setDocumentLocator(org.xml.sax.Locator locator) {
        theLocator = locator;
    }    
    

    // internal methods    
    
    private void pushHandler(ObjectTreeHandler handler) throws SAXException{
        if( handler==null )
        throw new SAXException(createMessage("Invalid null handler"));
        handlers_stack.addFirst(handler);
    }
    
    private void popHandler() throws SAXException { 
        if( handlers_stack.size()==0 )
        throw new SAXException(createMessage("Empty stack"));
        handlers_stack.removeFirst();
    }
    
    private ObjectTreeHandler currentHandler() throws SAXException {
        if( handlers_stack.size()==0 )
        throw new SAXException(createMessage("Empty stack"));
        return handlers_stack.getFirst();
    }

    }
  
    
    private static class DocumentHandler extends ObjectTreeHandler {
    
    private ObjectTreeHandler root_handler = null;
    
    public DocumentHandler(ObjectTreeHandler _root) {
        root_handler = _root;
    }
    
    public ObjectTreeHandler getHandler(String namespaceURI, String localName, String qName) {
        if( root_handler!=null && root_handler.isElement(namespaceURI,localName,qName) )
        return root_handler;
        return null;
    }
    }        

    /**
       Interface that should be implemented by every object that can
       write on a SAX stream.
     */
    public interface SAXWriter {
    /** Method that is called to write the information represented
        by this object into an XML document using the specified
        handler*/
    public void write(TransformerHandler handler) throws SAXException;        
    }

    private SAXUtils() {}
    
    /**
       Parse a stream in XML format using the specified handler
       @throws Exception on errors
     */
    public static void read(InputStream is, DefaultHandler h) throws Exception {
    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
    parser.parse(is,h);
    }
    
    /**
       Write to a stream in XML format using the specified writer
       @throws Exception on errors
     */
    public static void write(OutputStream os, SAXWriter w) throws Exception {    

    // init handler
    StreamResult streamResult = new StreamResult(os);
    SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    TransformerHandler hd = tf.newTransformerHandler();    
    Transformer serializer = hd.getTransformer();
    serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
    serializer.setOutputProperty(OutputKeys.INDENT,"yes");
    hd.setResult(streamResult);
 
    // write document
    hd.startDocument();
    w.write(hd);
    hd.endDocument();
    }


}
