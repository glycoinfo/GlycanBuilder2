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

import java.util.*;
import java.io.*;
import java.awt.Color;

import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.util.SAXUtils;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;
import org.eurocarbdb.application.glycanbuilder.util.XMLUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.helpers.AttributesImpl;

/**
   This class is used to store and retrieve user settings. The
   arguments are organized by name and section. All values are stored
   as strings. The class implement a SAX handler to store and retrieve
   all the information from file.

   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

public class Configuration implements SAXUtils.SAXWriter {    
    
    protected HashMap<String,HashMap<String,String>> properties = new HashMap<String,HashMap<String,String>>();

    /**
       Read the settings from an XML file.
     */
    public boolean open(String filename) {
    try {
        if( filename==null )
        return false;
      
        File file = new File(filename); 
        if( !file.exists() )
        return false;
        
        // open document
        FileInputStream fis = new FileInputStream(file);
        /*
        Document document = XMLUtils.read(fis);
        if( document==null )
        return false;
        fromXML(XMLUtils.assertChild(document,"Configuration"));
        */
        SAXUtils.read(fis,new SAXHandler(this));        
   
        return true;
    }
    catch(Exception e) {
        LogUtils.report(e);
        return false;
    }  
    }    

    /**
       Save the settings to an XML file.
     */
    public boolean save(String filename) {    
    try {
        if( filename==null )
        return false;

        FileOutputStream fos = new FileOutputStream(filename); 

        Document document = XMLUtils.newDocument();
        document.appendChild(toXML(document));
        return XMLUtils.write(fos,document);
    }
    catch(Exception e) {
        LogUtils.report(e);
        return false;
    }
    }

    /**
       Remove an argument from the list.
     */
    public void remove(String section, String argument) {
    if( properties.containsKey(section) )
        properties.get(section).remove(argument);
    }

    /**
       Set the value of an argument.
     */
    public void put(String section, String argument, String value) {
    if( !properties.containsKey(section) )
        properties.put(section,new HashMap<String,String>());
    properties.get(section).put(argument,value);

    }

    /**
       Set the value of an argument from a list of objects.
       @param delim the character used to separate the objects in
       their string representation
     */
    public void put(String section, String argument, List<String> values, char delim) {
    put(section, argument, TextUtils.toString(values,delim));
    }

    /**
       Set the value of an argument from a list of objects.
       @param delim the character used to separate the objects in
       their string representation
     */
    public void put(String section, String argument, String[] values, char delim) {
    put(section, argument, TextUtils.toString(values,delim));
    }

    /**
       Set the value of an argument from a double.
     */
    public void put(String section, String argument, double value) {
    put(section,argument,Double.toString(value));
    }
    
    /**
       Set the value of an argument from an integer.
     */
    public void put(String section, String argument, int value) {
    put(section,argument,Integer.toString(value));
    }

    /**
       Set the value of an argument from a boolean.
     */
    public void put(String section, String argument, boolean value) {
    put(section,argument,Boolean.toString(value));
    }        

    /**
       Set the value of an argument from a {@link Color} object.
     */
    public void put(String section, String argument, Color value) {
    put(section,argument,value.getRGB());
    }

    /**
       Set the value of an argument from a generic object. Only {@link
       Color} and scalar types are supported.
     */
    public void put(String section, String argument, Object value) {
    if( value instanceof Color )
        put(section,argument,((Color)value).getRGB());
    else
        put(section,argument,value.toString());
    }
    
    /**
       Return the value of an argument.
     */
    public String get(String section, String argument) {
    if( !properties.containsKey(section) ) 
        return null;    
    return properties.get(section).get(argument);
    }

    /**
       Return the value of an argument as a list of strings.
       @param delim the character used to separate the objects in
       their string representation
       @return <code>default_value</code> if the argument was not present
     */
    public String[] get(String section, String argument, String[] default_value, char delim) {
    String str_value = get(section,argument);
    if( str_value==null )
        return default_value;
    return str_value.split("\\"+delim);
    }

    /**
       Return the value of an argument as a list of strings.
       @param delim the character used to separate the objects in
       their string representation
       @return <code>default_value</code> if the argument was not present
     */
    public List<String> get(String section, String argument, List<String> default_value, char delim) {
    String str_value = get(section,argument);
    if( str_value==null )
        return default_value;
    return new ArrayList<String>(Arrays.asList(str_value.split("\\"+delim)));
    }
  
    /**
       Return the value of an argument.
       @return <code>default_value</code> if the argument was not present
     */
    public String get(String section, String argument, String default_value) {
    String str_value = get(section,argument);
    if( str_value==null )
        return default_value;
    return str_value;
    }
    
    /**
       Return the value of an argument as a double.
       @return <code>default_value</code> if the argument was not present
     */
    public double get(String section, String argument, double default_value) {
    String str_value = get(section,argument);
    if( str_value==null )
        return default_value;
    return Double.parseDouble(str_value);        
    }

    /**
       Return the value of an argument as an integer.
       @return <code>default_value</code> if the argument was not present
     */
    public int get(String section, String argument, int default_value) {
    String str_value = get(section,argument);
    if( str_value==null )
        return default_value;
    return Integer.parseInt(str_value);        
    }

    /**
       Return the value of an argument as a boolean.
       @return <code>default_value</code> if the argument was not present
     */
    public boolean get(String section, String argument, boolean default_value) {
    String str_value = get(section,argument);
    if( str_value==null )
        return default_value;
    return Boolean.valueOf(str_value).booleanValue();
    }

    /**
       Return the value of an argument as a {@link Color} object.
       @return <code>default_value</code> if the argument was not present
     */
    public Color get(String section, String argument, Color default_value) {
    String str_value = get(section,argument);
    if( str_value==null )
        return default_value;
    return new Color(Integer.parseInt(str_value));        
    }


    /**
       Return the value of an argument as a generic object. Only
       {@link Color} and scalar types are supported.
       @return <code>default_value</code> if the argument was not present 
     */
    public Object get(String section, String argument, Object default_value) {
    String str_value = get(section,argument);
    if( str_value==null )
        return default_value;

    if( default_value instanceof String )
        return str_value;
    if( default_value instanceof Double )
        return Double.valueOf(str_value);
    if( default_value instanceof Integer )
        return Integer.valueOf(str_value);
    if( default_value instanceof Boolean )
        return Boolean.valueOf(str_value);
    if( default_value instanceof Color )
        return new Color(Integer.valueOf(str_value));
    return null;
    }
 
    /**
       Parse the settings from its XML representation as part of a
       DOM tree.
    */
    public void fromXML(Node c_node) {
    properties.clear();

    for( Node s_node : XMLUtils.findAllChildren(c_node) )
        for( Node a_node : XMLUtils.findAllChildren(s_node) ) 
        put(s_node.getNodeName(),a_node.getNodeName(),XMLUtils.getText(a_node));
    }

    /**
       Create an XML representation of this object to
       be part of a DOM tree.
    */
    public Element toXML(Document from_doc) {

    Element c_node = from_doc.createElement("Configuration");
    for( Map.Entry<String,HashMap<String,String>> s : properties.entrySet() ) {

        // create section
        Element s_node = from_doc.createElement(s.getKey());
        for( Map.Entry<String,String> a : s.getValue().entrySet() ) {
        Element a_node = from_doc.createElement(a.getKey());
        XMLUtils.setText(a_node,a.getValue());
        s_node.appendChild(a_node);
        }
        c_node.appendChild(s_node);
    }
    return c_node;
    }


    /**
       SAX handler used to read a representation of an argument from
       an XML stream.
     */
    public static class ArgumentSAXHandler extends SAXUtils.ObjectTreeHandler {
    
    public boolean isElement(String namespaceURI, String localName, String qName) {
        return true;
    }

    protected SAXUtils.ObjectTreeHandler getHandler(String namespaceURI, String localName, String qName) {
        return null;
    }

    protected Object finalizeContent(String namespaceURI, String localName, String qName) throws SAXException{
        return (object = text.toString());
    }
    } 
 

    /**
       SAX handler used to read a representation of a section from
       an XML stream.
     */
    public static class SectionSAXHandler extends SAXUtils.ObjectTreeHandler {
    
    public boolean isElement(String namespaceURI, String localName, String qName) {
        return true;
    }

    protected SAXUtils.ObjectTreeHandler getHandler(String namespaceURI, String localName, String qName) {
        return new ArgumentSAXHandler();
    }

    protected Object finalizeContent(String namespaceURI, String localName, String qName) throws SAXException{

        HashMap<String,String> arguments = new HashMap<String,String>();
        for(Map.Entry<String,LinkedList<Object>> a : sub_objects.entrySet() ) {
        if( a.getValue().size()>1 )
            throw new SAXException(createMessage("Duplicated argument"));
        arguments.put(a.getKey(),(String)a.getValue().get(0));
        }
        
        return (object = arguments);
    }
    } 

    /**
       SAX handler used to read a representation of the configuration
       from an XML stream.
     */
    public static class SAXHandler extends SAXUtils.ObjectTreeHandler {
    
    private Configuration theDocument;
    
    public SAXHandler(Configuration _doc) {
        theDocument = _doc;
    }

    public boolean isElement(String namespaceURI, String localName, String qName) {
        return qName.equals(getNodeElementName());
    }

    public static String getNodeElementName() {
        return "Configuration";
    }

    protected SAXUtils.ObjectTreeHandler getHandler(String namespaceURI, String localName, String qName) {
        return new SectionSAXHandler();
    }

    protected Object finalizeContent(String namespaceURI, String localName, String qName) throws SAXException{
        theDocument.properties.clear();
        
        for(Map.Entry<String,LinkedList<Object>> s : sub_objects.entrySet() ) {
        if( s.getValue().size()>1 )
            throw new SAXException(createMessage("Duplicated section"));
        theDocument.properties.put(s.getKey(),(HashMap<String,String>)s.getValue().get(0));
        }
        
        return (object = theDocument);
    }
    }       

    /**
       Write a representation of this object into an XML stream using
       a SAX handler.
     */
    public void write(TransformerHandler th) throws SAXException {
    th.startElement("","","Configuration",new AttributesImpl());    
    for( Map.Entry<String,HashMap<String,String>> s : properties.entrySet() ) {

        th.startElement("","",s.getKey(),new AttributesImpl());
        for( Map.Entry<String,String> a : s.getValue().entrySet() ) {
        th.startElement("","",a.getKey(),new AttributesImpl());
        th.characters(a.getValue().toCharArray(),0,a.getValue().length());
        th.endElement("","",a.getKey());
        }
        th.endElement("","",s.getKey());
    }  
    th.endElement("","","Configuration");
    }


}