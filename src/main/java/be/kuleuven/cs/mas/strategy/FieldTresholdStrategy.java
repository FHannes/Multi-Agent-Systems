package be.kuleuven.cs.mas.strategy;

public class FieldTresholdStrategy extends FieldStrategy {

    private long treshold;
    private double baseStrength;
    private double priorityStrength;

    public FieldTresholdStrategy(long treshold, double baseStrength, double priorityStrength) {
        this.treshold = treshold;
        this.baseStrength = baseStrength;
        this.priorityStrength = priorityStrength;
    }

    @Override
    public double calculateFieldStrength(long elapsed) {
        return elapsed >= treshold ? priorityStrength : baseStrength;
    }

}
