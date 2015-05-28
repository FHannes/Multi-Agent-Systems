package be.kuleuven.cs.mas.strategy;

class FieldTimeStrategy extends FieldStrategy {

    private long timeUnit;
    private double baseStrength;

    /**
     * Constructor for this strategy.
     *
     * @param timeUnit
     *        The divider for the elapsed time. The elapsed time will be divided by this value to determine the field
     *        strength.
     */
    public FieldTimeStrategy(long timeUnit, double baseStrength) {
        this.timeUnit = timeUnit;
        this.baseStrength = baseStrength;
    }

    @Override
    public double calculateFieldStrength(long elapsed) {
        return baseStrength + elapsed / (double) timeUnit;
    }

    @Override
    public String toString() {
        return String.format("time;%d;%.2f", timeUnit, baseStrength);
    }

}
