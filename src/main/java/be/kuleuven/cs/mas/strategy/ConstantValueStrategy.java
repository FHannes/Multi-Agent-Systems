package be.kuleuven.cs.mas.strategy;

class ConstantValueStrategy extends FieldStrategy {

    private double strength;

    public ConstantValueStrategy(double strength) {
        this.strength = strength;
    }

    @Override
    public double calculateFieldStrength(long elapsed) {
        return strength;
    }

    @Override
    public String toString() {
        return String.format("constant;%.2f", strength);
    }

}
