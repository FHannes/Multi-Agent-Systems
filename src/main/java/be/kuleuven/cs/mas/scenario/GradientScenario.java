package be.kuleuven.cs.mas.scenario;

import be.kuleuven.cs.mas.GraphUtils;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.generator.Models;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

public class GradientScenario extends Scenario {

    @Override
    public ImmutableList<? extends Supplier<? extends Model<?>>> getModelSuppliers() {
        return ImmutableList.<Supplier<? extends Model<?>>> builder()
                .add(() -> CommModel.builder().build())
                .add(() -> CollisionGraphRoadModel.builder(GraphUtils.createGraph())
                        .setVehicleLength(GraphUtils.VEHICLE_LENGTH)
                        .build())
                .add(Models.pdpModel(TimeWindowPolicy.TimeWindowPolicies.LIBERAL))
                .build();
    }

    @Override
    public TimeWindow getTimeWindow() {
        return TimeWindow.ALWAYS;
    }

    @Override
    public long getTickSize() {
        return 1;
    }

    @Override
    public Predicate<Simulator> getStopCondition() {
        return (context) -> {
            assert context != null;
            final StatisticsDTO stats = DynamicPDPTWProblem.getStats(context);
            return stats.totalVehicles == stats.vehiclesAtDepot
                    && stats.movedVehicles > 0
                    && stats.totalParcels == stats.totalDeliveries;
        };
    }

    @Override
    public Unit<Duration> getTimeUnit() {
        return SI.MILLI(SI.SECOND);
    }

    @Override
    public Unit<Velocity> getSpeedUnit() {
        return NonSI.KILOMETERS_PER_HOUR;
    }

    @Override
    public Unit<Length> getDistanceUnit() {
        return SI.METER;
    }

    @Override
    public ProblemClass getProblemClass() {
        return null;
    }

    @Override
    public String getProblemInstanceId() {
        return null;
    }

}
