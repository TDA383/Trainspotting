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

	public static int simSpeed;
	
	private int id;
	private int speed;
	private TSimInterface tsi;
	private Boolean isAtStation = true;
	private Boolean isInCritical = false;
	
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
				// Critical section 1
				new SensorEvent(id,6,6,INACTIVE),
				new SensorEvent(id,8,5,INACTIVE),
				new SensorEvent(id,10,8,INACTIVE),
				new SensorEvent(id,11,7,INACTIVE),
				// Critical section 2					
				new SensorEvent(id,14,7,INACTIVE),	
				new SensorEvent(id,15,8,INACTIVE),	
				new SensorEvent(id,19,9,INACTIVE),
				// Critical section 3					
				new SensorEvent(id,18,9,INACTIVE),	
				new SensorEvent(id,12,9,INACTIVE),	
				new SensorEvent(id,13,10,INACTIVE),
				// Critical section 4
				new SensorEvent(id,7,9,INACTIVE),	
				new SensorEvent(id,6,10,INACTIVE),	
				new SensorEvent(id,1,9,INACTIVE),
				// Critical section 5
				new SensorEvent(id,1,10,INACTIVE),	
				new SensorEvent(id,6,11,INACTIVE),	
				new SensorEvent(id,4,13,INACTIVE),
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
		run();
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
		if (sensorEqual(e, criticals[0]) || sensorEqual(e, criticals[1]) || 
				sensorEqual(e, criticals[2]) || sensorEqual(e, criticals[3])) {
			i = 0;
		} else if (sensorEqual(e, criticals[4]) ||
				sensorEqual(e, criticals[5]) || sensorEqual(e, criticals[6])) {
			i = 1;
		} else if (sensorEqual(e, criticals[7]) || 
				sensorEqual(e, criticals[8]) || sensorEqual(e, criticals[9])) {
			i = 2;
		} else if (sensorEqual(e, criticals[10]) || 
				sensorEqual(e, criticals[11]) || sensorEqual(e, criticals[12])) {
			i = 3;
		} else if (sensorEqual(e, criticals[13]) || 
				sensorEqual(e, criticals[14]) || sensorEqual(e, criticals[15])) {
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
			if (sensorEqual(e,stations[i])) return i;
		}
		return -1;	// Returns -1 if 'e' isn't in 'stations'.
	}
	
	/** Retrieves the current active sensor. If none is active, then null is
	 *  returned.
	 * 
	 *  @return A SensorEvent.
	 */
	public SensorEvent getSensor() {
		SensorEvent s = null;
		try {
			s = tsi.getSensor(id);
		} catch (CommandException | InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return s;
	}
	
	/** Checks whether an instance of a SensorEvent is contained in 'criticals'.
	 * 
	 *  @param SensorEvent e, reference to a SensorEvent.
	 *  @return true if 'criticals' contains instance 'e', false otherwise.
	 */
	public Boolean isStation(SensorEvent e) {
		return isSensor(e, stations); 
	}
	
	/** Checks whether an instance of a SensorEvent is contained in 'stations'.
	 * 
	 *  @param SensorEvent e, reference to a SensorEvent.
	 *  @return true if 'stations' contains instance 'e', false otherwise.
	 */
	public Boolean isCritical(SensorEvent e) {
		return isSensor(e, criticals); 
	}
	
	/** Checks whether an instance of a SensorEvent is contained in a list 
	 *  of SensorEvents.
	 * 
	 *  @param SensorEvent e, reference to a SensorEvent.
	 *  @param SensorEvent[] sensors, a list of sensors.
	 *  @return true if 'sensors' contains instance 'e', false otherwise.
	 */
	private Boolean isSensor(SensorEvent e, SensorEvent[] sensors) {
		for (SensorEvent sensor : sensors) {
			if (sensorEqual(e, sensor)) return true;
		}
		return false;
	}
	
	private Boolean sensorEqual(SensorEvent e1, SensorEvent e2) {
		return e1.getXpos() == e2.getXpos() && e1.getYpos() == e2.getYpos();
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
	
	public void checkEnvironment() {
		SensorEvent sensor = getSensor();
		if (sensor != null) {
			if (isCritical(sensor)) {
				if (!isInCritical) {
					System.err.println("Train " + id +" entering cs");
					request(getSectionNumber(sensor));
					isInCritical = true;
				} else {
					if (getSensor().getStatus() == INACTIVE) {
						System.err.println("Train " + id +" exiting cs");
						signal(getSectionNumber(sensor));
						isInCritical = false;
					}
				}
			} else if (isStation(sensor)) {
				if (!isAtStation) {
					System.err.println("Train " + id +" entering station");
					isAtStation = true;
					brake();
					try {
						sleep(2000 + 2 * simSpeed * Math.abs(speed));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					reverse();
					run();
				} else {
					if (getSensor().getStatus() == INACTIVE) {
						System.err.println("Train " + id +" exiting station");
						isAtStation = false;
					}
				}
			}
		}
	}
}