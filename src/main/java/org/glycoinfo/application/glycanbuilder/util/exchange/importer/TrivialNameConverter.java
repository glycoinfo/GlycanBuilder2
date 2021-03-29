package org.glycoinfo.application.glycanbuilder.util.exchange.importer;

import org.glycoinfo.GlycanFormatconverter.Glycan.*;
import org.glycoinfo.GlycanFormatconverter.io.IUPAC.IUPACNotationConverter;
import org.glycoinfo.GlycanFormatconverter.io.IUPAC.extended.ExtendedConverter;
import org.glycoinfo.GlycanFormatconverter.util.TrivialName.ModifiedMonosaccharideDescriptor;
import org.glycoinfo.GlycanFormatconverter.util.exchange.WURCSGraphToGlyContainer.WURCSGraphToGlyContainer;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSGraph;
import org.glycoinfo.WURCSFramework.wurcs.sequence2.GRES;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrivialNameConverter {

    private Node node;
    private String trivialName;
    private String fullName;
    private final ArrayList<String> modifications = new ArrayList<>();

    public Node getNode () {
        return this.node;
    }

    public String getTrivialName () {
        return this.trivialName;
    }

    public String getIUPACNotation () {
        String ret = this.fullName;

        ret = ret.replaceAll(AnomericStateDescriptor.ALPHA.getIUPACAnomericState(), "\u03B1");
        ret = ret.replaceAll(AnomericStateDescriptor.BETA.getIUPACAnomericState(), "\u03B2");

        return ret;
    }

    public ArrayList<String> getModifications () {
        return this.modifications;
    }

    public void start (GRES _gres) throws GlycanException, WURCSException {
        String wurcs = "WURCS=2.0/1,1,0/[" +_gres.getMS().getString() + "]/1/";
        WURCSFactory wf = new WURCSFactory(wurcs);
        WURCSGraph graph = wf.getGraph();

        WURCSGraphToGlyContainer wg2gc = new WURCSGraphToGlyContainer();
        wg2gc.start(graph);
        GlyContainer gc = wg2gc.getGlycan();

        Node node = gc.getAllNodes().get(0);
        this.node = node;

        this.makeTrivialName(node);
        this.makeIUPACNotation(node);
        this.parseModifications(_gres);
    }

    private void makeTrivialName (Node _node) {
        IUPACNotationConverter inConv = new IUPACNotationConverter();
        try {
            inConv.makeTrivialName(_node);
        } catch (GlycanException e) {
            e.printStackTrace();
        }
        this.trivialName = inConv.getCoreCode();
    }

    public void makeIUPACNotation (Node _node) {
        try {
            ExtendedConverter extConv = new ExtendedConverter();
            this.fullName = extConv.start(_node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseModifications (GRES _gres) {
        String skeletonCode = _gres.getMS().getCoreStructure().getSkeletonCode();
        String unsaturation = "";
        ArrayList<Integer> a_aPoss = new ArrayList<>();

        Monosaccharide mono = (Monosaccharide) this.node;
        ModifiedMonosaccharideDescriptor monosaccharideDescriptor = ModifiedMonosaccharideDescriptor.forTrivialName(this.trivialName);

        if (monosaccharideDescriptor == null) {
            Matcher matMono = Pattern.compile(".+(HexA)").matcher(this.trivialName);
            if (matMono.find()) {
                monosaccharideDescriptor = ModifiedMonosaccharideDescriptor.HEXA;
            }
        }

        for(int i = 0; i < skeletonCode.length(); i++) {
            ModificationTemplate modTemp = ModificationTemplate.forCarbon(skeletonCode.charAt(i));
            if (modTemp == null) continue;
            if (modTemp.equals(ModificationTemplate.HLOSE_5) || modTemp.equals(ModificationTemplate.HLOSE_6) ||
                    modTemp.equals(ModificationTemplate.HLOSE_7) || modTemp.equals(ModificationTemplate.HLOSE_8) ||
                    modTemp.equals(ModificationTemplate.HLOSE_X)) continue;

            int pos = i + 1;

            if(skeletonCode.charAt(i) == 'e' ||
                    skeletonCode.charAt(i) == 'E' ||
                    skeletonCode.charAt(i) == 'f' ||
                    skeletonCode.charAt(i) == 'F' ||
                    skeletonCode.charAt(i) == 'z' ||
                    skeletonCode.charAt(i) == 'Z') {
                a_aPoss.add(pos);
                continue;
            }

            if (monosaccharideDescriptor != null) {
                if (pos == 2 && skeletonCode.charAt(i) == 'U') continue;
                if (monosaccharideDescriptor.getModifications().contains(pos + "*" + skeletonCode.charAt(i))) continue;
            }
            if (skeletonCode.charAt(i) == 'h') {
                if (pos == mono.getSuperClass().getSize() || i == 0) continue;
            }
            if (skeletonCode.charAt(i) == 'a') {
                if (pos == mono.getAnomericPosition()) continue;
            }
            if (skeletonCode.charAt(i) == 'o') {
                if (pos == 1 && mono.getAnomer().equals(AnomericStateDescriptor.OPEN)) continue;
            }
            this.modifications.add(pos + "*" + skeletonCode.charAt(i));
        }

        for(Iterator<Integer> i = a_aPoss.iterator(); i.hasNext();) {
            unsaturation += i.next();
            if(i.hasNext()) unsaturation +=",";
        }

        if(unsaturation.length() != 0) {
            unsaturation += "*en";
            this.modifications.add(unsaturation);
        }
    }
}
