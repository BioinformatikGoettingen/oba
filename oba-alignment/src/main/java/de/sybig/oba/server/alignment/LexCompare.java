package de.sybig.oba.server.alignment;

import info.debatty.java.stringsimilarity.JaroWinkler;
import java.util.HashMap;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class LexCompare {

    private final Properties props;
    private HashMap<String, String> replacements;
    private JaroWinkler jaroWinkler;
    private final static Logger log = LoggerFactory.getLogger(LexCompare.class);

    public LexCompare(Properties properties) {
        this.props = properties;
    }

    private HashMap<String, String> getReplacements() {
        if (replacements == null) {
            String allReplacements;
            if (props != null) {
                allReplacements = props.getProperty("alignment_text_replace", "");
            } else {
                allReplacements = "";
                log.error("Can not get list of string replacements for string comparison, because properties are null.");
            }
            replacements = new HashMap<>();
            if (allReplacements.contains(":")) {
                for (String replacement : allReplacements.split(";")) {
                    String[] parts = replacement.split(":");
                    replacements.put(parts[0], parts[1]);
                }
            }
        }
        return replacements;
    }

    public ScoreWithSource compareLabels(String labelA, String labelB) {

        if (labelA == null || labelB == null) {
            return new ScoreWithSource(0);
        }
        String replacement;
        for (String pattern : getReplacements().keySet()) {
            replacement = getReplacements().get(pattern);
            labelA = labelA.replaceAll(pattern, replacement);
            labelB = labelB.replaceAll(pattern, replacement);
        }
        labelA = labelA.toLowerCase();
        labelB = labelB.toLowerCase();
        double score = strictStringComparison(labelA, labelB);
        if (score == 1) {
            return new ScoreWithSource(1, Methods.LABEL_EQUAL);
        }
        if (!containsNumber(labelA) && !containsNumber(labelB)) {
            score = jaroWinklerDistance(labelA, labelB);
            if (score > 0.985) {
                return new ScoreWithSource(score, Methods.LABEL_WINKLER);
            }
        }
        return new ScoreWithSource(0);
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
