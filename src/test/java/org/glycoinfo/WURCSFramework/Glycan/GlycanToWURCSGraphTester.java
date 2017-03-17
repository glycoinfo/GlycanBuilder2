package org.glycoinfo.WURCSFramework.Glycan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.WURCSFormatException;
import org.glycoinfo.WURCSFramework.util.exchange.ConverterExchangeException;
import org.glycoinfo.WURCSFramework.util.exchange.Carbbank.ConverterCarbBankException;
import org.glycoinfo.application.glycanbuilder.util.exchange.GlycanToWURCSGraph;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSSequence2ToGlycan;

public class GlycanToWURCSGraphTester {
	
	/** repeating */
	//WURCS=2.0/3,3,3/[a2211m-1a_1-5][a2122h-1a_1-5][a1122h-1b_1-5_2*NCC/3=O]/1-2-3/a2-b1_b4-c1_a1-c?~n
	//WURCS=2.0/2,4,4/[a2122h-1a_1-5][ha122h-2b_2-5]/1-1-2-2/a1-b1_b6-c2_c1-d2_c1-c2~n
	/** repeating nested*/
	//WURCS=2.0/4,13,17/[a122h-1x_1-4][a2112h-1x_1-4][a2112h-1x_1-5][a1122h-1x_1-5]/1-1-2-3-1-1-1-1-1-1-1-4-4/a5-b1_c5-d1_c6-l1_d4-e1_e5-f1_f5-j1_g2-h1_h5-i1_j5-k1_l2-m1_g1-f2|f3_b?-c1_a1-k5~n_e1-e5~n_h1-h5~n_j1-j5~n_b1-b?~n
	/** cyclic */
	//WURCS=2.0/2,3,4/[a2122h-1a_1-5_2*N][a2122h-1a_1-5]/1-2-2/a1-c4_a4-b1_b4-c1_c1-c4~n
	//WURCS=2.0/1,7,7/[a2122h-1a_1-5_2*OC_3*OC_6*N]/1-1-1-1-1-1-1/a1-g4_a4-b1_b4-c1_c4-d1_d4-e1_e4-f1_f4-g1
	/** normal */
	//WURCS=2.0/1,1,0/[o212h]/1/
	//WURCS=2.0/3,3,2/[h222h][a2112A-1a_1-5][a2122m-1b_1-5_3*N]/1-2-3/a2-b1_b4-c1
	//WURCS=2.0/2,2,1/[u2112h_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]/1-2/a3-b1
	/** ambiguous */
	//WURCS=2.0/6,15,14/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a1221m-1a_1-5][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-1-2-3-3-4-1-4-5-6-1-4-5-1-5/a4-b1_a6-f1_b4-c1_c3-d1_c6-e1_g3-h1_g4-i1_k3-l1_k4-m1_n4-o1_i?-j2_g1-a?|b?|c?|d?|e?|f?}_k1-a?|b?|c?|d?|e?|f?}_n1-a?|b?|c?|d?|e?|f?}
	//WURCS=2.0/9,14,13/[a2122h-1x_1-5_2*NCC/3=O][a1221m-1a_1-5][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O][axxxxh-1x_1-?_2*NCC/3=O][axxxxh-1x_1-?]/1-2-3-4-5-3-6-7-5-3-6-7-8-9/a4-c1_c4-d1_d3-e1_d6-i1_e2-f1_i2-j1_a?-b1_f?-g1_g?-h2_j?-k1_k?-l2_m?-a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?}_n?-a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?}
	/** anhydro */
	//WURCS=2.0/2,4,3/[a1122h-1x_1-5_2-5][a1122h-1a_1-5]/1-2-2-2/a4-b1_b6-c1_c2-d1
	/** composition */
	//WURCS=2.0/3,9,0+/[uxxxxh_2*NCC/3=O][axxxxh-1x_1-5_?*][axxxxh-1x_1-5]/1-1-1-1-2-3-3-3-3/
	/** bridge modification */
	//WURCS=2.0/5,5,5/[a2211m-1b_1-5][a2112h-1b_1-5][a2211m-1a_1-5][o2h][a2122h-1b_1-5]/1-2-3-4-5/a4-b1_b2-c1_b3-d2*OPO*/3O/3=O_b4-e1_a1-e4~n
	//WURCS=2.0/4,4,4/[a2122h-1a_1-5_2*NCC/3=O][a2112h-1b_1-4_2*OCC/3=O][a2122h-1b_1-5_2*NCC/3=O][h2h]/1-2-3-4/a3-b1_b3-c1_c4-d1*OPO*/3O/3=O_a1-d2~n
	//WURCS=2.0/5,5,5/[a2122h-1a_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a2122h-1b_1-5][a2122h-1a_1-5][a2211m-1b_1-5]/1-2-3-4-5/a3-b1_a4-e1_b3-c1_c2-d1_a1-d6*OPO*/3O/3=O~n
	/** probability linkage */
	//WURCS=2.0/2,4,4/[a1122m-1a_1-5][a2112m-1a_1-4]/1-1-1-2/a2-b1_b3-c1_d1-b4%.35%_a1-c3~n
	/** complex monosaccharide : ex. D-gro-D-talOct*/
	//WURCS=2.0/3,3,2/[Aad1122h-2a_2-6][Aa11122h-2a_2-6][a211h-1a_1-5_4*N]/1-2-3/a4-b2_b8-c1
	//WURCS=2.0/10,15,14/[Aad1122h-2a_2-6_4*OPO/3O/3=O][a11221h-1a_1-5][a2122m-1b_1-5_2*NCC/3=O][a2112A-1a_1-5][a2122h-1b_1-5_2*NCC/3=O][a2112h-1b_1-5_4-6*OPO*/3O/3=O][a1d21m-1a_1-5][a2122h-1a_1-5][a2122h-1b_1-5][ha122h-2b_2-5]/1-2-2-2-3-4-5-6-7-7-8-2-9-10-8/a5-b1_b3-c1_b4-m1_b6-o1_c2-d1_c6-l1_d3-e1_d7-k1_e3-f1_f4-g1_g3-h1_g4-j1_h2-i1_m6-n2
	/** swap */
	//WURCS=2.0/5,6,6/[a1122h-1a_1-5][a2122h-1b_1-5_4n2-6n1*1OC^RO*2/3CO/6=O/3C][a2211m-1a_1-5][a2122h-1b_1-5][a2122A-1a_1-5]/1-2-3-4-1-5/a2-b1_a3-c1_a4-d1_d3-e1_e3-f1_a1-f4~n
	//WURCS=2.0/6,8,8/[a2211m-1a_1-5][a2122A-1b_1-5][a2122h-1b_1-5][a1122h-1b_1-5][a2112h-1a_1-5][a1122h-1b_1-5_4n2-6n1*1OC^RO*2/3CO/6=O/3C]/1-2-3-4-3-5-3-6/a2-b1_a4-c1_c4-d1_d4-e1_e4-f1_f4-g1_g6-h1_a1-g3~n

	//WURCS=2.0/5,5,5/[a2122A-1b_1-5][a2112h-1a_1-5_2*NCC/3=O][a2112A-1b_1-5_3*OCC/3=O_4*OCC/3=O][a2122h-1b_1-5_2*NCC/3=O][hxh]/1-2-3-4-5/a4-b1_b3-c1_c2-d1_c6-e2*N*_a1-d3~n
	//WURCS=2.0/4,4,3/[a2122h-1b_1-5][a2211m-1a_1-5][a212h-1b_1-5][a26h-1b_1-4_3*CO]/1-2-3-4/a2-b1_a3-d1_b4-c1
	
	//WURCS=2.0/3,3,3/[a1221m-1a_1-5_2*N][Aad21122h-2a_2-6_5*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O_6*OCC/3=O]/1-2-3/a3-b2_b7-c1_a1-c3~n
	
	//WURCS=2.0/5,6,6/[a2112h-1a_1-5][a2122A-1b_1-5][a2122h-1b_1-5][a1122h-1a_1-5][a2122h-1b_1-5_4-6*OC^XO*/3CO/6=O/3C]/1-2-3-4-4-5/a3-b1_b3-c1_b4-e1_c3-d1_e2-f1_a1-d2~n
	
	//WURCS=2.0/1,2,1/[a2122h-1b_1-5]/1-1/a?-b3*OSO*/3=O/3=O
	
	private static WURCSSequence2ToGlycan a_oWS22G;
	private static GlycanToWURCSGraph a_oG2WG;
	private static String a_sOutputStatus = "";	
	
	
	public static void main(String[] args) throws WURCSFormatException, ConverterExchangeException, ConverterCarbBankException, Exception {	

		/** init a data set of GlycanBuilder*/
		BuilderWorkspace a_objBW = new BuilderWorkspace(new GlycanRendererAWT());
		a_objBW.initData();

		String a_sPath = "src/test/java/WURCS2.0Sample";
		
		if(new File(a_sPath).isFile()) {
			LinkedHashMap<String, String> wurcsIndex = openString(a_sPath);
			for(String key : wurcsIndex.keySet()) {
				GlycanWriterWURCS2(key, wurcsIndex.get(key));
			} 
		}else if(args.length > 0) {
			for(String a_sInput : args) {
				GlycanWriterWURCS2("", a_sInput);
			}
		}else {
			throw new Exception("This file is not found !");
		}
		System.out.println(a_sOutputStatus);
	}
	
	private static void GlycanWriterWURCS2(String a_sAccessionID, String a_sInput) {
		try {
			a_oWS22G = new WURCSSequence2ToGlycan();
			a_oG2WG = new GlycanToWURCSGraph();

			/** import method */
			WURCSFactory a_oWF = new WURCSFactory(a_sInput);
			a_oWS22G.start(a_oWF, null);
			
			/** export method */
			a_oG2WG.start(a_oWS22G.getGlycan());
			a_oWF = new WURCSFactory(a_oG2WG.getGraph());

			if(!a_sInput.equals(a_oWF.getWURCS())) {
				a_sOutputStatus += a_sAccessionID + "	" + a_sInput + "\n";
				a_sOutputStatus += a_sAccessionID + "	" + a_oWF.getWURCS() + "\n";
			}/*else 
				a_sOutputStatus += a_sAccessionID + "\n";
			*/
		} catch (Exception e) {
			e.printStackTrace();
			a_sOutputStatus += e.getMessage() + "\n";
			a_sOutputStatus += a_sAccessionID + "	" + a_sInput + "\n";			
		}
	}
	
	/**
	 * 
	 * @param a_strFile
	 * @return
	 * @throws Exception
	 */
	private static LinkedHashMap<String, String> openString(String a_strFile) throws Exception {
		try {
			return readWURCS(new BufferedReader(new FileReader(a_strFile)));
		}catch (IOException e) {
			throw new Exception();
		}
	}

	/**
	 * 
	 * @param a_bfFile
	 * @return
	 * @throws IOException
	 */
	private static LinkedHashMap<String, String> readWURCS(BufferedReader a_bfFile) throws IOException {
		String line = "";
		LinkedHashMap<String, String> wret = new LinkedHashMap<String, String>();
		wret.clear();

		while((line = a_bfFile.readLine()) != null) {
			line.trim();
			if(line.indexOf("WURCS") != -1) {
				if(line.indexOf(" ") != -1) line = line.replace(" ", "\t"); 
				String[] IDandWURCS = line.split("\t");
				if (IDandWURCS.length == 2) {
					wret.put(IDandWURCS[0].trim(), IDandWURCS[1]);
				}
			}
		}
		a_bfFile.close();

		return wret;
	}
}
