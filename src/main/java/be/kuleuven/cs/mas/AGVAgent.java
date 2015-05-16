package be.kuleuven.cs.mas;

import be.kuleuven.cs.mas.architecture.AgentState;
import be.kuleuven.cs.mas.architecture.FollowGradientFieldState;
import be.kuleuven.cs.mas.gradientfield.FieldEmitter;
import be.kuleuven.cs.mas.gradientfield.GradientModel;
import be.kuleuven.cs.mas.message.AgentMessage;
import be.kuleuven.cs.mas.message.AgentMessageBuilder;
import be.kuleuven.cs.mas.modifiedclasses.CollisionGraphRoadModel;
import be.kuleuven.cs.mas.parcel.TimeAwareParcel;
import be.kuleuven.cs.mas.vision.VisualSensor;
import be.kuleuven.cs.mas.vision.VisualSensorOwner;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;

import javax.annotation.Nullable;
import java.util.*;

public class AGVAgent extends Vehicle implements MovingRoadUser, FieldEmitter, CommUser, VisualSensorOwner {

    @Nullable
    private GradientModel gradientModel;

    private final RandomGenerator rng;
    private Optional<CollisionGraphRoadModel> roadModel;
    private Optional<PDPModel> pdpModel;
    private Optional<Point> nextPointDestination;
    private Queue<Point> path;
    private Optional<CommDevice> commDevice;
    private Optional<TimeAwareParcel> parcel;
    private AgentState state;
    private VisualSensor sensor;
    private AgentMessageBuilder msgBuilder;
    private String name;
    private Point mostRecentPosition;

    // TODO: move determination of unoccupied position to start out on to simulation initialisation
    AGVAgent(RandomGenerator r, int visualRange, Point startPosition, String name) {
        rng = r;
        roadModel = Optional.absent();
        pdpModel = Optional.absent();
        nextPointDestination = Optional.absent();
        path = new LinkedList<>();
        commDevice = Optional.absent();
        parcel = Optional.absent();
        state = new FollowGradientFieldState(this);
        sensor = new VisualSensor(this, visualRange);
        this.setStartPosition(startPosition);
        this.mostRecentPosition = startPosition;
        this.name = name;
    }
    
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {
    	this.roadModel = Optional.of((CollisionGraphRoadModel) roadModel);
    	this.pdpModel = Optional.of(pdpModel);
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
    public void tickImpl(TimeLapse timeLapse) {
    	// TODO move all agent behaviour to states
    	if (this.getRoadModel().getGraph().containsNode(this.getPosition().get())) {
    		this.setMostRecentPosition(this.getPosition().get());
    	}
    	
    	for (Message msg : this.getCommDevice().getUnreadMessages()) {
    		this.getAgentState().processMessage((AgentMessage) msg.getContents());
    	}
    	
    	this.getAgentState().act(timeLapse);
    	
    	// TODO expanding on the above, that means moving all this
        if (!nextPointDestination.isPresent()) {
            nextDestination();
        }

        roadModel.get().followPath(this, path, timeLapse);

        if (roadModel.get().getPosition(this).equals(nextPointDestination.get())) {
            nextDestination();
        }
    }

    public void followPath(TimeLapse timeLapse) {
    	this.getRoadModel().followPath(this, this.getPath(), timeLapse);
    }
    
    public void followPath(Point point, TimeLapse timeLapse) {
    	this.getRoadModel().followPath(this, new LinkedList<>(Arrays.asList(point)), timeLapse);
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
    public Optional<Point> getPosition() {
        if (! this.roadModel.isPresent()) {
        	return Optional.absent();
        }
        return Optional.of(this.roadModel.get().getPosition(this));
    }
    
    public Point getMostRecentPosition() {
    	return this.mostRecentPosition;
    }
    
    private void setMostRecentPosition(Point point) {
    	this.mostRecentPosition = point;
    }
    
    public void sendMessage(AgentMessage msg) throws IllegalStateException {
    	if (! this.commDevice.isPresent()) {
    		throw new IllegalStateException("comm device not set");
    	}
    	this.commDevice.get().broadcast(msg);
    }

    public CommDevice getCommDevice() {
    	return this.commDevice.get();
    }
    
	@Override
	public void setCommDevice(CommDeviceBuilder builder) {
		this.commDevice = Optional.of(builder.build());
	}
	
	public Optional<TimeAwareParcel> getParcel() {
		return this.parcel;
	}
	
	public void setParcel(TimeAwareParcel parcel) {
		assert(! this.getParcel().isPresent());
		this.parcel = Optional.of(parcel);
		this.replanRoute();
	}
	
	public void unsetParcel() {
		assert(this.getParcel().isPresent());
		this.parcel = Optional.absent();
	}
	
	public AgentState getAgentState() {
		return this.state;
	}
	
	public void setAgentState(AgentState state) {
		this.state = state;
		state.uponSet();
	}

	@Override
	public CollisionGraphRoadModel getRoadModel() {
		return this.roadModel.get();
	}
	
	public PDPModel getPDPModel() {
		return this.pdpModel.get();
	}
	
	private VisualSensor getVisualSensor() {
		return this.sensor;
	}
	
	public boolean occupiedPointsOnPathWithinRange() {
		Set<Point> occupiedPoints = this.getOccupiedPointsInVisualRange();
		
		// the following only works under the assumption that a queue's iterator returns its elements in the logical order,
		// which seems to be satisfied if the queue is a linked list
		Iterator<Point> pointIt = this.getPath().iterator();
		Point pathPoint;
		int i = 0;
		while (pointIt.hasNext() && i < this.getVisualSensor().getVisualRange()) {
			pathPoint = pointIt.next();
			i++;
			if (occupiedPoints.contains(pathPoint)) {
				return true;
			}
			if (this.getParcel().get().getDestination().equals(pathPoint)) {
				// the path to this agent's destination is currently clear of obstacles, so return false
				return false;
			}
		}
		return false;
	}
	
	public AgentMessageBuilder getMessageBuilder() {
		return this.msgBuilder;
	}
	
	public Set<Point> getOccupiedPointsInVisualRange() {
		return this.getVisualSensor().getOccupiedPointsWithinVisualRange();
	}
	
	public Optional<Point> getNextPointOnPath() {
		Point toReturn = this.path.peek();
		if (toReturn == null) {
			return Optional.absent();
		}
		return Optional.of(toReturn);
	}
	
	private Queue<Point> getPath() {
		return this.path;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void replanRoute() {
		if (! this.getParcel().isPresent()) {
			return;
		}
		this.path = new LinkedList<>(this.getRoadModel().getShortestPathTo(this, this.getParcel().get()));
	}
	
	public Optional<Point> getRandomReachablePoint(Set<Point> excludeSet) {
		Set<Point> occupiedPoints = this.getVisualSensor().getOccupiedPointsWithinVisualRange();
		Collection<Point> neighbours = this.getRoadModel().getGraph().getOutgoingConnections(this.getPosition().get());
		neighbours.removeAll(occupiedPoints);
		neighbours.removeAll(excludeSet);
		
		if (neighbours.isEmpty()) {
			return Optional.absent();
		} else {
			ArrayList<Point> asList = new ArrayList<>(neighbours);
			return Optional.of(asList.get(this.rng.nextInt(asList.size())));
		}
	}
	
	public Optional<Point> getRandomNeighbourPoint(Set<Point> excludeSet) {
		Collection<Point> neighbours = this.getRoadModel().getGraph().getOutgoingConnections(this.getPosition().get());
		neighbours.removeAll(excludeSet);
		
		if (neighbours.isEmpty()) {
			return Optional.absent();
		} else {
			ArrayList<Point> asList = new ArrayList<>(neighbours);
			return Optional.of(asList.get(this.rng.nextInt(asList.size())));
		}
	}
}
