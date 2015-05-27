package be.kuleuven.cs.mas.strategy;

class FieldTimeStrategy extends FieldStrategy {

    private long timeUnit;

    /**
     * Constructor for this strategy.
     *
     * @param timeUnit
     *        The divider for the elapsed time. The elapsed time will be divided by this value to determine the field
     *        strength.
     */
    public FieldTimeStrategy(long timeUnit) {
        this.timeUnit = timeUnit;
    }

    @Override
    public double calculateFieldStrength(long elapsed) {
        return 1.0D + elapsed / (double) timeUnit;
    }

    @Override
    public String toString() {
        return String.format("time;%d", timeUnit);
    }

}
