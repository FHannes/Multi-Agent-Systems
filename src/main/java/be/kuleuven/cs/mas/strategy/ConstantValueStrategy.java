package be.kuleuven.cs.mas.strategy;

public class ConstantValueStrategy extends FieldStrategy {

    private double strength;

    public ConstantValueStrategy(double strength) {
        this.strength = strength;
    }

    @Override
    public double calculateFieldStrength(long elapsed) {
        return strength;
    }

}
