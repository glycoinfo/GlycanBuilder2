package org.glycoinfo.application.glycanbuilder.util.exchange.importer.glycontainer2glycan;

import org.eurocarbdb.application.glycanbuilder.Residue;
import org.glycoinfo.GlycanFormatconverter.Glycan.*;
import org.glycoinfo.GlycanFormatconverter.util.TrivialName.ModifiedMonosaccharideDescriptor;
import org.glycoinfo.GlycanFormatconverter.util.TrivialName.TrivialNameDictionary;

import java.util.HashMap;

public class NodeAnalyzer {

    private final HashMap<String, Edge> subList;
    private final HashMap<String, GlyCoModification> modList;

    public NodeAnalyzer () {
        this.subList = new HashMap<>();
        this.modList = new HashMap<>();
    }

    public Node start (Node _node, Residue _residue) throws GlycanException {
        // extract current subtituents
        this.extractSubstituents(_node);

        // extract current modifications
        this.extractModifications(_node);

        this.removeCoreSubstituent(_node, _residue);

        return _node;
    }

    private void extractSubstituents (Node _node) {
        Monosaccharide mono = (Monosaccharide) _node;

        for (Edge edge : mono.getChildEdges()) {
            if (edge.getSubstituent() == null) continue;

            Substituent sub = (Substituent) edge.getSubstituent();
            if (edge.getSubstituent() instanceof GlycanRepeatModification) continue;
            if (sub.getSubstituent() instanceof BaseCrossLinkedTemplate) continue;
            if (edge.getGlycosidicLinkages().get(0).getParentLinkages().size() != 1) continue;

            subList.put(edge.getGlycosidicLinkages().get(0).getParentLinkages().get(0) + sub.getSubstituent().getIUPACnotation(), edge);
        }
    }

    private void extractModifications (Node _node) {
        Monosaccharide mono = (Monosaccharide) _node;

        for (GlyCoModification gMod : mono.getModifications()) {
            modList.put(gMod.getPositionOne() + String.valueOf(gMod.getModificationTemplate().getCarbon()), gMod);
        }
    }

    private Node removeCoreSubstituent (Node _node, Residue _residue) throws GlycanException {
        String trivialName = _residue.getTypeName();

        // remove core substituent and modification
        TrivialNameDictionary trivialNameDictionary = TrivialNameDictionary.forThreeLetterCode(trivialName);
        if (trivialNameDictionary != null) {
            // for substituent
            String subNotations = trivialNameDictionary.getSubstituents();
            if (!subNotations.equals("")) {
                for (String item : subNotations.split("_")) {
                    String[] items = item.split("\\*");
                    this.removeModifications(_node, items[0] + items[1]);
                }
            }

            // for modification
            String modNotations = trivialNameDictionary.getModifications();
            if (!modNotations.equals("")) {
                for (String item : modNotations.split("_")) {
                    String[] items = item.split("\\*");
                    this.removeModifications(_node, items[0] + items[1]);
                }
            }
        }

        ModifiedMonosaccharideDescriptor modMonoDesc = ModifiedMonosaccharideDescriptor.forTrivialName(trivialName);
        if (modMonoDesc != null) {
            // for substituent
            String subNotations = modMonoDesc.getSubstituents();
            if (!subNotations.equals("")) {
                for (String item : subNotations.split("_")) {
                    String[] items = item.split("\\*");
                    if (item.equals("5*NAc") || item.equals("5*NGc")) {
                        this.removeModifications(_node, items[0] + items[1].replaceFirst("N", ""));
                    } else {
                        this.removeModifications(_node, items[0] + items[1]);
                    }
                }
            }

            // for modification
            String modNotations = modMonoDesc.getModifications();
            if (!modNotations.equals("")) {
                for (String item : modMonoDesc.getModifications().split("_")) {
                    String[] items = item.split("\\*");
                    this.removeModifications(_node, items[0] + items[1]);
                }
            }
        }

        return _node;
    }

    private void removeModifications (Node _acceptor, String _key) throws GlycanException {
        Monosaccharide mono = (Monosaccharide) _acceptor;

        for (String key : subList.keySet()) {
            if (key.equals(_key)) {
                mono.removeChildEdge(subList.get(key));
            }
        }

        for (String key : modList.keySet()) {
            if (key.equals(_key)) {
                mono.removeModification(modList.get(_key));
            }
        }
    }
}
