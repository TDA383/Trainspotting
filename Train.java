import java.util.concurrent.*;
import TSim.*;
import static TSim.SensorEvent.*;

/** This class is intended to create threads for trains, which can travel
 *  along the railroad independent of each other. The Train class communicates
 *  with the simulator through the TSimInterface. Each train must be assigned
 *  with an id that matches with an id from one of the trains in the railroad
 *  map.
 * 
 *  @author Dennis Bennhage & Hampus Lidin
 */
public class Train extends Thread {

	private int id;
	private int speed;
	private TSimInterface tsi;
	
	private static final Semaphore[] sems = new Semaphore[]{
		new Semaphore(1),
		new Semaphore(1),
		new Semaphore(1),
		new Semaphore(1),
		new Semaphore(1),
	};
	
	/** A list of station sensors.
	 */
	public final SensorEvent[] stations = 
			new SensorEvent[]{
			new SensorEvent(id,12,3,INACTIVE),
			new SensorEvent(id,12,5,INACTIVE),
			new SensorEvent(id,12,11,INACTIVE),
			new SensorEvent(id,12,13,INACTIVE),
	};
	
	/** A list of critical section sensors.
	 */
	public final SensorEvent[] criticals =
			new SensorEvent[]{
				new SensorEvent(id,6,6,INACTIVE),
				new SensorEvent(id,11,8,INACTIVE),
				new SensorEvent(id,14,7,INACTIVE),
				new SensorEvent(id,12,9,INACTIVE),
				new SensorEvent(id,7,10,INACTIVE)
	};
	
	/** Creates a new instance of a train.
	 * 
	 *  @param int id, the train id. This can be found by looking at the railroad map.
	 *  @param int speed, the speed in which the train will be traveling in.
	 */
	public Train(int id, int speed) {
		tsi = TSimInterface.getInstance();
		this.id = id;
		this.speed = speed;
	}
	
	/** Starts the train with its specified speed.
	 */
	public void run() {
		try {
			tsi.setSpeed(id, speed);
		} catch (CommandException e) {
			e.printStackTrace();
		}
	}
	
	/** Stops the train.
	 */
	public void brake() {
		try {
			tsi.setSpeed(id, 0);
		} catch (CommandException e) {
			e.printStackTrace();
		}
	}
	
	/** Reverses the direction of the train.
	 */
	public void reverse() {
		speed = -speed;
	}
	
	/** Simply retrieves this train instance's current speed.
	 * 
	 *  @return The current speed of the train.
	 */
	public int getSpeed() {
		return speed;
	}
	
	/** Returns the critical section number.
	 * 
	 *  @param SensorEvent e, the sensor associated with the station.
	 *  @return The critical section number if 'e' is in 'criticals', otherwise -1.
	 */
	public int getSectionNumber(SensorEvent e) {
		int i = -1;
		if (criticals[0] == e) {
			i = 0;
		} else if (criticals[1] == e) {
			i = 1;
		} else if (criticals[2] == e) {
			i = 2;
		} else if (criticals[3] == e) {
			i = 3;
		} else if (criticals[4] == e) {
			i = 4;
		}
		return i;
	}
	
	/** Returns the station number.
	 * 
	 *  @param SensorEvent e, the sensor associated with the station.
	 *  @return The station number if 'e' is in 'stations', otherwise -1.
	 */
	public int getStationNumber(SensorEvent e) {
		// The number of stations is 4.
		for (int i = 0; i < 4; i++) {
			if (e == stations[i]) return i;
		}
		return -1;	// Returns -1 if 'e' isn't in 'stations'.
	}
	
	/** Checks whether an instance of an SensorEvent is contained in 'criticals'.
	 * 
	 *  @param SensorEvent e, reference to an SensorEvent.
	 *  @return true if 'criticals' contains instance 'e', false otherwise.
	 */
	public Boolean isStation(SensorEvent e) {
		return isSensor(e, stations);
	}
	
	/** Checks whether an instance of an SensorEvent is contained in 'stations'.
	 * 
	 *  @param SensorEvent e, reference to an SensorEvent.
	 *  @return true if 'stations' contains instance 'e', false otherwise.
	 */
	public Boolean isCritical(SensorEvent e) {
		return isSensor(e, criticals);
	}
	
	/** Checks whether an instance of an SensorEvent is contained in a list 
	 *  of SensorEvents.
	 * 
	 *  @param SensorEvent e, reference to an SensorEvent.
	 *  @param SensorEvent[] sensors, a list of sensors.
	 *  @return true if 'sensors' contains instance 'e', false otherwise.
	 */
	private Boolean isSensor(SensorEvent e, SensorEvent[] sensors) {
		for (SensorEvent sensor : sensors) {
			if (e == sensor) return true;
		}
		return false;
	}
	
	/** Requests to pass through a critical section by asking the semaphore of
	 *  that section for a permit.
	 *  
	 *  @param int sectionNumber, the number associated with the critical
	 *  	   section.
	 */
	synchronized public void request(int sectionNumber) {
		if (sems[sectionNumber].availablePermits() == 0) {
			try {
				tsi.setSpeed(id, 0);
			} catch (CommandException e) {
				e.printStackTrace();
			}
		}
		
		try {
			sems[sectionNumber].acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/** Releases the permit for the train from the semaphore of a particular
	 *  critical section
	 *  
	 *  @param int sectionNumber, the number associated with the critical
	 *  	   section.
	 */
	synchronized public void signal(int sectionNumber) {
		try {
			tsi.setSpeed(id, speed);
		} catch (CommandException e) {
			e.printStackTrace();
		}
		
		sems[sectionNumber].release();
	}

	/** Retrieves the current active sensor. If none is active, then null is
	 *  returned.
	 * 
	 *  @return A SensorEvent.
	 */
	synchronized public SensorEvent getActiveSensor() {		
		for (SensorEvent s : criticals) {
			if (s.getStatus() == ACTIVE) return s;
		}
		for (SensorEvent s : stations) {
			if (s.getStatus() == ACTIVE) return s;
		}
		return null;
	}
}