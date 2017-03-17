package org.glycoinfo.application.glycanbuilder.converterWURCS1;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;

public class WURCSParserLinkageUtil {
	private LinkedList<Residue> lst_Residue = new LinkedList<Residue>();
	private LinkedList<String> lst_MLU = new LinkedList<String>();
	
	public WURCSParserLinkageUtil() {
		this.lst_Residue.clear();
		this.lst_MLU.clear();
	}
	
	public void addMLU(String str_mlu) {
		this.lst_MLU.addLast(str_mlu);
	}
	
	public void addResidue(Residue obj_Residue) {
		this.lst_Residue.addLast(obj_Residue);
	}
	
	public LinkedList<Residue> getResidue() {
		return this.lst_Residue;
	}
	
	public LinkedList<String> getMLU() {
		return this.lst_MLU;
	}
	
	public void generateLinkage(String str_LMU) {
		/**ambiguous structure or ambiguous linkage position*/
		Matcher mat_lin = Pattern.compile("(([\\d]+)\\+([\\d\\?]+)),(\\(?([\\d\\?]+)\\+([\\d\\?]+))").matcher(str_LMU);
		if(mat_lin.find()) {
			// 曖昧な結合情報を扱う
			//ブラケットが連なるため、塊を取り出して後で分解する処理に移行する
			Residue parent = null;
			Residue child = this.lst_Residue.get(Integer.parseInt(mat_lin.group(2)) - 1);
	
			if(!mat_lin.group(5).equals("?"))
				parent = this.lst_Residue.get(Integer.parseInt(mat_lin.group(5)) - 1);
			Linkage a_objParent = new Linkage(parent, child);
			a_objParent.setLinkagePositions(mat_lin.group(6).charAt(0));
			child.setParentLinkage(a_objParent);
			if(parent != null) {
				parent.addChild(child, child.getParentLinkage().getBonds());
				parent.sortChildLinkage();
			}
		}
		return;
	}
	
	private Residue searchChild(String str_ID) {
		for(String s : this.lst_MLU) {
			if(s.substring(s.indexOf(",") + 1, s.lastIndexOf("+")).equals(str_ID)) {
				return this.lst_Residue.get(Integer.parseInt(s.substring(0, s.indexOf("+"))) - 1);
			}
		}
		
		return null;
	}
	
	public void generateRepeatingBlock(String str_repMLU, boolean repBlock) {
		Matcher mat_lin = Pattern.compile("(<?([\\dn-]?):?([\\d]+)\\+([\\d\\?]+)),(\\(?([\\d\\?]+)\\+([\\d\\?]+)\\)?>?)").matcher(str_repMLU);
		if(mat_lin.find()) {
			Residue child = this.lst_Residue.get(Integer.parseInt(mat_lin.group(3)) - 1);
			Residue parent = this.lst_Residue.get(Integer.parseInt(mat_lin.group(6)) - 1);
			
			Residue start = null;
			Residue end = null;
		
			/**set end repetition bracket*/
			if(mat_lin.group(0).contains(">") && mat_lin.group(0).contains("<")) {	
				String min = "-1";
				String max = "-1";
				if(!mat_lin.group(2).equals("n") && !mat_lin.group(2).contains("-")) max = min = mat_lin.group(2);
				if(!mat_lin.group(2).equals("n") && mat_lin.group(2).contains("-")) {
					String[] rep_cnt = mat_lin.group(2).split("-");
					min = rep_cnt[0];
					max = rep_cnt[1];
				}
				
				end = ResidueDictionary.createEndRepetition(min, max);
				Residue c = this.searchChild(mat_lin.group(3));
				end.addChild(c, c.getParentLinkage().getBonds());	
				child.addChild(end);
				for(Linkage l : child.getChildrenLinkages()) {
					if(l.getChildResidue().equals(end.getChildrenLinkages().getFirst().getChildResidue())) 
						child.getChildrenLinkages().remove(l);
				}

				/**set start repetition bracket*/
				start = ResidueDictionary.createStartRepetition();
				Linkage a_objRepLIN = new Linkage(start, child);
				a_objRepLIN.setLinkagePositions(mat_lin.group(7).charAt(0));
				child.setParentLinkage(a_objRepLIN);
				start.addChild(child, child.getParentLinkage().getBonds());
				
				start.setEndRepitionResidue(end);
				parent.addChild(start);
			}
			
			if(repBlock) {
				start = ResidueDictionary.createStartRepetition();
				Linkage a_objRepLIN = new Linkage(start, parent);
				a_objRepLIN.setLinkagePositions(mat_lin.group(7).charAt(0));
				parent.setParentLinkage(a_objRepLIN);
				start.addChild(parent, parent.getParentLinkage().getBonds());
				parent.setStartRepetiionResidue(start);
				
				end = ResidueDictionary.createEndRepetition("-1", "-1");
				Linkage node_end = new Linkage(child, end);
				node_end.setLinkagePositions('?');
				end.setParentLinkage(node_end);
				child.addChild(end, end.getParentLinkage().getBonds());
				child.setEndRepitionResidue(end);
				
				this.generateLinkage(mat_lin.group(0).substring(0, mat_lin.group(0).length() - 1));
			}
		}
		
		return;
	}
}
