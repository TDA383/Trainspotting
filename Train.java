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
	};
	
	private static final Semaphore[] statSems = new Semaphore[]{
		new Semaphore(1),
		new Semaphore(1),
		new Semaphore(1),
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
				// Critical section 1
				new SensorEvent(id,6,6,INACTIVE),
				new SensorEvent(id,8,5,INACTIVE),
				new SensorEvent(id,11,7,INACTIVE),
				new SensorEvent(id,10,8,INACTIVE),
				// Critical section 2					
				new SensorEvent(id,14,7,INACTIVE),	
				new SensorEvent(id,15,8,INACTIVE),			 
				new SensorEvent(id,12,9,INACTIVE),			 
				new SensorEvent(id,13,10,INACTIVE),	
				// Critical section 3
				new SensorEvent(id,7,9,INACTIVE),	
				new SensorEvent(id,6,10,INACTIVE),	
				new SensorEvent(id,6,11,INACTIVE),		
				new SensorEvent(id,4,13,INACTIVE)
	};
	
	/** A list of coordinates of the switches.
	 */
	static final Dimension[] switches = new Dimension[]{
			new Dimension(17, 7),
			new Dimension(15, 9),
			new Dimension(4, 9),
			new Dimension(3, 11),
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
		tsi.setDebug(true);
	}
	
	/** Starts the train with its specified speed.
	 */
	public void run() {
		isInCritical = true;
		super.run();
		try {
			tsi.setSpeed(id, speed);
		} catch (CommandException e) {
			e.printStackTrace();
		}
		try {
			statSems[0].acquire();
			statSems[2].acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			System.exit(1);}
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
	synchronized private void request(int sectionNumber) {
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
	synchronized private void signal(int sectionNumber) {
		try {
			tsi.setSpeed(id, speed);
		} catch (CommandException e) {
			e.printStackTrace();
		}
		
		critSems[sectionNumber].release();
	}
	
	private void checkEnvironment() throws CommandException, InterruptedException {
		int swL = TSimInterface.SWITCH_LEFT;
		int swR = TSimInterface.SWITCH_RIGHT;
		
		SensorEvent sensor = tsi.getSensor(id);
		if (isStation(sensor)) {
			// Is not at a station.
			if (!isAtStation) {
				System.err.println("Train " + id +" entering station "
						 + (getStationIndex(sensor) + 1));
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
				run();
			}
			// Is at a station.
			else {
				if (tsi.getSensor(id).getStatus() == INACTIVE) {
					System.err.println("Train " + id +" exiting station "
							+ (getStationIndex(sensor) + 1));
					isAtStation = false;
				}
			}
		} else {
			// Is not in a critical section.
			if (!isInCritical) {
				// Leaving upper station 1 and entering critical section 2.
				if (sensorEqual(sensor, criticals[4])) {
					request(1);
					tsi.setSwitch(switches[0].width, switches[0].height, swR);
					tsi.setSwitch(switches[1].width, switches[1].height, swL);
					statSems[0].release();
				}
				// Leaving lower station 1 and entering critical section 2 .
				else if (sensorEqual(sensor, criticals[5])) {
					request(1);
					tsi.setSwitch(switches[0].width, switches[0].height, swL);
					tsi.setSwitch(switches[1].width, switches[1].height, swL);
					statSems[1].release();
				}
				// Leaving upper station 2 and entering critical section 3.
				else if (sensorEqual(sensor, criticals[10])) {
					request(2);
					tsi.setSwitch(switches[3].width, switches[3].height, swL);
					tsi.setSwitch(switches[2].width, switches[1].height, swL);
					statSems[2].release();
				}
				// Leaving lower station 2 and entering critical section 3.
				else if (sensorEqual(sensor, criticals[11])) {
					request(2);
					tsi.setSwitch(switches[3].width, switches[3].height, swR);
					tsi.setSwitch(switches[2].width, switches[1].height, swL);
					statSems[3].release();
				}
				// Entering critical section 1
				else if (sensorEqual(sensor, criticals[0]) ||
						sensorEqual(sensor, criticals[1]) ||
						sensorEqual(sensor, criticals[2]) ||
						sensorEqual(sensor, criticals[3])) {
					request(0);
				}
				// Entering critical section 2 and heading towards station 1.
				else if (sensorEqual(sensor, criticals[6])) {
					request(1);
					tsi.setSwitch(switches[1].width, switches[1].height, swR);
					if (statSems[0].availablePermits() == 1) {
						statSems[0].acquire();
						tsi.setSwitch(switches[0].width, switches[0].height, swR);
					} else {
						statSems[1].acquire();
						tsi.setSwitch(switches[0].width, switches[0].height, swL);
					}
				}
				// Entering critical section 3 and heading towards station 2.
				else if (sensorEqual(sensor, criticals[9])) {
					request(2);
					tsi.setSwitch(switches[2].width, switches[2].height, swR);
					if (statSems[2].availablePermits() == 1) {
						statSems[2].acquire();
						tsi.setSwitch(switches[3].width, switches[3].height, swL);
					} else {
						statSems[3].acquire();
						tsi.setSwitch(switches[3].width, switches[3].height, swR);
					}	
				}
				isInCritical = true;
				System.err.println("Train " + id +" entering critical "
						+ "section " + (getSectionIndex(sensor) + 1));
			// Is in a critical section.
			} else {
				if (sensor.getStatus() == INACTIVE) {
					System.err.println("Train " + id +" exiting critical "
							+ "section " + (getSectionIndex(sensor) + 1));
					signal(getSectionIndex(sensor));
					isInCritical = false;
				}
			}
		}
	}
}