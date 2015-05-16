package be.kuleuven.cs.mas.strategy;

public class FieldTimeStrategy extends FieldStrategy {

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
        return elapsed / (double) timeUnit;
    }

}
