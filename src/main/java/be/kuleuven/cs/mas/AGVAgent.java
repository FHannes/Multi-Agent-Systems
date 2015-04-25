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

import java.util.*;

import be.kuleuven.cs.mas.gradientfield.EmitterListener;
import be.kuleuven.cs.mas.gradientfield.FieldEmitter;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

class AGVAgent implements TickListener, MovingRoadUser, FieldEmitter {
    private final RandomGenerator rng;
    private Optional<CollisionGraphRoadModel> roadModel;
    private Optional<Point> destination;
    private LinkedList<Point> path;
    private Set<EmitterListener> listeners = new HashSet<>();

    AGVAgent(RandomGenerator r) {
        rng = r;
        roadModel = Optional.absent();
        destination = Optional.absent();
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
        destination = Optional.of(roadModel.get().getRandomPosition(rng));
        path = new LinkedList<>(roadModel.get().getShortestPathTo(this,
                destination.get()));
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (!destination.isPresent()) {
            nextDestination();
        }

        roadModel.get().followPath(this, path, timeLapse);

        if (roadModel.get().getPosition(this).equals(destination.get())) {
            nextDestination();
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    @Override
    public double getStrength() {
        return 0;
    }

    @Override
    public Point getPosition() {
        return null;
    }

    @Override
    public void addListener(EmitterListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(EmitterListener listener) {
        listeners.remove(listener);
    }

}
