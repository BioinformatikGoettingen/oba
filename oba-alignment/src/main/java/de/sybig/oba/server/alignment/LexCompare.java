package de.sybig.oba.server.alignment;

import de.sybig.oba.server.ObaClass;
import java.util.HashMap;
import java.util.Properties;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class LexCompare {

    private final Properties props;
    private HashMap<String, String> replacements;

    public LexCompare(Properties properties) {
        this.props = properties;
        initReplacements();
    }

    private void initReplacements() {
        String allReplacements = props.getProperty("alignment_text_replace", "");
        replacements = new HashMap<>();
        for (String replacement : allReplacements.split(";")) {
            String[] parts = replacement.split(":");
            replacements.put(parts[0], parts[1]);
//            System.out.println(parts[0]+" will be replaced by -" +parts[1] + "-");
        }
    }

    public double compareLabels(String labelA, String labelB) {

        if (labelA == null || labelB == null) {
            return 0;
        }
        String replacement;
        for (String pattern : replacements.keySet()) {
            replacement = replacements.get(pattern);
            labelA = labelA.replaceAll(pattern, replacement);
            labelB = labelB.replaceAll(pattern, replacement);
        }

        if (labelA.equalsIgnoreCase(labelB)) {
            return 1;
        }
        return 0;
    }
}
