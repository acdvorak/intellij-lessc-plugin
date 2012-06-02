package net.andydvorak.intellij.lessc;

class IndicatorState {

    private String text;
    private double fraction;

    IndicatorState(String text, double fraction) {
        this.text = text;
        this.fraction = fraction;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getFraction() {
        return fraction;
    }

    public void setFraction(double fraction) {
        this.fraction = fraction;
    }
}
