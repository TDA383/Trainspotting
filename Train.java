import java.awt.*;
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
	
	private static final Semaphore[] critSems = new Semaphore[]{
		new Semaphore(1),
		new Semaphore(1),
		new Semaphore(1)
//		new Semaphore(1),
//		new Semaphore(1),
	};
	
	private static final Semaphore[] statSems = new Semaphore[]{
		new Semaphore(1),
		new Semaphore(1)
//		new Semaphore(1),
//		new Semaphore(1),
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
/*				new SensorEvent(id,19,9,INACTIVE),				
				new SensorEvent(id,18,9,INACTIVE),	 */
				new SensorEvent(id,12,9,INACTIVE),	
				new SensorEvent(id,13,10,INACTIVE),
				// Critical section 3
				new SensorEvent(id,7,9,INACTIVE),	
				new SensorEvent(id,6,10,INACTIVE),	
//				new SensorEvent(id,1,9,INACTIVE),
//				new SensorEvent(id,1,10,INACTIVE),	
				new SensorEvent(id,6,11,INACTIVE),	
				new SensorEvent(id,4,13,INACTIVE),
	};
	
	/** A list of coordinates of the switches.
	 */
	Dimension[] switches = new Dimension[]{
			new Dimension(0, 0),
			new Dimension(0, 0),
			new Dimension(0, 0)
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
		super.run();
		try {
			tsi.setSpeed(id, speed);
		} catch (CommandException e) {
			e.printStackTrace();
		}
		while (true) {
			checkEnvironment();
		}
	}
	
	/** Returns the critical section index.
	 * 
	 *  @param SensorEvent e, the sensor associated with the station.
	 *  @return The critical section index if 'e' is in 'criticals', otherwise -1.
	 */
	public int getSectionIndex(SensorEvent e) {
		int i = -1;
		if (sensorEqual(e, criticals[0]) || sensorEqual(e, criticals[1]) || 
				sensorEqual(e, criticals[2]) || sensorEqual(e, criticals[3])) {
			i = 0;
		} else if (sensorEqual(e, criticals[4]) ||
				sensorEqual(e, criticals[5]) || sensorEqual(e, criticals[6]) ||
				sensorEqual(e, criticals[7])) {
			i = 1;
		} else if (sensorEqual(e, criticals[8]) ||
				sensorEqual(e, criticals[9]) || sensorEqual(e, criticals[10]) ||
				sensorEqual(e, criticals[11])) {
			i = 2;
		}
		return i;
	}
	
	/** Returns the station index.
	 * 
	 *  @param SensorEvent e, the sensor associated with the station.
	 *  @return The station index if 'e' is in 'stations', otherwise -1.
	 */
	public int getStationIndex(SensorEvent e) {
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
	 *  @param e reference to a SensorEvent.
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
	
	/**	Manages the switches based on the triggered sensor and the semaphores
	 *  handling the station entering and exiting.
	 *  
	 * 	@param e the sensor that was triggered
	 */
	private void doSwitch(SensorEvent e) {
		// Leaving upper station 1 and entering critical station 2.
		if (isStationEmpty(0) && sensorEqual(e, criticals[4])) {
		
		}
		// Leaving lower station 1 and entering critical station 2 .
		else if (isStationEmpty(0) && sensorEqual(e, criticals[5])) {
			
		}
		// Leaving upper station 2 and entering critical station 3.
		else if (isStationEmpty(1) && sensorEqual(e, criticals[10])) {
		
		}
		// Leaving lower station 2 and entering critical station 3.
		else if (isStationEmpty(1) && sensorEqual(e, criticals[11])) {
		
		}
		// Entering critical section 2 and heading towards station.
		else if (sensorEqual(e, criticals[6]) || sensorEqual(e, criticals[7]) || 
				sensorEqual(e, criticals[8]) || sensorEqual(e, criticals[9])) {
			
		}
		// Entering critical section 3 and heading towards station.
		else if (sensorEqual(e, criticals[6]) || sensorEqual(e, criticals[7]) || 
				sensorEqual(e, criticals[8]) || sensorEqual(e, criticals[9])) {
					
		}
		// Entering critical section 1
		else if (sensorEqual(e, criticals[6]) || sensorEqual(e, criticals[7]) || 
				sensorEqual(e, criticals[8]) || sensorEqual(e, criticals[9])) {
			
		}	
	}
	
	/** Checks whether a station is empty or not
	 *	@param stationIndex the index of the station
	 *	@return 'true' if station is empty, 'false' otherwise
	 */
	private Boolean isStationEmpty(int stationIndex) {
		return statSems[stationIndex].availablePermits() == 1;
	}
	
	/** Requests to pass through a critical section by asking the semaphore of
	 *  that section for a permit.
	 *  
	 *  @param int sectionNumber, the number associated with the critical
	 *  	   section.
	 */
	synchronized public void request(int sectionNumber) {
		if (critSems[sectionNumber].availablePermits() == 0) {
			try {
				tsi.setSpeed(id, 0);
			} catch (CommandException e) {
				e.printStackTrace();
			}
		}
		
		try {
			critSems[sectionNumber].acquire();
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
		
		critSems[sectionNumber].release();
	}
	
	public void checkEnvironment() {
		SensorEvent sensor = getSensor();
		if (sensor != null) {
			if (isStation(sensor)) {
				if (!isAtStation) {
					System.err.println("Train " + id +" entering station "
							 + (getStationIndex(sensor) + 1));
					isAtStation = true;
					try {
						tsi.setSpeed(id, 0);
					} catch (CommandException e1) {
						e1.printStackTrace();
					}
					try {
						sleep(2000 + 2 * simSpeed * Math.abs(speed));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					speed = -speed;
					run();
				} else {
					if (getSensor().getStatus() == INACTIVE) {
						System.err.println("Train " + id +" exiting station "
								+ (getStationIndex(sensor) + 1));
						isAtStation = false;
					}
				}
			} else {
				if (!isInCritical) {
					System.err.println("Train " + id +" entering critical "
							+ "section " + (getSectionIndex(sensor) + 1));
					request(getSectionIndex(sensor));
					doSwitch(sensor);
					isInCritical = true;
				} else {
					if (getSensor().getStatus() == INACTIVE) {
						System.err.println("Train " + id +" exiting critical "
								+ "section " + (getSectionIndex(sensor) + 1));
						signal(getSectionIndex(sensor));
						isInCritical = false;
					}
				}
			}
		}
	}
}