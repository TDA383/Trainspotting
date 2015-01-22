import java.util.concurrent.*;

import TSim.*;

public class TrainSemaphore extends Semaphore {

	private Train t;
	
	public TrainSemaphore(int permits) {
		super(permits);
	}
	
	public void acquire(Train t) throws InterruptedException {
		if (t != null) {
			this.t = t;
			t.changeSpeed(0);
		}
		super.acquire();
	}
	
	public void release() {
		super.release();
		if (t != null) {
			t.changeSpeed(t.getSpeed());
			t = null;
		}
	}
}
