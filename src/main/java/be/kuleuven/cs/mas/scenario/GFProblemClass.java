package be.kuleuven.cs.mas.scenario;

import com.github.rinde.rinsim.scenario.Scenario;

public enum GFProblemClass implements Scenario.ProblemClass {

    DEFAULT;

    @Override
    public String getId() {
        return this.name();
    }

}
