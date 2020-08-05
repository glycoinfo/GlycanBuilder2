package org.glycoinfo.application.glycanbuilder.util.exchange.exporter;

import org.eurocarbdb.MolecularFramework.sugar.LinkageType;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.glycoinfo.GlycanFormatconverter.util.exchange.SugarToWURCSGraph.SubstituentTypeToMAP;
import org.glycoinfo.WURCSFramework.util.oldUtil.SubstituentTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class ResidueToModification {

	private Residue subtituent;
	private Linkage childLinkage = null;
	private Linkage parentLinkage = null;

	private int parentMAPPosition = 0;
	private int childMAPPosition = 0;
	private String headAtom = "";
	private String tailAtom = "";
	private String map = "";
	
	private SubstituentTypeToMAP subType2MAP;
	private SubstituentTemplate subTemp;
	
	private LinkageType parentLinkageType;
	private LinkageType childLinkageType;
	
	private String notationGCT = "";
	
	public String getMAPCode() {
		return this.map;
	}
	
	public int getMAPPositionForParentSide() {
		return this.parentMAPPosition;
	}
	
	public int getMAPPositionForChildSide() {
		return this.childMAPPosition;
	}
	
	public String getHeadAtom() {
		return this.headAtom;
	}
	
	public String getTailAtom() {
		return this.tailAtom;
	}
	
	public SubstituentTemplate getSubstituentTemplate() {
		return this.subTemp;
	}
	
	public void setParentLinkage(Linkage _linkage) {
		this.parentLinkage = _linkage;
	}
	
	public void setChildLinkage(Linkage _linkage) {
		this.childLinkage = _linkage;
	}
	
	public void setSubstituentTemplate(Residue _substituent) throws Exception {
		this.subTemp =	SubstituentTemplate.forIUPACNotation(_substituent.getTypeName());
		this.notationGCT = this.subTemp.getGlycoCTnotation();
		
		return;
	}
	
	public void start(Residue _substituent) throws Exception {
		this.subtituent = _substituent;
		this.subType2MAP = SubstituentTypeToMAP.forName(this.notationGCT);
		
		this.headAtom = this.subType2MAP.getHeadAtom();
		this.tailAtom = this.subType2MAP.getTailAtom();
		
		if(this.subtituent.getParentLinkage() != null)
			this.parentLinkage = this.subtituent.getParentLinkage();
		if(this.parentLinkage == null)
			throw new Exception("Substituent should have parent linkage");
		if(!this.subtituent.getChildrenLinkages().isEmpty())
			this.childLinkage = this.subtituent.getChildrenLinkages().get(0);
		
		this.parentLinkageType = this.parentLinkage.getParentLinkageType();
		this.childLinkageType = this.parentLinkage.getChildLinkageType();
		
		if(this.parentLinkageType == LinkageType.UNKNOWN)
			this.parentLinkageType = LinkageType.H_AT_OH;
		
		String mapDouble = this.subType2MAP.getMAPDouble();
		if(mapDouble != null && mapDouble.equals("") &&
				_substituent.getParentLinkage().getBonds().size() > 1) return;
		
		this.map = (_substituent.getParentLinkage().getBonds().size() == 1 &&
				!_substituent.getType().getSuperclass().equals("Bridge")) ?
				this.getMAPCodeSingle() : this.getMAPCodeDouble();
	}
	
	public String getMAPCodeSingle() {
		String mapSingle = this.subType2MAP.getMAPSingle();
		boolean isBond = (mapSingle.startsWith("C") && !mapSingle.equals("CO") && !mapSingle.equals("Cl"))  ||
				(mapSingle.startsWith("S") && !mapSingle.equals("S") ||
				(mapSingle.startsWith("P"))) ? true : false;
		
		if(this.parentLinkageType.equals(LinkageType.H_AT_OH)) isBond = true;
		else isBond = false;
		
		if(isBond) {
			this.headAtom = "O";
			mapSingle = this.addOxygenToHead(mapSingle);
		}
		
		return "*" + mapSingle;
	}
	
	public String getMAPCodeDouble() {
		String mapDouble = this.subType2MAP.getMAPDouble();
		Boolean isSwap = this.subType2MAP.isSwapCarbonPositions();
		boolean hasOrder = false;
		
		if(isSwap == null && this.parentLinkageType != this.childLinkageType) {
			if(this.parentLinkageType == LinkageType.H_AT_OH)
				isSwap = false;
			else if (this.childLinkageType == LinkageType.H_AT_OH)
				isSwap = true;
		}
		if(isSwap != null) {
			this.parentMAPPosition = 1;
			this.childMAPPosition = 2;
			if(isSwap) {
				this.parentMAPPosition = 2;
				this.childMAPPosition = 1;
			}
			hasOrder = true;
		} else {
			isSwap = false;
		}

		// add oxygen
		if(this.parentLinkageType == LinkageType.H_AT_OH) {
			this.headAtom = "O";
			mapDouble = (isSwap) ?
					this.addOxygenToTail(mapDouble) : this.addOxygenToHead(mapDouble);
		}
		if(this.childLinkageType == LinkageType.H_AT_OH) {
			this.tailAtom = "O";
			mapDouble = (isSwap) ?
					this.addOxygenToHead(mapDouble) : this.addOxygenToTail(mapDouble);
		}

		if(hasOrder)
			mapDouble = this.addMAPStarIndex(mapDouble);
		
		mapDouble = "*" + mapDouble;
		mapDouble = mapDouble.replace("*OP^XO*", "*OPO*");
		mapDouble = mapDouble.replace("*P^X*", "*P*");
		
		return mapDouble;
	}
	
	private String addOxygenToHead(String _map) {
		if(_map.startsWith("NCCOP")) return _map;
		
		ArrayList<Integer> nums = new ArrayList<Integer>();
		String num = "";
		for(int i = 0; i < _map.length(); i++) {
			char unit = _map.charAt(i);
			if( Character.isDigit(unit)) {
				num += unit;
				continue;
			}
			if(num.equals("")) continue;
			if(nums.contains(Integer.parseInt(num))) continue;
			nums.add(Integer.parseInt(num));
			num = "";
		}
		Collections.sort(nums);
		Collections.reverse(nums);
		
		String newMAP = _map;
		for(Iterator<Integer> it = nums.iterator(); it.hasNext();) {
			Integer num1 = it.next();
			Integer num2 = num1 + 1;
			newMAP = newMAP.replaceAll(num1.toString(), num2.toString());
		}
		return "O" + newMAP;
	}
	
	private String addOxygenToTail(String _map) {
		StringBuilder mapNotation = new StringBuilder(_map);
		int insertPos = mapNotation.lastIndexOf("*");
		mapNotation.insert(insertPos, 'O');
		_map = mapNotation.toString();
		
		int oxygenPosition = 1;
		for(int i = 0; i < insertPos; i++) {
			char mapUnit = _map.charAt(i);
			if(mapUnit == '^' || mapUnit == '/') {
				i++;
				continue;
			} else if (mapUnit == '=' || mapUnit == '#') {
				continue;
			} else if (mapUnit == '*') {
				break;
			}
			oxygenPosition++;
		}
		
		ArrayList<Integer> nums = new ArrayList();
		String numString = "";
		for(int i = 0; i < _map.length(); i++) {
			char mapUnit = _map.charAt(i);
			if(Character.isDigit(mapUnit)) {
				numString += mapUnit;
				continue;
			}
			if(numString.equals("")) continue;
			if(nums.contains(Integer.parseInt(numString))) continue;
			nums.add(Integer.parseInt(numString));
			numString = "";
		}
		Collections.sort(nums);
		Collections.reverse(nums);
		
		String newMAP = _map;
		
		for(Iterator<Integer> it = nums.iterator(); it.hasNext();) {
			Integer num1 = it.next();
			if(num1 <= oxygenPosition) continue;
			Integer num2 = num1 + 1;
			newMAP = newMAP.replaceAll(num1.toString(), num2.toString());
		}
		return newMAP;
	}
	
	private String addMAPStarIndex(String _map) {
		StringBuilder mapNotation = new StringBuilder(_map);
		int insertPos2 = _map.indexOf("*");
		mapNotation.insert(insertPos2 + 1,  '2');
		mapNotation.insert(0, '1');
		return mapNotation.toString();
	}
}
