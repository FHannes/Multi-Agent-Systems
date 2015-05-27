package be.kuleuven.cs.mas.strategy;

public class StrategyFactory {

    public FieldStrategy makeConstantStrategy(double strength) {
        return new ConstantValueStrategy(strength);
    }

    public FieldStrategy makeTimeStrategy(long timeUnit) {
        return new FieldTimeStrategy(timeUnit);
    }

    public FieldStrategy makeTresholdStrategy(long treshold, double baseStrength, double priorityStrength) {
        return new FieldTresholdStrategy(treshold, baseStrength, priorityStrength);
    }

    private FieldStrategy parseConstantStrategy(String[] params) {
        if (params.length < 2) {
            return null;
        }
        try {
            double strength = Double.parseDouble(params[1]);
            return makeConstantStrategy(strength);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private FieldStrategy parseTimeStrategy(String[] params) {
        if (params.length < 2) {
            return null;
        }
        try {
            long strength = Long.parseLong(params[1]);
            return makeTimeStrategy(strength);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private FieldStrategy parseTresholdStrategy(String[] params) {
        if (params.length < 4) {
            return null;
        }
        try {
            long treshold = Long.parseLong(params[1]);
            double baseStrength = Double.parseDouble(params[2]);
            double priorityStrength = Double.parseDouble(params[3]);
            return makeTresholdStrategy(treshold, baseStrength, priorityStrength);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public FieldStrategy makeFromString(String args) {
        String[] params = args.split(";");

        if (params.length == 1) {
            return null;
        }

        String name = params[0].toLowerCase();
        if (name.startsWith("const")) {
            return parseConstantStrategy(params);
        } else if (name.startsWith("time")) {
            return parseTimeStrategy(params);
        } else if (name.startsWith("tres")) {
            return parseTresholdStrategy(params);
        }

        return null;
    }

}
