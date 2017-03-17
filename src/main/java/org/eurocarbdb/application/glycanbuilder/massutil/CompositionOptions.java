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

package org.eurocarbdb.application.glycanbuilder.massutil;

import org.eurocarbdb.application.glycanbuilder.Configuration;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;



/**
   Class that contains the option values to create a new glycan
   composition object.
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/


public class CompositionOptions {
    

    /** Identifier for the first generic residue */
    public String OR1_NAME = "Or1";

    /** Identifier for the second generic residue */
    public String OR2_NAME = "Or2";

    /** Identifier for the third generic residue */
    public String OR3_NAME = "Or3";

    /** Mass of the first generic residue */
    public double OR1_MASS = 0.;

    /** Mass of the second generic residue */
    public double OR2_MASS = 0.;

    /** Mass of the third generic residue */
    public double OR3_MASS = 0.;

    /** Number of pentoses */
    public int PEN=0;

    /** Number of hexoses */
    public int HEX=0;

    /** Number of heptoses */
    public int HEP=0;

    /** Number of hexosamines */
    public int HEXN=0;

    /** Number of n-acetyl hexosamines */
    public int HEXNAC=0;

    /** Number of deoxy-pentoses */
    public int DPEN=0;

    /** Number of deoxy-hexoses */
    public int DHEX=0;

    /** Number of dideoxy-hexoses */
    public int DDHEX=0;

    /** Number of metyl-hexoses */
    public int MEHEX=0;

    /** Number of instances of the first generic residue */
    public int OR1=0;

    /** Number of instances of the second generic residue */
    public int OR2=0;

    /** Number of instances of the third generic residue */
    public int OR3=0;

    /** Number of hexuronic acids */
    public int HEXA=0;

    /** Number of deoxy hexuronic acids */
    public int DHEXA=0;

    /** Number of n-glycolyl neuraminic acids */
    public int NEU5GC=0;

    /** Number of n-acetyl neuraminic acids */
    public int NEU5AC=0;

    /** Number of lactonised n-glycolyl neuraminic acids */
    public int NEU5GCLAC=0;

    /** Number of lactonised n-acetyl neuraminic acids */
    public int NEU5ACLAC=0;

    /** Number of Kdo residues */
    public int KDO=0;

    /** Number of Kdn residues */
    public int KDN=0;

    /** Number of muramic acids */
    public int MUR=0;

    /** Number of sulfates */
    public int S=0;

    /** Number of phosphates */
    public int P=0;

    /** Number of acetyl groups */
    public int AC=0;

    /** Number of pyruvates */
    public int PYR=0;

    /** Number of phosphocholines */
    public int PC=0; 
    
    /** Number of ketose */
    public int KETOSE = 0;
    
    /** Number of Unknown */
    public int UNKNOWN = 0;
    
    // serialization    

    /**
       Store the option values into a configuration object.
     */
    public void store(Configuration config) {

    config.put("CompositionOptions","pen",PEN);    
    config.put("CompositionOptions","hex",HEX);    
    config.put("CompositionOptions","hep",HEP);    
    config.put("CompositionOptions","hexn",HEXN);    
    config.put("CompositionOptions","hexnac",HEXNAC);    
    config.put("CompositionOptions","dpen",DPEN);    
    config.put("CompositionOptions","dhex",DHEX);    
    config.put("CompositionOptions","ddhex",DDHEX);    
    config.put("CompositionOptions","mehex",MEHEX);    

    config.put("CompositionOptions","or1",OR1);    
    config.put("CompositionOptions","or2",OR2);    
    config.put("CompositionOptions","or3",OR3);    
    config.put("CompositionOptions","or1_mass",OR1_MASS);    
    config.put("CompositionOptions","or2_mass",OR2_MASS);    
    config.put("CompositionOptions","or3_mass",OR3_MASS);    

    config.put("CompositionOptions","hexa",HEXA);    
    config.put("CompositionOptions","dhexa",DHEXA);    
    config.put("CompositionOptions","neu5gc",NEU5GC);    
    config.put("CompositionOptions","neu5ac",NEU5AC);    
    config.put("CompositionOptions","neu5gclac",NEU5GCLAC);    
    config.put("CompositionOptions","neu5aclac",NEU5ACLAC);    
    config.put("CompositionOptions","kdp",KDO);    
    config.put("CompositionOptions","kdn",KDN);    
    config.put("CompositionOptions","mur",MUR);    

    config.put("CompositionOptions","s",S);    
    config.put("CompositionOptions","p",P);    
    config.put("CompositionOptions","ac",AC);    
    config.put("CompositionOptions","pyr",PYR);    
    config.put("CompositionOptions","pc",PC);  
    
    config.put("CompositionOptions","ketose",KETOSE);
    config.put("CompositionOptions","Unknown",UNKNOWN);
    }


    /**
       Retrieve the option values from a configuration object.
     */  
    public void retrieve(Configuration config) {

    PEN = config.get("CompositionOptions","pen",PEN);    
    HEX = config.get("CompositionOptions","hex",HEX);    
    HEP = config.get("CompositionOptions","hep",HEP);    
    HEXN = config.get("CompositionOptions","hexn",HEXN);    
    HEXNAC = config.get("CompositionOptions","hexnac",HEXNAC);    
    DPEN = config.get("CompositionOptions","dpen",DPEN);    
    DHEX = config.get("CompositionOptions","dhex",DHEX);    
    DDHEX = config.get("CompositionOptions","ddhex",DDHEX);    
    MEHEX = config.get("CompositionOptions","mehex",MEHEX);    

    OR1 = config.get("CompositionOptions","or1",OR1);    
    OR2 = config.get("CompositionOptions","or2",OR2);    
    OR3 = config.get("CompositionOptions","or3",OR3);    
    OR1_MASS = config.get("CompositionOptions","or1_mass",OR1_MASS);    
    OR2_MASS = config.get("CompositionOptions","or2_mass",OR2_MASS);    
    OR3_MASS = config.get("CompositionOptions","or3_mass",OR3_MASS);    

    HEXA = config.get("CompositionOptions","hexa",HEXA);    
    DHEXA = config.get("CompositionOptions","dhexa",DHEXA);    
    NEU5GC = config.get("CompositionOptions","neu5gc",NEU5GC);    
    NEU5AC = config.get("CompositionOptions","neu5ac",NEU5AC);    
    NEU5GCLAC = config.get("CompositionOptions","neu5gclac",NEU5GCLAC);    
    NEU5ACLAC = config.get("CompositionOptions","neu5aclac",NEU5ACLAC);    
    KDO = config.get("CompositionOptions","kdp",KDO);    
    KDN = config.get("CompositionOptions","kdn",KDN);    
    MUR = config.get("CompositionOptions","mur",MUR);    

    S = config.get("CompositionOptions","s",S);    
    P = config.get("CompositionOptions","p",P);    
    AC = config.get("CompositionOptions","ac",AC);    
    PYR = config.get("CompositionOptions","pyr",PYR);    
    PC = config.get("CompositionOptions","pc",PC);    
    
    KETOSE = config.get("CompositionOptions","ketose",KETOSE);
    UNKNOWN = config.get("CompositionOptions", "Unknown",UNKNOWN);
    }

    public Glycan getCompositionAsGlycan(MassOptions mopt) throws Exception {
    
    Glycan ret = Glycan.createComposition(mopt);

    for( int i=0; i<PEN; i++ ) ret.addAntenna(ResidueDictionary.newResidue("Pen"));
    for( int i=0; i<HEX; i++ ) ret.addAntenna(ResidueDictionary.newResidue("Hex"));
    for( int i=0; i<HEP; i++ ) ret.addAntenna(ResidueDictionary.newResidue("Hep"));
    for( int i=0; i<HEXN; i++ ) ret.addAntenna(ResidueDictionary.newResidue("HexN"));
    for( int i=0; i<HEXNAC; i++ ) ret.addAntenna(ResidueDictionary.newResidue("HexNAc"));
    for( int i=0; i<DPEN; i++ ) ret.addAntenna(ResidueDictionary.newResidue("dPen"));
    for( int i=0; i<DHEX; i++ ) ret.addAntenna(ResidueDictionary.newResidue("dHex"));
    for( int i=0; i<DDHEX; i++ ) ret.addAntenna(ResidueDictionary.newResidue("ddHex"));
    for( int i=0; i<MEHEX; i++ ) ret.addAntenna(ResidueDictionary.newResidue("MeH"));

    for( int i=0; i<HEXA; i++ ) ret.addAntenna(ResidueDictionary.newResidue("HexA"));
    for( int i=0; i<DHEXA; i++ ) ret.addAntenna(ResidueDictionary.newResidue("dHexA"));
    for( int i=0; i<NEU5GC; i++ ) ret.addAntenna(ResidueDictionary.newResidue("NeuGc"));
    for( int i=0; i<NEU5AC; i++ ) ret.addAntenna(ResidueDictionary.newResidue("NeuAc"));
    for( int i=0; i<NEU5GCLAC; i++ ) ret.addAntenna(ResidueDictionary.newResidue("NeuGcLac"));
    for( int i=0; i<NEU5ACLAC; i++ ) ret.addAntenna(ResidueDictionary.newResidue("NeuAcLac"));
    for( int i=0; i<KDO; i++ ) ret.addAntenna(ResidueDictionary.newResidue("KDO"));
    for( int i=0; i<KDN; i++ ) ret.addAntenna(ResidueDictionary.newResidue("KDN"));
    for( int i=0; i<MUR; i++ ) ret.addAntenna(ResidueDictionary.newResidue("MurNAc"));

    for( int i=0; i<S; i++ ) ret.addAntenna(ResidueDictionary.newResidue("S"));
    for( int i=0; i<P; i++ ) ret.addAntenna(ResidueDictionary.newResidue("P"));
    for( int i=0; i<AC; i++ ) ret.addAntenna(ResidueDictionary.newResidue("Ac"));
    for( int i=0; i<PYR; i++ ) ret.addAntenna(ResidueDictionary.newResidue("Pyr"));
    for( int i=0; i<PC; i++ ) ret.addAntenna(ResidueDictionary.newResidue("PC"));
    
    for( int i=0; i<KETOSE; i++) ret.addAntenna(ResidueDictionary.newResidue("Ketose"));
    for( int i=0; i<UNKNOWN; i++) ret.addAntenna(ResidueDictionary.newResidue("Unknown"));

    for( int i=0; i<OR1; i++ ) ret.addAntenna(new Residue(ResidueType.createOtherResidue(OR1_NAME,OR1_MASS)));
    for( int i=0; i<OR2; i++ ) ret.addAntenna(new Residue(ResidueType.createOtherResidue(OR2_NAME,OR2_MASS)));
    for( int i=0; i<OR3; i++ ) ret.addAntenna(new Residue(ResidueType.createOtherResidue(OR3_NAME,OR3_MASS)));

    /** define a boolean of composition */
    for(Residue a_oRES : ret.getAllResidues()) a_oRES.isComposition(true);
    
    return ret;
    }
}
