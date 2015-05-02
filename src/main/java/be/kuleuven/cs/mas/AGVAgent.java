/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.kuleuven.cs.mas;

import be.kuleuven.cs.mas.gradientfield.FieldEmitter;
import be.kuleuven.cs.mas.gradientfield.GradientModel;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;

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
