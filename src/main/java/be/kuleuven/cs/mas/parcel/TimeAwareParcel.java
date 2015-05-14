package be.kuleuven.cs.mas.parcel;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;

public class TimeAwareParcel extends Parcel {

	private long waitingSince;
	private Optional<RoadModel> roadModel;
	private Optional<PDPModel> pdpModel;
	private boolean delivered = false;
	
	public TimeAwareParcel(Point startPosition, Point pDestination,
			double pMagnitude, long currentTime) {
		super(pDestination, 0, TimeWindow.ALWAYS, 0, TimeWindow.ALWAYS,
				pMagnitude);
		setStartPosition(startPosition);
		this.waitingSince = currentTime;
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
		this.getOwnRoadModel().get().removeObject(this);
		this.setDelivered(true);
		// TODO write relevant variables to experiment result 
	}
	
	public long getWaitingSince() {
		return this.waitingSince;
	}
}
