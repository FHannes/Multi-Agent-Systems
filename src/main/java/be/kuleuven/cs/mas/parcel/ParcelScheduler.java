package be.kuleuven.cs.mas.parcel;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import org.apache.commons.math3.random.RandomGenerator;

public abstract class ParcelScheduler implements TickListener, RandomUser {

    /**
     * Minimum amount of time before spawning a parcel.
     */
    private static final int TIME_MIN = 15000;
    /**
     * Maximum amount of time before spawning a parcel.
     */
    private static final int TIME_MAX = 45000;

    private RandomGenerator rng;
    private ParcelFactory parcelFactory;

    private Long next = null;

    public void scheduleNext(long currentTime) {
        next = currentTime + TIME_MIN + rng.nextInt(TIME_MAX - TIME_MIN);
    }

    public ParcelScheduler(ParcelFactory parcelFactory) {
        this.parcelFactory = parcelFactory;
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (next == null) {
            scheduleNext(timeLapse.getEndTime());
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
        if (next <= timeLapse.getEndTime()) {
            generateParcel(parcelFactory.makeParcel(next));
            next = null;
        }
    }

    @Override
    public void setRandomGenerator(RandomProvider provider) {
        rng = provider.sharedInstance(ParcelScheduler.class);
    }

    public abstract void generateParcel(TimeAwareParcel parcel);

}
