import javax.swing.text.Position;

import TSim.*;

public class Train implements Runnable {

	private int id;
	private int speed;
	private TSimInterface tsi;
	
	public static final Position stn1up  = new Position(12,3);
	public static final Position stn1dwn = new Position(12,5);
	public static final Position stn2up  = new Position(12,11);
	public static final Position stn2dwn = new Position(12,13);
	public static final Position sensor1 = new Position(6,6);
	public static final Position sensor2 = new Position(11,8);
	public static final Position sensor3 = new Position(14,7);
	public static final Position sensor4 = new Position(12,9);
	public static final Position sensor5 = new Position(7,10);
	
	public Train(int id, int speed) {
		tsi = TSimInterface.getInstance();
		this.id = id;
		this.speed = speed;
		run();
	}
	
	public void run() {
		try {
			tsi.setSpeed(id, speed);
		} catch (CommandException e) {
			e.printStackTrace();
		}
		
		while (true) {
			try {
				SensorEvent event = tsi.getSensor(id);
				
				for (int i = 0; i < 9; i++) {
					
				}
				
			} catch (CommandException | InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	public void changeSpeed(int v) {
		try {
			tsi.setSpeed(id, v);
		} catch (CommandException e) {
			e.printStackTrace();
		}
	}
	
	public Position[] getSensorsPos() {
		
	}
}

class Position {
	public int x;
	public int y;
	private String description;
	
	public Position(int x, int y) {
		this.x = x;
		this.y = y;
		description = "";
	}
	
	public Position(int x, int y, String description) {
		this.x = x;
		this.y = y;
		this.description = description;
	}
	
	public String description() {
		return description;
	}
}
