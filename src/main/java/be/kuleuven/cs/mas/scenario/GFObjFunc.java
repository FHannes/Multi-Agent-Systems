package be.kuleuven.cs.mas.scenario;

import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;

import java.io.Serializable;

public class GFObjFunc implements ObjectiveFunction, Serializable {

    @Override
    public boolean isValidResult(StatisticsDTO stats) {
        return stats.totalParcels == stats.acceptedParcels
                && stats.totalParcels == stats.totalPickups
                && stats.totalParcels == stats.totalDeliveries
                && stats.simFinish;
    }

    @Override
    public double computeCost(StatisticsDTO stats) {
        return 0;
    }

    @Override
    public String printHumanReadableFormat(StatisticsDTO stats) {
        return null;
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }

}
