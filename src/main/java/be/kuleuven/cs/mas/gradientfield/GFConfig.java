package be.kuleuven.cs.mas.gradientfield;

import be.kuleuven.cs.mas.agent.AgentFactory;
import be.kuleuven.cs.mas.parcel.ParcelFactory;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.experiment.DefaultMASConfiguration;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem;
import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class GFConfig extends DefaultMASConfiguration {

    private AgentFactory agentFactory;
    private ParcelFactory parcelFactory;

    public GFConfig(AgentFactory agentFactory, ParcelFactory parcelFactory) {
        this.agentFactory = agentFactory;
        this.parcelFactory = parcelFactory;
    }

    @Override
    public ImmutableList<? extends StochasticSupplier<? extends Model<?>>> getModels() {
        return ImmutableList.of(GradientModel.supplier());
    }

    @Override
    public DynamicPDPTWProblem.Creator<AddVehicleEvent> getVehicleCreator() {
        return (sim, event) -> sim.register(agentFactory.makeAgent(sim));
    }

    @Override
    public Optional<? extends DynamicPDPTWProblem.Creator<AddParcelEvent>> getParcelCreator() {
        return Optional.of((sim, event) -> sim.register(parcelFactory.makeParcel(sim, sim.getCurrentTime())));
    }

}
