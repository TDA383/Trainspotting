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

	public int simSpeed;
	
	private int id;
	private int speed;
	private TSimInterface tsi;
	private Boolean isAtStation = true;
	private Boolean isInCritical = false;
	
	private static final Semaphore[] critSems = new Semaphore[]{
		new Semaphore(1),	// Cross-section semaphore
		new Semaphore(1),	// Station 1 merge section semaphore
		new Semaphore(1),	// Station 2 merge section semaphore
		new Semaphore(1)	// Two-track section semaphore
	};
	
	private static final Semaphore[] statSems = new Semaphore[]{
		new Semaphore(0),	// Train 1 is initially at upper station 1
		new Semaphore(1),
		new Semaphore(0), 	// Train 2 is initially at upper station 2
		new Semaphore(1)
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
				// Critical section, cross-section
				new SensorEvent(id,6,6,INACTIVE),
				new SensorEvent(id,8,5,INACTIVE),
				new SensorEvent(id,11,7,INACTIVE),
				new SensorEvent(id,10,8,INACTIVE),
				// Critical section, station 1 merge					
				new SensorEvent(id,14,7,INACTIVE),	
				new SensorEvent(id,15,8,INACTIVE),			 
				new SensorEvent(id,12,9,INACTIVE),			 
				new SensorEvent(id,13,10,INACTIVE),	
				// Critical section, station 2 merge
				new SensorEvent(id,7,9,INACTIVE),	
				new SensorEvent(id,6,10,INACTIVE),	
				new SensorEvent(id,6,11,INACTIVE),		
				new SensorEvent(id,4,13,INACTIVE)
	};
	
	/** A list of coordinates of the switches.
	 */
	private final Dimension[] switches = new Dimension[]{
			new Dimension(17, 7),
			new Dimension(15, 9),
			new Dimension(4, 9),
			new Dimension(3, 11)
	};
	
	/** Creates a new instance of a train.
	 * 
	 *  @param int id, the train id. This can be found by looking at the railroad map.
	 *  @param int speed, the speed in which the train will be traveling in.
	 */
	public Train(int id, int speed, int simSpeed) {
		tsi = TSimInterface.getInstance();
		this.id = id;
		this.speed = speed;
		this.simSpeed = simSpeed;
	}
	
	/** Starts the train with its specified speed.
	 */
	public void run() {
		try {
			tsi.setSpeed(id, speed);
		} catch (CommandException e) {
			e.printStackTrace();
		}
		while (true) {
			try {
				checkEnvironment();
			} catch (CommandException | InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
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
	private int getStationIndex(SensorEvent e) {
		// The number of stations is 4.
		for (int i = 0; i < 4; i++) {
			if (sensorEqual(e,stations[i])) return i;
		}
		return -1;	// Returns -1 if 'e' isn't in 'stations'.
	}
	
	/** Checks whether an instance of a SensorEvent is contained in 'stations'.
	 * 
	 *  @param SensorEvent e, reference to a SensorEvent.
	 *  @return true if 'sensors' contains instance 'e', false otherwise.
	 */
	private Boolean isStation(SensorEvent e) {
		for (SensorEvent sensor : stations) {
			if (sensorEqual(e, sensor)) return true;
		}
		return false;
	}
	
	/** Compares two sensors' positions.
	 * 
	 *  @param e1 the first SensorEvent
	 *  @param e2 the second SensorEvent
	 *  @return 'true' if the sensors are at the same position, 'false'
	 *  	otherwise
	 */
	private Boolean sensorEqual(SensorEvent e1, SensorEvent e2) {
		return e1.getXpos() == e2.getXpos() && e1.getYpos() == e2.getYpos();
	}
	
	/** Requests to pass through a critical section by asking the semaphore of
	 *  that section for a permit.
	 *  
	 *  @param int sectionNumber, the number associated with the critical
	 *  	   section.
	 */
	private void request(int sectionNumber) {
		try {
			tsi.setSpeed(id, 0);
		} catch (CommandException e) {
			e.printStackTrace();
		}
		
		try {
			critSems[sectionNumber].acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			tsi.setSpeed(id, speed);
		} catch (CommandException e) {
			e.printStackTrace();
		}
	}
	
	/** Releases the permit for the train from the semaphore of a particular
	 *  critical section
	 *  
	 *  @param int sectionNumber, the number associated with the critical
	 *  	   section.
	 */
	private void signal(int sectionNumber) {
		critSems[sectionNumber].release();
	}
	
	/** Sets the two track section switches based on the vacancy of the track
	 *  and in which direction the train is coming from.
	 * 
	 * 	@param direction 0 if the train comes from the left, 1 otherwise. Other
	 * 		values will throw an IllegalArgumentException.
	 *  @throws CommandException
	 *  @throws InterruptedException
	 *  @throws InterruptedException
	 */
	synchronized private void setTwoTrackSwitches(int direction)
			throws CommandException, InterruptedException,
			IllegalArgumentException {
		int swL = TSimInterface.SWITCH_LEFT;
		int swR = TSimInterface.SWITCH_RIGHT;
		if (!(direction == 0 || direction == 1))
			throw new IllegalArgumentException();
		
		if (critSems[3].availablePermits() == 1) {
			critSems[3].acquire();
			System.err.println("Permit acquired! " + critSems[3].availablePermits() + " left.");
			if (direction == 1)
				tsi.setSwitch(switches[1].width, switches[1].height, swR);
			else tsi.setSwitch(switches[2].width, switches[2].height, swL);
		} else {
			if (direction == 1)
				tsi.setSwitch(switches[1].width, switches[1].height, swL);
			else tsi.setSwitch(switches[2].width, switches[2].height, swR);
		}
	}
	
	private void checkEnvironment()
			throws CommandException, InterruptedException {
		int swL = TSimInterface.SWITCH_LEFT;
		int swR = TSimInterface.SWITCH_RIGHT;
		
		SensorEvent sensor = tsi.getSensor(id);
		if (isStation(sensor)) {
			// Is not at a station.
			if (!isAtStation) {
//				System.err.println("Train " + id +" entering station "
//						 + (getStationIndex(sensor) + 1));
				isAtStation = true;
				try {
					tsi.setSpeed(id, 0);
				} catch (CommandException e) {
					e.printStackTrace();
				}
				try {
					sleep(2000 + 2 * simSpeed * Math.abs(speed));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				speed = -speed;
				tsi.setSpeed(id, speed);
			}
			// Is at a station.
			else {
				if (sensor.getStatus() == INACTIVE) {
//					System.err.println("Train " + id +" exiting station "
//							+ (getStationIndex(sensor) + 1));
					isAtStation = false;
				}
			}
		}
		// We only check critical sections on active sensor triggers.
		else if (sensor.getStatus() == ACTIVE) {
			// Is not in a critical section.
			if (!isInCritical) {
				// Exiting upper station 1 and entering station 1 merge section
				if (sensorEqual(sensor, criticals[4])) {
					request(1);	// Requesting pass-through for station 1 merge
					tsi.setSwitch(switches[0].width, switches[0].height, swR);
					setTwoTrackSwitches(1);
					statSems[0].release();
//					System.err.println("Upper station 1 is now vacant.");
				}
				// Exiting lower station 1 and entering station 1 merge section
				else if (sensorEqual(sensor, criticals[5])) {
					request(1);	// Requesting pass-through for station 1 merge
					tsi.setSwitch(switches[0].width, switches[0].height, swL);
					setTwoTrackSwitches(1);
					statSems[1].release();
//					System.err.println("Lower station 1 is now vacant.");
				}
				// Exiting upper station 2 and entering station 2 merge section
				else if (sensorEqual(sensor, criticals[10])) {
					request(2);	// Requesting pass-through for station 2 merge
					tsi.setSwitch(switches[3].width, switches[3].height, swL);
					setTwoTrackSwitches(0);
					statSems[2].release();
//					System.err.println("Upper station 2 is now vacant.");
				}
				// Exiting lower station 2 and entering station 2 merge section
				else if (sensorEqual(sensor, criticals[11])) {
					request(2);	// Requesting pass-through for station 2 merge
					tsi.setSwitch(switches[3].width, switches[3].height, swR);
					setTwoTrackSwitches(0);
					statSems[3].release();
//					System.err.println("Lower station 2 is now vacant."); 
				}
				// Entering cross-section
				else if (sensorEqual(sensor, criticals[0]) ||
						sensorEqual(sensor, criticals[1]) ||
						sensorEqual(sensor, criticals[2]) ||
						sensorEqual(sensor, criticals[3])) {
					request(0);	// Requesting pass-through for cross-section
				}
				// Entering station 1 merge section and heading towards station
				else if (sensorEqual(sensor, criticals[6]) ||
						sensorEqual(sensor, criticals[7])) {
					request(1);	// Requesting pass-through for station 1 merge
					if (sensorEqual(sensor, criticals[6])) {
						critSems[3].release();
						System.err.println("Permit released! " + critSems[3].availablePermits() + " left.");
						tsi.setSwitch(switches[1].width, switches[1].height, swR);
					} else {
						tsi.setSwitch(switches[1].width, switches[1].height, swL);
					}
					if (statSems[0].availablePermits() == 1) {
						statSems[0].acquire();
						tsi.setSwitch(switches[0].width, switches[0].height, swR);
					} else {
						statSems[1].acquire();
						tsi.setSwitch(switches[0].width, switches[0].height, swL);
					}
				}
				// Entering station 2 merge section and heading towards station
				else if (sensorEqual(sensor, criticals[8]) ||
						sensorEqual(sensor, criticals[9])) {
					request(2);	// Requesting pass-through for station 1 merge
					if (sensorEqual(sensor, criticals[8])) {
						critSems[3].release();
						System.err.println("Permit released! " + critSems[3].availablePermits() + " left.");
						tsi.setSwitch(switches[2].width, switches[2].height, swL);
					} else {
						tsi.setSwitch(switches[2].width, switches[2].height, swR);
					}
					if (statSems[2].availablePermits() == 1) {
						statSems[2].acquire();
						tsi.setSwitch(switches[3].width, switches[3].height, swL);
					} else {
						statSems[3].acquire();
						tsi.setSwitch(switches[3].width, switches[3].height, swR);
					}
				}
				isInCritical = true;
//				System.err.println("Train " + id +" entering critical "
//						+ "section " + (getSectionIndex(sensor) + 1));
			// Is in a critical section.
			} else {
//				System.err.println("Train " + id +" exiting critical "
//						+ "section " + (getSectionIndex(sensor) + 1));
				signal(getSectionIndex(sensor));
				isInCritical = false;
			}
		}
	}
}
