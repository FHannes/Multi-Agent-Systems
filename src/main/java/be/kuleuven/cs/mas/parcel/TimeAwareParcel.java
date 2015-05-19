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

public class TimeAwareParcel extends Parcel implements FieldEmitter, TickListener {

	private FieldStrategy fieldStrategy;

	private long waitingSince;
	private long currentTime;
	private Optional<RoadModel> roadModel;
	private Optional<PDPModel> pdpModel;
	private boolean delivered = false;
	private Optional<Point> position;
	private GradientModel gradientModel;
	
	TimeAwareParcel(FieldStrategy fieldStrategy, Point startPosition, Point pDestination,
			double pMagnitude, long currentTime) {
		super(pDestination, 0, TimeWindow.ALWAYS, 0, TimeWindow.ALWAYS,
				pMagnitude);
		this.fieldStrategy = fieldStrategy;
		setPosition(startPosition);
		this.waitingSince = currentTime;
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
	
	private boolean isDelivered() {
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
		// TODO write relevant variables to experiment result 
	}
	
	public long getWaitingSince() {
		return this.waitingSince;
	}

	/**
	 * Calculates and returns the time elapsed since the parcel was created.
	 *
	 * @return The elapsed time in milliseconds.
	 */
	public long getElapsedTime() {
		return currentTime - getWaitingSince();
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
	public void notifyPickup() {
		position = Optional.absent();
		gradientModel.unregister(this);
	}

	@Override
	public void tick(TimeLapse timeLapse) {
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		currentTime = timeLapse.getEndTime();
	}

}
