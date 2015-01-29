public class Lab1 {
	
	private static final int maxSpeed = 50;
	
	public static void main(String[] args) {
		new Lab1(args);
	}

	public Lab1(String[] args) {
		int t1speed = maxSpeed, t2speed = maxSpeed;
		int simSpeed = 100;
		
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
		Train.simSpeed = simSpeed;
		while(true) {
			t1.checkEnvironment();
			t2.checkEnvironment();
		}
	}
}