package org.glycoinfo.WURCSFramework.Glycan;

import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.array.mass.WURCSMassCalculator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSSequence2ToGlycan;

public class WURCSSequence2ToGlycanTester {

	public static void main(String[] args) throws Exception {
		String a_sInput = 
				//"WURCS=2.0/5,16,17/[a2112h-1b_1-5][a344h-1a_1-?][a211h-1a_1-?][a211h-1x_1-?][a211h-1a_1-4]/1-1-1-2-3-1-4-3-1-4-3-1-1-5-4-3/a3-b1_a6-l1_b6-c1_c3-d1_c6-f1_d5-e1_f3-g1_f6-i1_g5-h1_j5-k1_l3-m1_m3-n1_n3-o1_o5-p1_j1-i3|i4_a1-a3~n_b1-b3~n";
				//"G39823IR	WURCS=2.0/9,16,16/[AUd1122h][a11221h-1a_1-5_4*OP^XOP^XOCCN/5O/5=O/3O/3=O][a11221h-1a_1-5][a2122h-1a_1-5][a2112h-1a_1-5][a2122h-1b_1-5][a2112m-1b_1-5_2*NCC/3=O_4*N][a2111A-1a_1-5_2*NCC/3=O][a2122h-1a_1-5_2*N]/1-2-3-4-4-5-5-6-7-8-7-8-7-8-3-9/a5-b1_b3-c1_c3-d1_c7-o1_d3-e1_e2-f1_e3-h1_f2-g1_h3-i1_i3-j1_j4-k1_k3-l1_l4-m1_m3-n1_o7-p1_k1-l4~n";
				//"G39625MY	WURCS=2.0/3,31,31/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]/1-1-2-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3-3/a4-b1_b4-c1_c3-d1_c6-B1_d2-e1_d6-h1_e2-f1_f3-g1_h2-i1_h6-k1_i3-j1_k2-l1_k6-q1_l2-m1_l6-o1*OPO*/3O/3=O_m3-n1_o3-p1_q2-r1_q6-u1_r2-s1_s3-t1_u2-v1_u6-x1_v2-w1_x2-y1_x6-z1_z2-A1_B3-C1_B6-D1_D2-E1_k1-x6~n";
				//"WURCS=2.0/2,2,2/[h4344h_2*NCC/3=O][a2112h-1b_1-5]/1-2/a4-b1_a6-b2";
				
				//"WURCS=2.0/2,5,0+/[uxxxxh_2*NCC/3=O][uxxxxh]/1-1-2-2-2/";
				
				//"WURCS=2.0/4,10,9/[a2122h-1a_1-5][ha122h-2b_2-5][a2122h-1b_1-5][a2112h-1a_1-5]/1-2-3-3-3-3-3-3-3-4/a1-b2_a6-j1_b6-c1_c4-d1_d4-e1_e4-f1_f4-g1_g4-h1_h4-i1";
				//"WURCS=2.0/4,5,4/[u2122h_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5]/1-2-3-4-4/a4-b1_b4-c1_c3-d1_c6-e1";
				//"WURCS=2.0/2,3,4/[a2122h-1a_1-5_2*N][a2122h-1a_1-5]/1-2-2/a1-c4_a4-b1_b4-c1_c1-c4~n";
				//"WURCS=2.0/1,7,7/[a2122h-1a_1-5_2*OC_3*OC_6*N]/1-1-1-1-1-1-1/a1-g4_a4-b1_b4-c1_c4-d1_d4-e1_e4-f1_f4-g1";
				//"WURCS=2.0/2,6,5/[a2122h-1a_1-5][ha122h-2b_2-5]/1-2-2-2-2-2/a1-b2_b6-c2_c1-d2_c6-e2_e6-f2";
				//"WURCS=2.0/2,4,6/[a2122h-1a_1-5][a2122A-1a_1-5]/1-2-1-2/a1-d4_a4-b1_b4-c1_c4-d1_a1-a4~n_c1-c4~n";
				//"WURCS=2.0/2,4,3/[a2112h-1a_1-5_2*NCC/3=O_3*OCC/3=O][a2112h-1b_1-5_2*NCC/3=O]/1-2-1-2/a4-b1_b3-c1*OPO*/3O/3=O_c4-d1";
				//"WURCS=2.0/4,13,17/[a122h-1x_1-4][a2112h-1x_1-4][a2112h-1x_1-5][a1122h-1x_1-5]/1-1-2-3-1-1-1-1-1-1-1-4-4/a5-b1_c5-d1_c6-l1_d4-e1_e5-f1_f5-j1_g2-h1_h5-i1_j5-k1_l2-m1_g1-f2|f3_b?-c1_a1-k5~n_e1-e5~n_h1-h5~n_j1-j5~n_b1-b?~n";
				//"WURCS=2.0/6,15,14/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][a2112m-1a_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-1-2-3-1-4-1-4-3-1-4-5-6-6-6/a4-b1_a6-l1_b4-c1_c3-d1_c6-i1_d2-e1_d4-g1_e4-f1_g4-h1_i2-j1_j4-k1_m2-f3|h3|k3}_n2-f3|h3|k3}_o2-f6|h6|k6}";
				
				//"WURCS=2.0/6,11,10/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][a1221m-1a_1-5]/1-2-3-4-2-5-4-2-6-2-5/a4-b1_a6-i1_b4-c1_c3-d1_c6-g1_d2-e1_e4-f1_g2-h1_j4-k1_j1-d4|d6|g4|g6}";
				//"WURCS=2.0/2,5,4/[u2122h_2*NCC/3=O][u1122h]/1-1-2-2-2/a?|b?|c?|d?|e?}-{a?|b?|c?|d?|e?_a?|b?|c?|d?|e?}-{a?|b?|c?|d?|e?_a?|b?|c?|d?|e?}-{a?|b?|c?|d?|e?_a?|b?|c?|d?|e?}-{a?|b?|c?|d?|e?";
				//"WURCS=2.0/4,5,4/[a2112h-1x_1-5_2*NCC/3=O][a2112h-1x_1-5][a2122h-1x_1-5_2*NCC/3=O][Aad21122h-2x_2-6_5*NCCO/3=O]/1-2-3-2-4/a3-b1_a6-c1_c4-d1_e2-a3|b3|c3|d3}";
				//"WURCS=2.0/4,5,4/[h2112h_2*NCC/3=O][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O][Aad21122h-2x_2-6_5*NCC/3=O]/1-2-3-3-4/a3-b1_a6-d2_b3-c2_e2-a?|b?|c?|d?}";
				//"WURCS=2.0/7,10,9/[h2122h_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2122h-1x_1-5_2*NCC/3=O][a1221m-1x_1-5][a2112h-1x_1-5]/1-2-3-4-4-5-6-7-5-7/a4-b1_b4-c1_c3-d1_c6-e1_f?-g1_f?-h1_i?-j1_f1-a?|b?|c?|d?|e?}_i1-a?|b?|c?|d?|e?}";
				
				//"WURCS=2.0/9,20,19/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][a2112h-1a_1-5][Aad21122h-2a_2-6_5*NCCO/3=O][Aad21122h-2a_2-6_5*NCC/3=O][a1221m-1a_1-5]/1-2-3-4-2-5-6-2-5-2-4-2-5-2-5-7-2-5-8-9/a4-b1_a6-t1_b4-c1_c3-d1_c4-j1_c6-k1_d2-e1_d4-h1_e4-f1_f3-g1_h4-i1_k2-l1_k6-q1_l4-m1_m3-n1_n4-o1_q4-r1_p2-o3|o6_s2-r3|r6";
				//"WURCS=2.0/2,2,1/[a2112h-1x_1-5][a2112h-1a_1-5_3*C]/1-2/a4-b1";
				//"WURCS=2.0/1,1,0/[Aad21122h-2a_2-6_5*NCC/3=O_9*ON]/1/";
				"WURCS=2.0/2,2,1/[a1221m-1x_1-5_2*NCN/3=N][Aad12212m-2a_2-6_5*NCC/3=O_7*NCC/3=O]/1-2/a3-b2";
				
		WURCSSequence2ToGlycan a_oWG2G = new WURCSSequence2ToGlycan();
		WURCSFactory a_oWF = new WURCSFactory(a_sInput);
		BuilderWorkspace a_objBW = new BuilderWorkspace(new GlycanRendererAWT());
		a_objBW.initData();

		a_oWG2G.start(a_oWF, null);
		System.out.println(a_oWG2G.getGlycan());
		a_oWG2G.getGlycan().getMassOptions().setDerivatization("Und");
		a_oWG2G.getGlycan().getMassOptions().ION_CLOUD.set("Na", 0);
		
		System.out.println(a_oWG2G.getGlycan().computeMZ() + " " + WURCSMassCalculator.calcMassWURCS(a_oWF.getArray()).doubleValue());
	}
}
