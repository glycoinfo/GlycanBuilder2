package org.glycoinfo.application.glycanbuilder.converterWURCS2;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.glycoinfo.GlycanFormatconverter.util.exchange.SugarToWURCSGraph.SugarToWURCSGraph;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.application.glycanbuilder.util.exchange.importer.WURCSSequence2ToGlycan;

public class WURCS2ParserViaCT implements GlycanParser{
	
	public void setTolerateUnknown(boolean f) {}
	
	public String writeGlycan(Glycan structure) {
		if (structure.isFragment()) return "";

		try{
			SugarToWURCSGraph a_oS2WG = new SugarToWURCSGraph();
			a_oS2WG.start(structure.toSugar());
			WURCSFactory a_oWF = new WURCSFactory(a_oS2WG.getGraph());
			
			return a_oWF.getWURCS();
		}catch (Exception e) {
			LogUtils.report(e);
			return "";
		}
	}
	
	public Glycan readGlycan(String str, MassOptions mass_opt) throws Exception{
		if(str.equals("") || str == null || !str.contains("WURCS")) throw new Exception("This string is wrong format");
		
		str = str.trim();		
		if(str.contains("\t")) str = str.substring(str.indexOf("\t") + 1, str.length());
		
		WURCSFactory a_oWF = new WURCSFactory(str);
		WURCSSequence2ToGlycan a_oWS22G = new WURCSSequence2ToGlycan();
		a_oWS22G.start(a_oWF, mass_opt);
		return a_oWS22G.getGlycan();
	}

	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		throw new UnsupportedOperationException();
	}
}
