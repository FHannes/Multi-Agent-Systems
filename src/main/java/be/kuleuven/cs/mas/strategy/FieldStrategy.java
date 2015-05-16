package be.kuleuven.cs.mas.strategy;

/**
 * A strategy (algorithm provider) for calculating field strengths in the gradient field.
 */
public abstract class FieldStrategy {

    public abstract double calculateFieldStrength(long elapsed);

}
