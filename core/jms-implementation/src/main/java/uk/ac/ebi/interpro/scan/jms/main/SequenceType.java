package uk.ac.ebi.interpro.scan.jms.main;

import java.util.HashSet;
import java.util.Set;

/**
 * Sequence type
 */
public enum SequenceType {
    P("p", "Protein sequence"),
    N("n", "Nucleotide sequence");

    private String code;
    private String description;

    SequenceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Set<String> getSequenceTypes() {
        Set<String> seqTypes = new HashSet<String>();
        for (SequenceType seqType : SequenceType.values()) {
            seqTypes.add(seqType.getCode());
        }
        return seqTypes;
    }
}
