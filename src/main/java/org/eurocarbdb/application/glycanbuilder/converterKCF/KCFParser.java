
package org.eurocarbdb.application.glycanbuilder.converterKCF;

import java.util.*;
import java.util.regex.*;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.dataset.GWSParser;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.linkage.Bond;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage.LinkageComparator;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.util.TextUtils;

public class KCFParser implements GlycanParser {
	//単糖のデータオブジェクト
	private static final class SU {
		public String code;
		public char chirality;
		public char ring_size;

		public SU(String _code) {
			code = _code;
			chirality = '?';
			ring_size = '?';
		}

		public SU(String _code, char _chirality, char _ring_size) {
			code = _code;
			chirality = _chirality;
			ring_size = _ring_size;
		}
	}

	private static Pattern gmind_pattern;
	private static Pattern gmind_sub_pattern;

	private static HashMap<String, String> gmind_types;
	private static HashMap<String, SU> gmind_codes;

	static {
		//糖鎖から単糖データを取り出す場合に使用する正規表現
		gmind_pattern = Pattern
				.compile("([A-Z]+)([\\'\\^\\~]?)((?:\\[[\\?1-9a-zA-Z\\,]+\\])?)([abo\\?]?)([1-9\\?]?(?:/[1-9\\?])*)\\z");
		//修飾データを扱う正規表現
		gmind_sub_pattern = Pattern
				.compile("([\\?1-9]{0,1})([a-zA-Z]*)(?:\\,([\\?1-9]{0,1})([a-zA-Z]+))*");

		//LinearCodeから単糖のデータを作り出す場合に使用する
		gmind_types = new HashMap<String, String>();
		gmind_types.put("G", "?1D-Glc,p");
		gmind_types.put("A", "?1D-Gal,p");
		gmind_types.put("GN", "?1D-GlcNAc,p");
		gmind_types.put("AN", "?1D-GalNAc,p");
		gmind_types.put("M", "?1D-Man,p");
		gmind_types.put("N", "?2D-Neu,p");
		gmind_types.put("NN", "?2D-NeuAc,p");
		gmind_types.put("NJ", "?2D-NeuGc,p");
		gmind_types.put("K", "?2D-KDN,p");
		gmind_types.put("W", "?2D-KDO,p");
		gmind_types.put("L", "?1D-GalA,p");
		gmind_types.put("I", "?1D-IdoA,p");
		gmind_types.put("H", "?1L-Rha,p");
		gmind_types.put("F", "?1L-Fuc,p");
		gmind_types.put("X", "?1D-Xyl,p");
		gmind_types.put("B", "?1D-Rib,p");
		gmind_types.put("R", "?1L-Ara,f");
		gmind_types.put("U", "?1D-GlcA,p");
		gmind_types.put("O", "?1D-All,p");
		gmind_types.put("P", "?1D-Api,p");
		gmind_types.put("E", "?2D-Fru,f");
		
		gmind_types.put("LYX", "?1L-Lyx,p");
		gmind_types.put("QUI", "?1D-Qui,p");
		gmind_types.put("DTAL", "?1D-dTal,p");
		gmind_types.put("BAC", "?1D-Bac,p");
		gmind_types.put("TAL", "?1D-Tal,p");
		gmind_types.put("J", "?1L-Alt,p");
		gmind_types.put("GL", "?1D-Gul,f");
		gmind_types.put("SOR", "?2L-Sor,p");
		gmind_types.put("TAG", "?2L-Tag,p");
		gmind_types.put("MA", "?1D-ManA,p");
		gmind_types.put("JA", "?1L-AltA,p");
		gmind_types.put("GLA", "?1D-GulA,f");
		gmind_types.put("MAC", "?1D-ManNAc,p");
		gmind_types.put("BNAC", "?1D-BacNAc,p");
		gmind_types.put("MUR", "?1D-MurNAc,p");
		gmind_types.put("KO", "?2D-Ko,p");

		gmind_types.put("C", "Cer");
		gmind_types.put("DAG", "DAG");
		gmind_types.put("IPC", "IPC");
		gmind_types.put("LIA", "LipdA");
		gmind_types.put("D", "Sph");
		
		gmind_types.put("P", "P");
		gmind_types.put("ME", "Me");
		gmind_types.put("T", "Ac");
		gmind_types.put("NAC", "NAc");
		gmind_types.put("S", "S");
		gmind_types.put("PYR", "Pyr");
		gmind_types.put("PC", "PC");
		gmind_types.put("PPETN", "PPEtn");
		gmind_types.put("PETN", "PEtn");
		gmind_types.put("N", "N");
		gmind_types.put("DO", "deoxy");

		
		//描画した構造から線形表示にする場合に使用する
		gmind_codes = new HashMap<String, SU>();
		gmind_codes.put("Glc", new SU("G", 'D', 'p'));
		gmind_codes.put("Gal", new SU("A", 'D', 'p'));
		gmind_codes.put("GlcNAc", new SU("GN", 'D', 'p'));
		gmind_codes.put("GalNAc", new SU("AN", 'D', 'p'));
		gmind_codes.put("Man", new SU("M", 'D', 'p'));
		gmind_codes.put("Neu", new SU("N", 'D', 'p'));
		gmind_codes.put("NeuAc", new SU("NN", 'D', 'p'));
		gmind_codes.put("NeuGc", new SU("NJ", 'D', 'p'));
		gmind_codes.put("KDN", new SU("K", 'D', 'p'));
		gmind_codes.put("KDO", new SU("W", 'D', 'p'));
		gmind_codes.put("GalA", new SU("L", 'D', 'p'));
		gmind_codes.put("IdoA", new SU("I", 'D', 'p'));
		gmind_codes.put("Rha", new SU("H", 'L', 'p'));
		gmind_codes.put("Fuc", new SU("F", 'L', 'p'));
		gmind_codes.put("Xyl", new SU("X", 'D', 'p'));
		gmind_codes.put("Rib", new SU("B", 'D', 'p'));
		gmind_codes.put("Ara", new SU("R", 'L', 'f'));
		gmind_codes.put("GlcA", new SU("U", 'D', 'p'));
		gmind_codes.put("All", new SU("O", 'D', 'p'));
		gmind_codes.put("Api", new SU("P", 'D', 'p'));
		gmind_codes.put("Fru", new SU("E", 'D', 'f'));
		
		gmind_codes.put("Lyx", new SU("LYX", 'L', 'p'));
		gmind_codes.put("Qui", new SU("QUI", 'D', 'p'));
		gmind_codes.put("dTal", new SU("DTAL", 'D', 'p'));
		gmind_codes.put("Bac", new SU("BAC", 'D', 'p'));
		gmind_codes.put("Tal", new SU("TAL", 'D', 'p'));
		gmind_codes.put("Alt", new SU("J", 'L', 'p'));
		gmind_codes.put("Gul", new SU("GL", 'D', 'f'));
		gmind_codes.put("Sor", new SU("SOR", 'L', 'p'));
		gmind_codes.put("Tag", new SU("TAG", 'L', 'p'));
		gmind_codes.put("ManA", new SU("MA", 'D', 'p'));
		gmind_codes.put("AltA", new SU("JA", 'L', 'p'));
		gmind_codes.put("GulA", new SU("GLA", 'D', 'f'));
		gmind_codes.put("ManNAc", new SU("MAC", 'D', 'p'));
		gmind_codes.put("BacNAc", new SU("BNAC", 'D', 'p'));
		gmind_codes.put("MurNAc", new SU("MUR", 'D', 'p'));
		gmind_codes.put("Ko", new SU("KO", 'D', 'p'));
		
		gmind_codes.put("Cer", new SU("C"));
		gmind_codes.put("DAG", new SU("DAG"));
		gmind_codes.put("IPC", new SU("IPC"));
		gmind_codes.put("LipdA", new SU("LIA"));
		gmind_codes.put("Sph", new SU("D"));
		
		gmind_codes.put("P", new SU("P"));
		gmind_codes.put("Me", new SU("ME"));
		gmind_codes.put("Ac", new SU("T"));
		gmind_codes.put("NAc", new SU("NAC"));
		gmind_codes.put("S", new SU("S"));
		gmind_codes.put("Pyr", new SU("PYR"));
		gmind_codes.put("PC", new SU("PC"));
		gmind_codes.put("PPEtn", new SU("PPETN"));
		gmind_codes.put("PEtn", new SU("PETN"));
		gmind_codes.put("N", new SU("N"));
		gmind_codes.put("deoxy", new SU("DO"));
		gmind_types.put("DO", "deoxy");
	}

	public void setTolerateUnknown(boolean f) {
	}
	// 多糖類構造を引数とし、文字列に変換
	public String writeGlycan(Glycan structure) {

		if (structure.isFragment())
			return "";
		int bracket = 0;	
		
		//構造のRootを得る
		//rootが、Nullでない、かつ単糖でない場合、rootの最初の子をrootに入れる
		Residue root = structure.getRoot();  
		if (root != null && !root.isSaccharide()) 
			root = root.firstChild(); 
		
		// 構造を書く
		//構造にBracket残基が存在しない場合、rootを文字列で返す
		if (structure.getBracket() == null){
			return writeSubtree(root,bracket,false);
		}
		
		//構造にBracket残基が存在する場合、
		List<String> myList = new ArrayList<String>();
		HashMap<String,String> map = new HashMap();
		StringBuilder sb = new StringBuilder();
		
		for (Linkage l : structure.getBracket().getChildrenLinkages()) {
			bracket++;
		}
		// コアを書く
		System.out.println(bracket);
		sb.append(writeSubtree(root,bracket,false));
		
		// アンテナを書く
		int antena = 1;
		bracket = 0;
		for (Linkage l : structure.getBracket().getChildrenLinkages()) {
			sb.insert(0,"=" + antena + "%|");
			sb.insert(0,writeSubtree(l.getChildResidue(),bracket,false));
			antena++;
		}
		
		return sb.toString();
	}
	
	// 文字列から Glycan型に変換
	public Glycan readGlycan(String str, MassOptions default_mass_options) throws Exception {
		//strの初めと終わりから、スペース・改行・タブ・復帰を取り除く
		str = TextUtils.trim(str);
		if (str.indexOf("//") != -1)
			throw new Exception("Unsupported structures with uncertain residues");
	
		//糖でない部分を取り除く
		int index = str.indexOf(";");
		if (index == -1) {	
			index = str.indexOf(":");
			if (index == -1)
				index = str.indexOf("#");
		}
		if (index != -1)	
			str = str.substring(0, index);	
		
		// 様々な文字（％、-）を取り除く
		str = str.replaceAll("(\\([1-9]+\\%\\))|([1-9]+\\%)", "");
		str = str.replaceAll("-", "");
		
		//アンテナ部分とコア部分を分ける
		String[] tokens1 = str.split("\\|");
		String str_core = tokens1[tokens1.length - 1];
		
		// parse the core
		Glycan structure = new Glycan(readSubtree(str_core), true,
				default_mass_options);

		//アンテナ部分をソートする
		List<String> myList = new ArrayList<String>();
		HashMap<String,String> map = new HashMap();
		for (int i = tokens1.length - 2; i >= 0; i--) {
			String str_antenna = tokens1[i].substring(0,tokens1[i].length() - 1);
			Matcher m = gmind_pattern.matcher(str_antenna);
			if (!m.find())
				throw new Exception("Unrecognized format: " + str_antenna);
			map.put(m.group(1), str_antenna);
			myList.add(m.group(1));
		
		}
		Collections.sort(myList);
		Iterator<String> it = myList.iterator();
		
		// parse the antenna
		for(int i = 0; i < myList.size(); i++){
			String an = myList.get(i);
			String ans = map.get(an);
			Residue antenna = readSubtree(ans);
			structure.addAntenna(antenna, antenna.getParentLinkage().getBonds());
	
		}
		return structure;
	}
	
	/**
	* 文字列からサブ木をResidue型にする
	* @param str
	* @return
	* @throws Exception
	*/
	private static Residue readSubtree(String str) throws Exception {
		//startRepは繰り返し構造の初め、endRepは終わりの部分を表すResidue型の構造
		Residue startRep = null;
		Residue endRep = null;
		if( str.charAt(str.length()-1)=='}' ){ 
			startRep= ResidueDictionary.createStartRepetition();	
			str = str.substring(0, str.length()-1);	
		}else if(str.charAt(str.length()-1)=='{'){
			endRep=ResidueDictionary.createEndRepetition();
			int repetation = TextUtils.findEnclosedInvert(str, str.length() - 1,'{', '}');
			str = str.substring(0, str.length()-1);
		}
		
		//retは実際に返すResidue型の構造
		Residue ret = null;
		if(endRep!=null){
			//endRepが存在するとき、retとendRepを関連付け、結合情報を入れる
			ret = endRep;
			Linkage par_link = new Linkage(null, ret);
			Bond bond = new Bond();
			bond.setChildPosition('?');
			ret.setAnomericCarbon(bond.getChildPosition());
			par_link.setLinkagePositions(parsePositions("?"));
			ret.setParentLinkage(par_link);
		}else if(startRep!=null){
			//startRepが存在するとき、結合情報を入れる
			ret = startRep;
			Linkage par_link = new Linkage(null, ret);
			Bond bond = new Bond();
			bond.setChildPosition('?');
			ret.setAnomericCarbon(bond.getChildPosition());
			par_link.setLinkagePositions(parsePositions("?"));
			ret.setParentLinkage(par_link);
		}else if(str.length()>0){
			//読み込んだ文字列から1つ単糖情報を抜き出し、データを作る
				Matcher m = gmind_pattern.matcher(str);
				if (!m.find())
					throw new Exception("Unrecognized format: " + str);
				//m.group(1)は単糖、m.group(2)は異性体や環構造の情報、
				//m.group(3)は修飾、m.group(4)はアノマー、m.group(5)は結合位置
				ret = createFromRINGS(m.group(1), m.group(2), m.group(3),m.group(4), m.group(5));
				str = str.substring(0, str.length() - m.group(0).length());
		}
		
		// parse children
		Vector<Linkage> children = new Vector<Linkage>();
		while (str.length() > 0) {
			Residue child = null;
			//par_indは’(’の位置
			int par_ind = TextUtils.findEnclosedInvert(str, str.length() - 1,'(', ')');
				if (par_ind != -1) {
					//分岐が存在する場合
					child = readSubtree(str.substring(par_ind + 1, str.length() - 1));
					str = str.substring(0, par_ind);
				} else {	
					child = readSubtree(str);	
					str = "";
				}
			//分岐が存在する場合、clidrenには複数の情報が存在する
			children.add(child.getParentLinkage());
			
		}
		
		if (children.size() > 0) {
			//子供が存在する場合
			children.insertElementAt(children.lastElement(), 0);
			children.remove(children.size() - 1);
		}
		
		fixBisectingGlcNAc(ret, children);
		// add children
		 Collections.sort(children,new Linkage.LinkageComparator());
		for (Linkage l : children)
			ret.addChild(l.getChildResidue(), l.getBonds());
			
		return ret;
	}

	private static void fixBisectingGlcNAc(Residue parent,Vector<Linkage> children) {
		if (!parent.getTypeName().equals("Man") || children.size() != 3)
			return;

		int glcnac_pos = -1;
		int no_glcnac = 0;
		int no_man = 0;
		for (int i = 0; i < children.size(); i++) {
			Linkage l = children.get(i);
			if (l.getChildResidue().getTypeName().equals("Man"))
				no_man++;
			else if (l.getChildResidue().getTypeName().equals("GlcNAc")) {
				no_glcnac++;
				glcnac_pos = i;
			} else
				return;
		}

		if (no_glcnac != 1 || no_man != 2)
			return;

		if (glcnac_pos != 1) {
			Linkage help = children.get(1);
			children.set(1, children.get(glcnac_pos));
			children.set(glcnac_pos, help);
		}
	}

	//取り出した1つの単糖情報からデータを作り出す
	private static Residue createFromRINGS(String type, String mod_stereo,
			String subs, String anom, String link) throws Exception {

		// get residue type
		String res_type = gmind_types.get(type);
		if (res_type == null){
			throw new Exception("Unrecognized gmind type: " + type);
		}
		Residue ret = GWSParser.readSubtree(res_type, false);

		// stereochemistry modifications
		if (mod_stereo != null && mod_stereo.length() > 0) {
			if (mod_stereo.equals("'"))
				ret.setChirality((ret.getChirality() == 'D') ? 'L' : 'D');
			else if (mod_stereo.equals("^"))
				ret.setRingSize((ret.getRingSize() == 'p') ? 'f' : 'p');
			else if (mod_stereo.equals("~")) {
				ret.setChirality((ret.getChirality() == 'D') ? 'L' : 'D');
				ret.setRingSize((ret.getRingSize() == 'p') ? 'f' : 'p');
			}
		}

		// anomericity
		if (anom != null && anom.length() > 0)
			ret.setAnomericState(anom.charAt(0));

		// substitutions
		if (subs != null && subs.length() > 1) {
			subs = subs.substring(1, subs.length() - 1);
			Matcher m = gmind_sub_pattern.matcher(subs);
			if (!m.lookingAt())
				throw new Exception("Unrecognized format for substitution: "
						+ subs);

			for (int i = 0; i < m.groupCount(); i += 2) {
				String sub = m.group(i + 2);
				if (sub != null && sub.length() > 0) {
					String sub_type = gmind_types.get(sub);
					if (sub_type == null)
						throw new Exception("Unrecognized gmind type: " + sub);
					Residue ret_sub = ResidueDictionary.newResidue(sub_type);
		
					StringBuilder sb = new StringBuilder();
					sb.append("1");
					sb.append(m.group(i + 1));
					
					ret.addChild(ret_sub, sb.charAt(sb.length()-1));
				}
			}
		}

		// linkage position
		Linkage par_link = new Linkage(null, ret);
		if (link != null && link.length() > 0)
			par_link.setLinkagePositions(parsePositions(link));
		ret.setParentLinkage(par_link);

		return ret;
	}

	static private char[] parsePositions(String str) {
		String[] fields = str.split("/");
		char[] ret = new char[fields.length];
		for (int i = 0; i < fields.length; i++)
			ret[i] = fields[i].charAt(0);
		return ret;
	}

	/**
	* 残基を文字列で書く
	* @param r
	* @param add_uncertain_leaf
	* @return
	*/
	private String writeSubtree(Residue r,int bracket, boolean repetition) {
		//文字列操作のためのsb、最終的にこれの中身を返す
		StringBuilder sb = new StringBuilder();
		
		//rがNullの場合、空の文字列を返す。
		if (r == null)
			return "";
		if( r.isStartRepetition() ){
		}else if( r.isEndRepetition() ) {
		}else if(!r.isSaccharide()){
			return "?";
		}
	
		if( r.getType().isStartRepetition() ){
			//rが繰り返し構造の初めの部分の場合
			repetition = true;
			sb.insert(0,'}');
			sb.insert(0,writeSubtree(r.getChildrenLinkages().getFirst().getChildResidue(),bracket,repetition));
		}else if( r.getType().isEndRepetition() ){
			//rが繰り返し構造の終わりの部分の場合
			sb.insert(0,'{');
			repetition = false;
		}else if(gmind_codes.get(r.getTypeName()) == null){
			// *(未知の単糖)をsbに追加する
			sb.insert(0, '*');
		}else{	
			//gmind_codesにそれに対応したものが存在する場合
			// 結合情報を追加
			if (r.getParentLinkage() != null) {		
				char ppos = r.getParentLinkage().getParentPositionsSingle();
				sb.insert(0, ppos); 
			}
			//rのアノマーをsbに入れる
			sb.insert(0, r.getAnomericState()); 
			Vector<Linkage> modifications = new Vector<Linkage>();
		
			// 子を入れる
			Vector<Linkage> children = new Vector<Linkage>();
			for (Linkage l : r.getChildrenLinkages()) { 
				if( l.getChildResidue().isStartRepetition() ) {
					children.add(l);
				}else if( l.getChildResidue().isEndRepetition() ) {
					children.add(l);
				}else if (l.getChildResidue().isSaccharide()){ 
					children.add(l);	
				}else{
					//残基が単糖でなかった場合、modificationにlを加える
					modifications.add(l); 
				}
			}
			Collections.sort(modifications, new Linkage.LinkageComparator());

			// add modifications
			if (modifications.size() > 0) { 
				StringBuilder msb = new StringBuilder();
				msb.append('[');
				for (Linkage l : modifications) {
					if (gmind_codes.get(l.getChildResidue().getTypeName()) == null)
						sb.append('*'); 
					else {
						
						if(l.getParentPositionsSingle() == '1'){
						}else{
							msb.append(l.getParentPositionsSingle());
						}
						msb.append(gmind_codes.get(l.getChildResidue().getTypeName()).code);
					}
				}
				msb.append(']');

				sb.insert(0, msb.toString());	
			}

			// typeを加える
			SU su = gmind_codes.get(r.getTypeName());
			//suとrのchiralityが一致しない、かつring_sizeが一致しない場合
			if (su.chirality != r.getChirality()&& su.ring_size != r.getRingSize())	
				sb.insert(0, '~');
			else if (su.chirality != r.getChirality())	
				sb.insert(0, '\'');
			else if (su.ring_size != r.getRingSize())	
				sb.insert(0, '^');
			sb.insert(0, su.code);
			
			// 子を加える
			if (children.size() > 0) { 
				for (int i = 1; i < children.size(); i++) {
					//分岐構造が存在する場合
					sb.insert(0, ')');
					//繰り返し構造内である場合、"（）"内を"-"でくくる
					if(repetition)
						sb.insert(0, '-');
					//"()"内の糖鎖を文字列にする
					sb.insert(0,writeSubtree(children.get(i).getChildResidue(),bracket,repetition));
					if(repetition)
						sb.insert(0, '-');
					sb.insert(0, '(');
					
				}
				sb.insert(0,writeSubtree(children.firstElement().getChildResidue(),bracket,repetition));
				
			} else {
				if (bracket > 0 )
					//曖昧な構造が存在する場合、変数"%"を加える
					for(int i=1;bracket>0;bracket--){
						sb.insert(0, i + "%");
						i++;
					}
			}
		}
		return sb.toString();
	}

	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		throw new UnsupportedOperationException();
	}
}