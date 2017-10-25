package de.sybig.oba.server.alignment;

import info.debatty.java.stringsimilarity.JaroWinkler;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class LexCompare {

    private final Properties props;
    private HashMap<String, String> replacements;
    private JaroWinkler jaroWinkler;

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

        double score = strictStringComparison(labelA, labelB);
        if (score == 1) {
            return score;
        }
        if (!containsNumber(labelA) && !containsNumber(labelB)) {
            score = jaroWinklerDistance(labelA, labelB);
            if (score > 0.985) {
                return score;
            }
        }
        return 0;
    }

    private double strictStringComparison(String labelA, String labelB) {
        if (labelA.equalsIgnoreCase(labelB)) {
            return 1;
        }
        return 0;
    }

    private double jaroWinklerDistance(String stringA, String stringB) {
        return getJaroWinkler().similarity(stringA, stringB);
    }

    private JaroWinkler getJaroWinkler() {
        if (jaroWinkler == null) {
            jaroWinkler = new JaroWinkler();
        }
        return jaroWinkler;
    }

    private boolean containsNumber(String string) {
        return string.matches(".*[0-9].*");
    }
}
