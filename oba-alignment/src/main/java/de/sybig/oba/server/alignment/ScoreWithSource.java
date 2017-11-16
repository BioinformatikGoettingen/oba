package de.sybig.oba.server.alignment;

/**
 *
 * @author jdo
 */
public class ScoreWithSource {

    private double score;
    private Methods source;

    public ScoreWithSource(double score) {
        this.score = score;
    }

    public ScoreWithSource(double score, Methods source) {
        this.score = score;
        this.source = source;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Methods getSource() {
        return source;
    }

    public void setSource(Methods source) {
        this.source = source;
    }

}
