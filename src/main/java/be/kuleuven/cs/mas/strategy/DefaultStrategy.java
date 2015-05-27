package be.kuleuven.cs.mas.strategy;

public class DefaultStrategy extends FieldStrategy {

    @Override
    public double calculateFieldStrength(long elapsed) {
        return 1.0D;
    }

}
