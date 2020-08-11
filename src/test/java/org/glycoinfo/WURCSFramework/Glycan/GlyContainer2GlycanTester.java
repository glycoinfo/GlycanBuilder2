package org.glycoinfo.WURCSFramework.Glycan;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.glycoinfo.GlycanFormatconverter.Glycan.GlyContainer;
import org.glycoinfo.GlycanFormatconverter.io.WURCS.WURCSImporter;
import org.glycoinfo.application.glycanbuilder.util.exchange.importer.glycontainer2glycan.GlyContainer2Glycan;
import org.junit.Test;

public class GlyContainer2GlycanTester {

    @Test
    public void simple () {
        //https://glytoucan.org/Structures/Glycans/G00027JG
        String wurcs = "WURCS=2.0/4,6,5/[a2112h-1b_1-5_2*NCC/3=O][a2112h-1b_1-5][a1221m-1a_1-5][a2122h-1b_1-5_2*NCC/3=O]/1-2-3-4-2-3/a3-b1_a6-d1_b2-c1_d4-e1_e2-f1";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void fuzzyLinkage () {
        //https://glytoucan.org/Structures/Glycans/G00140YZ
        String wurcs = "WURCS=2.0/4,5,4/[h2112h_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a2112h-1b_1-5][a1221m-1a_1-5]/1-2-3-3-4/b4-c1_d2-e1_b1-a3|a6_d1-a3|a6";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void substituent () {
        //https://glytoucan.org/Structures/Glycans/G00020MO
        String wurcs = "WURCS=2.0/3,4,3/[u2122h_2*NCC/3=O_6*OSO/3=O/3=O][a2112h-1b_1-5][a2122h-1b_1-5_2*NCC/3=O_6*OSO/3=O/3=O]/1-2-3-2/a4-b1_b3-c1_c4-d1";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void cyclicSubstituent () {
        //https://glytoucan.org/Structures/Glycans/G13561HU
        String wurcs = "WURCS=2.0/3,3,2/[a2221m-1x_1-5][a2211m-1a_1-5][a2122h-1b_1-5_4-6*OC^XO*/3CO/6=O/3C_3*OC]/1-2-3/a2-b1_b3-c1";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void repeat () {
        //https://glytoucan.org/Structures/Glycans/G00003VQ
        String wurcs = "WURCS=2.0/4,5,5/[a2112h-1b_1-5_2*NCC/3=O][a2122A-1b_1-5][a2112h-1a_1-5][a2122h-1b_1-5]/1-2-1-3-4/a3-b1_b4-c1_c4-d1_d3-e1_a1-e4~n";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void fragment () {
        //https://glytoucan.org/Structures/Glycans/G00025YC
        String wurcs = "WURCS=2.0/6,11,10/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][a1221m-1a_1-5]/1-2-3-4-2-5-2-5-4-2-6/a4-b1_a6-k1_b4-c1_c3-d1_c6-i1_d2-g1_e4-f1_g4-h1_i2-j1_e1-d4|d6|i4|i6}";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void bridge () {
        //https://glytoucan.org/Structures/Glycans/G00019BE
        String wurcs = "WURCS=2.0/5,5,5/[a2211m-1b_1-5][a2112h-1b_1-5][a2211m-1a_1-5][o2h][a2122h-1b_1-5]/1-2-3-4-5/a4-b1_b2-c1_b3-d2*OPO*/3O/3=O_b4-e1_a1-e4~n";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void bridgeRepeat () {
        //TODO: 親子関係が不適切
        //https://glytoucan.org/Structures/Glycans/G42377JL
        String wurcs = "WURCS=2.0/2,4,4/[hxh][a2112h-1x_1-5_2*NCC/3=O]/1-1-1-2/a1-b1*OPO*/3O/3=O_b3-c1*OPO*/3O/3=O_c2-d1_a3-c3*OPO*/3O/3=O~n";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void cyclic () {
        //https://glytoucan.org/Structures/Glycans/G00048ZA
        String wurcs = "WURCS=2.0/1,7,7/[a2122h-1a_1-5_2*OC_3*OC_6*N]/1-1-1-1-1-1-1/a1-g4_a4-b1_b4-c1_c4-d1_d4-e1_e4-f1_f4-g1";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void composition () {
        //String wurcs = "WURCS=2.0/4,15,14/[AUd21122h_5*NCC/3=O][uxxxxh_2*NCC/3=O][uxxxxh][u1221m]/1-2-2-2-2-2-2-3-3-3-4-4-4-4-4/a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?}-{a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?|m?|n?|o?";
        String wurcs = "WURCS=2.0/4,15,0+/[AUd21122h_5*NCC/3=O][uxxxxh_2*NCC/3=O][uxxxxh][u1221m]/1-2-2-2-2-2-2-3-3-3-4-4-4-4-4/";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void modification () {
        //TODO: 取れてない
        //https://glytoucan.org/Structures/Glycans/G00173GD
        String wurcs = "WURCS=2.0/4,4,3/[a2122h-1a_1-5_2*NSO/3=O/3=O][a2121A-1a_1-5_2*OSO/3=O/3=O][a2122h-1a_1-5_2*NSO/3=O/3=O_6*OSO/3=O/3=O][a21EEA-1a_1-5_2*OSO/3=O/3=O]/1-2-3-4/a4-b1_b4-c1_c4-d1";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void fragmentSubstituent () {
        //https://glytoucan.org/Structures/Glycans/G00050XR
        String wurcs = "WURCS=2.0/7,12,14/[a2122h-1x_1-5_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][axxxxh-1x_1-5_2*NCC/3=O][a2112h-1b_1-5][Aad21122h-2a_2-6_5*NCC/3=O]/1-2-3-4-5-6-7-7-4-5-6-7/a4-b1_b4-c1_e4-f1_f3-g2_g8-h2_j4-k1_k3-l2_c?-d1_c?-i1_d?-e1_i?-j1_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?}*OCC/3=O_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?}*OCC/3=O_a?|b?|c?|d?|e?|f?|g?|h?|i?|j?|k?|l?}*OCC/3=O";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void anhydro () {
        //https://glytoucan.org/Structures/Glycans/G07716WO
        String wurcs = "WURCS=2.0/3,3,2/[u2122h][a2222h-1a_1-5_2-3][a2122h-1a_1-5]/1-2-3/a4-b1_b4-c1";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    @Test
    public void deoxy () {
        //https://glytoucan.org/Structures/Glycans/G12207CL
        String wurcs = "WURCS=2.0/2,2,1/[a2122h-1x_1-5][a21d2h-1a_1-5]/1-2/a4-b1";
        GlyContainer gc = makeGlyContainer(wurcs);
        makeGlycan(gc);
    }

    public GlyContainer makeGlyContainer (String _wurcs) {
        try {
            WURCSImporter wi = new WURCSImporter();
            wi.start(_wurcs);
            return wi.getConverter().getGlycan();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Glycan makeGlycan (GlyContainer _gc) {
        try {
            GlyContainer2Glycan gc2g = new GlyContainer2Glycan();
            return null;
            //return gc2g.start(_gc, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
