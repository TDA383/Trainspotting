import TSim.SensorEvent;

public class Lab1 {
	
	private static final int maxSpeed = 50;
	private static int simSpeed       = 100;
	
	public static void main(String[] args) {
		new Lab1(args);
	}

	@SuppressWarnings("static-access")
	public Lab1(String[] args) {
		int t1speed = maxSpeed, t2speed = maxSpeed;
		
		switch (args.length) {
		default:
		case 3: 
			simSpeed    = Integer.parseInt(args[2]); 
		case 2:
			t2speed = Integer.parseInt(args[1]);
		case 1:
			t1speed = Integer.parseInt(args[0]);	
		case 0:
			break;
		}
		
		Train t1 = new Train(1,t1speed);
		Train t2 = new Train(2,t2speed);
		t1.run(); t2.run();
		
		SensorEvent active;
		while(true) {
			active = t1.getActiveSensor();
			if (active != null) {
				if (t1.isCritical(active)) {
					t1.request(t1.getSectionNumber(active));
				} else if (t1.isStation(active)) {
					t1.brake();
					try {
						t1.sleep(2 + 2 * simSpeed * Math.abs(t1speed));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					t1.reverse();
					t1.run();
				}
			}
			active = t2.getActiveSensor();
			if (active != null) {
				if (t2.isCritical(active)) {
					t2.request(t2.getSectionNumber(active));
				} else if (t2.isStation(active)) {
					t2.brake();
					try {
						t2.sleep(2 + 2 * simSpeed * Math.abs(t2speed));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					t2.reverse();
					t2.run();
				} 
			}
		}
	}
}
