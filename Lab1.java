import TSim.*;

public class Lab1 {
	
	private static final int maxSpeed = 50;
	private static int simSpeed       = 100;
	
	public static void main(String[] args) {
		new Lab1(args);
	}

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
		
		Thread t1 = new Thread(new Train(1,t1speed));
		Thread t2 = new Thread(new Train(2,t2speed));
	}
}
