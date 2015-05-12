package be.kuleuven.cs.mas;

import be.kuleuven.cs.mas.gradientfield.FieldEmitter;
import be.kuleuven.cs.mas.gradientfield.GradientModel;
import org.apache.commons.math3.random.RandomGenerator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import javax.annotation.Nullable;
import java.util.LinkedList;

class AGVAgent implements TickListener, MovingRoadUser, FieldEmitter {

    @Nullable
    private GradientModel gradientModel;

    private final RandomGenerator rng;
    private Optional<CollisionGraphRoadModel> roadModel;
    private Optional<Point> nextPointDestination;
    private LinkedList<Point> path;

    AGVAgent(RandomGenerator r) {
        rng = r;
        roadModel = Optional.absent();
        nextPointDestination = Optional.absent();
        path = new LinkedList<>();
    }

    @Override
    public void initRoadUser(RoadModel model) {
        roadModel = Optional.of((CollisionGraphRoadModel) model);
        Point p;
        do {
            p = model.getRandomPosition(rng);
        } while (roadModel.get().isOccupied(p));
        roadModel.get().addObjectAt(this, p);
    }

    @Override
    public double getSpeed() {
        return 1;
    }

    void nextDestination() {
        nextPointDestination = Optional.of(gradientModel.getGradientTarget(this));
        path = new LinkedList<>(roadModel.get().getShortestPathTo(this, nextPointDestination.get()));
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (!nextPointDestination.isPresent()) {
            nextDestination();
        }

        roadModel.get().followPath(this, path, timeLapse);

        if (roadModel.get().getPosition(this).equals(nextPointDestination.get())) {
            nextDestination();
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    @Override
    public void setModel(GradientModel model) {
        this.gradientModel = model;
    }

    @Override
    public double getStrength() {
        return 0;
    }

    @Override
    public Point getPosition() {
        return null;
    }

}
