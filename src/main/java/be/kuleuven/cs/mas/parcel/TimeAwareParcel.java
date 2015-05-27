package be.kuleuven.cs.mas.parcel;

import be.kuleuven.cs.mas.agent.AGVAgent;
import be.kuleuven.cs.mas.gradientfield.FieldEmitter;
import be.kuleuven.cs.mas.gradientfield.GradientModel;
import be.kuleuven.cs.mas.strategy.FieldStrategy;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;

import java.util.HashSet;
import java.util.Set;

public class TimeAwareParcel extends Parcel implements FieldEmitter, TickListener {

	private FieldStrategy fieldStrategy;

	private long scheduleTime;
	private long currentTime;
	private long pickupTime;
	private long deliveryTime;
	private Optional<RoadModel> roadModel;
	private Optional<PDPModel> pdpModel;
	private boolean delivered = false;
	private Optional<Point> position;
	private GradientModel gradientModel;

	private Set<ParcelObserver> observers = new HashSet<>();
	
	TimeAwareParcel(FieldStrategy fieldStrategy, Point startPosition, Point pDestination,
			double pMagnitude, long currentTime) {
		super(pDestination, 0, TimeWindow.ALWAYS, 0, TimeWindow.ALWAYS,
				pMagnitude);
		this.fieldStrategy = fieldStrategy;
		setPosition(startPosition);
		this.scheduleTime = currentTime;
		this.currentTime = currentTime;
	}

	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		this.roadModel = Optional.of(pRoadModel);
		this.pdpModel = Optional.of(pPdpModel);
	}
	
	private Optional<RoadModel> getOwnRoadModel() {
		return this.roadModel;
	}
	
	private Optional<PDPModel> getOwnPDPModel() {
		return this.pdpModel;
	}
	
	public boolean isDelivered() {
		return this.delivered;
	}
	
	private void setDelivered(boolean delivered) {
		this.delivered = delivered;
	}
	
	public void notifyDelivered(TimeLapse time) throws IllegalStateException {
		if (this.isDelivered()) {
			throw new IllegalStateException("parcel was already delivered");
		}
		
		this.getOwnPDPModel().get().unregister(this);
		this.setDelivered(true);
		deliveryTime = time.getTime();
		observers.forEach(o -> o.parcelDelivered(this));
		// TODO write relevant variables to experiment result 
	}
	
	public long getScheduleTime() {
		return this.scheduleTime;
	}

	/**
	 * Calculates and returns the time elapsed since the parcel was created.
	 *
	 * @return The elapsed time in milliseconds.
	 */
	public long getElapsedTime() {
		return currentTime - getScheduleTime();
	}

	public long getPickupTime() {
		return pickupTime;
	}

	public long getDeliveryTime() {
		return deliveryTime;
	}

	protected void setPosition(Point position) {
		super.setStartPosition(position);
		this.position = Optional.of(position);
	}

	@Override
	public void setGradientModel(GradientModel model) {
		this.gradientModel = model;
	}

	@Override
	public double getStrength() {
		if (getPosition().isPresent()) {
			return -fieldStrategy.calculateFieldStrength(getElapsedTime());
		} else {
			return 0D;
		}
	}

	@Override
	public Optional<Point> getPosition() {
		return position;
	}

	@Override
	public Optional<Point> getLastPosition() {
		return getPosition();
	}

	/**
	 * To be called when the parcel is picked up by an {@link AGVAgent}.
	 */
	public void notifyPickup(TimeLapse timeLapse) {
		position = Optional.absent();
		gradientModel.unregister(this);
		pickupTime = timeLapse.getTime();
	}

	@Override
	public void tick(TimeLapse timeLapse) {
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		currentTime = timeLapse.getEndTime();
	}

	public void registerObserver(ParcelObserver observer) {
		if (observer != null) {
			observers.add(observer);
		}
	}

	public void unregisterObserver(ParcelObserver observer) {
		if (observer != null) {
			observers.remove(observer);
		}
	}

}
