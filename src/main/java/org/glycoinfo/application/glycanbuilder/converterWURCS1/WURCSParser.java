
package org.glycoinfo.application.glycanbuilder.converterWURCS1;

import java.util.*;
import java.util.regex.*;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.glycoinfo.application.glycanbuilder.convertutil.GlycanToResidueContainer;

public class WURCSParser implements GlycanParser {
	
	private WURCSParserUtil a_ParserUtil = new WURCSParserUtil();
	private WURCSParserLinkageUtil a_WPLU = new WURCSParserLinkageUtil();
	
	public void setTolerateUnknown(boolean f) {}
	
	public String writeGlycan(Glycan structure) {
		if (structure.isFragment()) return "";

		try{
			ResidueContainertoWURCS1 a_objWJC = new ResidueContainertoWURCS1();
			a_objWJC.start(new GlycanToResidueContainer().getResidueContainerList(structure));
			return a_objWJC.getWURCS();
		}catch (Exception e) {
			return "error";
		}
	}

	public Glycan readGlycan(String str, MassOptions mass_opt) throws Exception {
		if(str == null) throw new Exception(" This string is null");
		str = str.trim();
		
		Matcher mat_param = Pattern.compile("^WURCS=1.0/(\\d+),(\\d+)/(<?([\\dn-]?)(\\[.+\\]))(.+)").matcher(str);
		if(mat_param.find()) {
			boolean repeatingBlock = 
					(mat_param.group(3) + mat_param.group(6)).startsWith("<") && 
					(mat_param.group(3) + mat_param.group(6)).endsWith(">") ? true: false;
		
			/**Define monosaccharide*/
			for(String s : this.exractRepeatingStructure(mat_param.group(5))) {
				//this.a_ParserUtil.clearModificaiton();
				this.a_WPLU.addResidue(this.a_ParserUtil.convertSkeletonCodetoResidue(s));
			}
			
			/**Define linkage*/
			for(String s : this.extractMLU(mat_param.group(6))) {
				if(!s.contains(">") && !s.contains("<")) this.a_WPLU.generateLinkage(s);
				else this.a_WPLU.generateRepeatingBlock(s, repeatingBlock);
			}
		}
		
		return this.a_ParserUtil.makeGlycan(this.a_WPLU.getResidue().getLast(), this.a_WPLU.getResidue(), mass_opt);
	}

	private LinkedList<String> extractMLU(String str_MLUs) {
		for(String s : str_MLUs.split("\\|")) this.a_WPLU.addMLU(s);
		return this.a_WPLU.getMLU();
	}
	
	private LinkedList<String> exractRepeatingStructure(String str_BMUs) {
		LinkedList<String> ret = new LinkedList<String>();
		String str_block = "";
		for(int i = 0; i < str_BMUs.length(); i++) {
			if(str_BMUs.charAt(i) == '[') str_block = "";
			if(str_BMUs.charAt(i) != '[' && str_BMUs.charAt(i) != ']')str_block += str_BMUs.charAt(i);
			if(str_BMUs.charAt(i) == ']') ret.addLast(str_block);
		}
		return ret;
	}
	
	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		throw new UnsupportedOperationException();
	}
}