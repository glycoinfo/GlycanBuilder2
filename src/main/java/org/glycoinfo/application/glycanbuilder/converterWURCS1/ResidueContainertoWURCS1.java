package org.glycoinfo.application.glycanbuilder.converterWURCS1;

import org.glycoinfo.WURCSFramework.util.oldUtil.ConverterExchangeException;
import org.glycoinfo.WURCSFramework.util.oldUtil.SubstituentTemplate;
import org.glycoinfo.WURCSFramework.util.residuecontainer.*;
import org.glycoinfo.application.glycanbuilder.convertutil.WURCSconvertUtil_old;

import java.util.HashMap;
import java.util.LinkedList;

public class ResidueContainertoWURCS1 {
	private LinkedList<String> lst_linkage = new LinkedList<String>();
	private LinkedList<ResidueContainer> lst_RC = new LinkedList<ResidueContainer>();
	
	public void start(LinkedList<ResidueContainer> lst_WJE) throws ConverterExchangeException {
		this.sortBacktoForward(lst_WJE);
		
		for(ResidueContainer a_objRC : this.lst_RC) {
			StringBuilder str_linkage = new StringBuilder();
			this.convertSkeletonCode(a_objRC);
			
			if(a_objRC.getNodeID() > 1) {
				str_linkage.append(this.extractChild(a_objRC));	
				str_linkage.append(",");
				if(a_objRC.getRootStatusDescriptor().equals(RootStatusDescriptor.FRAGMENT)) {
					LinkedList<String> lst_amb = new LinkedList<String>();
					for(String s : a_objRC.getLinkage().getParent()) {
						lst_amb.addLast("(" + this.extractParent(this.getParent(s), a_objRC) + ")");
					}
//					str_linkage.append(String.join("/", lst_amb));
				}else {	
					String parentIndex = a_objRC.getLinkage().getParent().getFirst();
					str_linkage.append(this.extractParent(this.getParent(parentIndex), a_objRC));
				}
				this.lst_linkage.addLast(str_linkage.toString());
			}		
		}
	}
	
	public String getWURCS() {
		StringBuilder ret = new StringBuilder();
		ret.append("WURCS=1.0/");
		ret.append(this.lst_RC.size());
		ret.append(",");
		ret.append(this.lst_linkage.size());
		ret.append("/");
		
		StringBuilder str_link = new StringBuilder();
		for(ResidueContainer a_objRC : this.lst_RC) {
			ret.append(a_objRC.getMS());
			str_link.append(this.extractRepeatingUnitWithLinkage(a_objRC));
		}
		
		return ret.append(str_link).toString();
	}
		
	private void convertSkeletonCode(ResidueContainer a_objRC) throws ConverterExchangeException {
		StringBuilder ret = new StringBuilder();		
		
		ret = WURCSconvertUtil_old
				.convertSugartoBase(checkMonosaccharideName(a_objRC), 
						a_objRC.getDLconfiguration().getFirst());

		/**add modification in start/end position*/
		ret = WURCSconvertUtil_old.checkComposition(ret, a_objRC);
		
		/**set anomerpos*/
		String anomerPos = "";
		if(a_objRC.getAnomerSymbol() == 'a') anomerPos = "1";
		if(a_objRC.getAnomerSymbol() == 'b') anomerPos = "2";
		if(a_objRC.getAnomerSymbol() == '?') {
			/**alditonの条件*/
			if(a_objRC.getRingSize() == 'o') anomerPos = "h";
			else anomerPos = /*a_objWJE.getAnomerPosition() > 1 ? */"X"/* : "x"*/;
		}
		if(a_objRC.getAnomerPosition() > 0) ret.insert(a_objRC.getAnomerPosition() - 1, anomerPos);


		/**add ring size*/
		if(a_objRC.getRingSize() != 'o') {
			ret.append("|");
			if(a_objRC.getAnomerPosition() == 1) {
				/**p : 1,5 / f : 1,4*/
				if(a_objRC.getRingSize() == 'p') ret.append("1,5");
				if(a_objRC.getRingSize() == 'f') ret.append("1,4");
			}
			if(a_objRC.getAnomerPosition() == 2) {
				/**p : 2,6 / f : 2.5 */
				if(a_objRC.getRingSize() == 'p') ret.append("2,6");
				if(a_objRC.getRingSize() == 'f') ret.append("2,5");
			}
			if(a_objRC.getRingSize() == '?') ret.append("x,x");
		}

		/**add modification*/
		if(a_objRC.getSugarName().length() > 3) { 
		  ResidueContainerUtility rcu = new ResidueContainerUtility();
		  rcu.extractSubstituent(a_objRC);
		}
		for(String str_unit : a_objRC.getSubstituent()) {
			String[] str_mod = str_unit.split("\\*");
			SubstituentTemplate enum_ST = SubstituentTemplate.forIUPACNotation(str_mod[1]);
			if(!enum_ST.isSubstituent()) 
				ret.replace(Integer.parseInt(str_mod[0]) - 1, Integer.parseInt(str_mod[0]), enum_ST.getMAP());
			else ret.append("|" + str_mod[0] + enum_ST.getMAP());
		}			

		ret.insert(0, "[");
		ret.append("]");

		/**define repeating*/
		ret = this.extractRepeatingUnitWithResidue(ret, a_objRC);
		a_objRC.setMS(ret.toString());

		return;
	}
	
	private LinkedList<String> checkMonosaccharideName(ResidueContainer a_objRC) {
		LinkedList<String> ret = new LinkedList<String>();

		if(a_objRC.getSugarName().contains("Neu")) {
			ret.add("dgal");
			ret.add("dgro");		
			return ret;
		}
		if(a_objRC.getSugarName().contains("Kdo")) {
			ret.add("dman");
			return ret;
		}
		if(a_objRC.getSugarName().contains("-")) {
			for(String s : a_objRC.getIUPACExtendedNotation().split("-")) {
				if(s.length() == 7) s = s.substring(0, 4);
				ret.addFirst(s.toLowerCase());
			}
		}

		String a_strName = "";
		if(ret.size() == 0) {
			a_strName = a_objRC.getSugarName();
			if(a_objRC.isAcidicSugar() && a_objRC.getSugarName().endsWith("A"))
				a_strName = a_strName.replace("A", "");
			if(a_objRC.getSugarName().length() > 3)
				a_strName = a_strName.substring(0, 3);
			a_strName = (a_objRC.getDLconfiguration().getFirst() + a_strName);
		}

		ret.addLast(a_strName.toLowerCase());

		return ret;
	}

	private String convertStemType(String str_shapes) {
		if(str_shapes.length() > 3) str_shapes = str_shapes.substring(0, 2);
		System.out.println(str_shapes);
		SuperClass enum_base = SuperClass.getBaseType(str_shapes);//.getBaseType(str_shapes);
		
		return enum_base.getBasetype();
	}
	
	private StringBuilder extractRepeatingUnitWithResidue(StringBuilder str_wurcs, ResidueContainer a_objWJE) {
		HashMap<String, RepeatingBlock> a_objWJRB = a_objWJE.getLinkage().getRepeatingBlock();
		if(a_objWJRB.isEmpty()) return str_wurcs;

		if(a_objWJRB.containsKey("start")) {
			if(a_objWJE.equals(this.lst_RC.getLast())) return str_wurcs;
			str_wurcs.append(">");
		}
		if(a_objWJRB.containsKey("end")) {
			str_wurcs.insert(0, this.checkRepeatingCount(a_objWJRB.get("end")));
			str_wurcs.insert(0, "<");
		}
		
		return str_wurcs;
	}
	
	private String extractRepeatingUnitWithLinkage(ResidueContainer a_objWJE) {
		StringBuilder str_linkage = new StringBuilder();
		HashMap<String, RepeatingBlock> a_objWJRB = a_objWJE.getLinkage().getRepeatingBlock();
		if(this.lst_linkage.size() > this.lst_RC.indexOf(a_objWJE) ) {
			if(this.lst_RC.getFirst() != a_objWJE) str_linkage.append("|");
			str_linkage.append(this.lst_linkage.get(this.lst_RC.indexOf(a_objWJE)));
		}
		if(a_objWJRB.containsKey("start")) {
			if(!a_objWJE.equals(this.lst_RC.getLast())) str_linkage.append(">");
		}
		if(a_objWJRB.containsKey("end")) {
			if(this.lst_RC.size() == 2 && a_objWJE.equals(this.lst_RC.getFirst())) str_linkage.append(">");
			else {
				str_linkage.insert(1, ":");
				str_linkage.insert(1, this.checkRepeatingCount(a_objWJRB.get("end")));
				str_linkage.insert(1, "<");
			}
		}
		
		return str_linkage.toString();
	}
	
	private String checkRepeatingCount(RepeatingBlock a_objWJRB) {
		if(a_objWJRB.getMin() == -1 && a_objWJRB.getMax() == -1) return "n";
		if(a_objWJRB.getMin() != -1) return String.valueOf(a_objWJRB.getMin());
		if(a_objWJRB.getMax() != -1) return String.valueOf(a_objWJRB.getMax());
		if(a_objWJRB.getMin() != -1 && a_objWJRB.getMax() != -1) 
			return a_objWJRB.getMax() + "-" + a_objWJRB.getMax();
		return "";
	}
	
	/*private void sortMonosaccharide(LinkedList<ResidueContainer> lst_WJE, ResidueContainer a_objWJE) {
		LinkageBlock a_objWJLB = a_objWJE.getLinkage();
		if(a_objWJLB.getChild().size() == 0) {			
			this.lst_RC.addLast(a_objWJE);
		}
		if(a_objWJLB.getChild().size() == 1) {
			a_objWJE = ResidueContainerUtility.getIndex(lst_WJE, a_objWJLB.getChild().getFirst());
			this.sortMonosaccharide(lst_WJE, a_objWJE);
		}
		if(a_objWJLB.getChild().size() > 1) {
			for(int i = a_objWJLB.getChild().size() - 1; i >=0; i--) {
				String strIndex = a_objWJLB.getChild().get(i);
				a_objWJE = ResidueContainerUtility.getIndex(lst_WJE, strIndex);
				this.sortMonosaccharide(lst_WJE, a_objWJE);
			}
		}
	}*/
	
	private void sortBacktoForward(LinkedList<ResidueContainer> lst_RC) {
		for(ResidueContainer a_objRC : lst_RC) this.lst_RC.addFirst(a_objRC);
	}

	private void sortForwardtoBack(LinkedList<ResidueContainer> lst_RC) {
		for(ResidueContainer a_objRC : lst_RC) this.lst_RC.addLast(a_objRC);
	}
	
	private String extractChild(ResidueContainer obj_WJE) {
		StringBuilder ret = new StringBuilder();
		ret.append(this.lst_RC.indexOf(obj_WJE) + 1);
		ret.append("+");
		ret.append(obj_WJE.getAnomerPosition() == -1 ? "?" : obj_WJE.getAnomerPosition());
		
		return ret.toString();
	}
	
	private String extractParent(ResidueContainer obj_parent, ResidueContainer obj_child) {
		StringBuilder ret = new StringBuilder();
		ret.append(obj_parent == null ? "?" : this.lst_RC.indexOf(obj_parent) + 1);
		ret.append("+");
		ret.append(obj_child.getLinkage().getAcceptors().getFirst() > 0 ? obj_child.getLinkage().getAcceptors().getFirst() : "?");
		return ret.toString();
	}

	private ResidueContainer getParent(String parentIndex) {
		for(ResidueContainer a_objWJE : this.lst_RC) {
			if(a_objWJE.getNodeIndex().equals(parentIndex)) return a_objWJE;
		}
		
		return null;
	}
}