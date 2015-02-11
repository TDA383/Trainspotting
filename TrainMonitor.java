import java.util.concurrent.locks.*;

public class TrainMonitor {
	private final Lock lock = new ReentrantLock(true);
	private final Condition notOccupied = lock.newCondition();
	
	private final int maxPermits = 1;
	private int permits;
	
	public TrainMonitor() { this(1); }
	
	public TrainMonitor(int permits) {
		if (permits <= 0) 				this.permits = 0;
		else if (permits >= maxPermits) this.permits = maxPermits;
		else 							this.permits = permits;
	}
	
	/** Tries to enter a critical section.
	 *  @throws InterruptedException 
	 */
	public void enter() throws InterruptedException {
		lock.lock();
		try {
			if (permits == 0) notOccupied.await();
		} finally {
			permits--;
			lock.unlock();	
		}
	}
	
	/** Exits a critical section.
	 */
	public void leave() {
		lock.lock();
		try {
			notOccupied.signal();
		} finally {
			if (permits < maxPermits) permits++;
			lock.unlock();	
		}
	}
	
	/** Tries to enter the critical section, if there is an available permit.
	 * 
	 *  @return 'true' if entering the critical section was successful, 'false'
	 *  otherwise
	 *  @throws InterruptedException 
	 */
	public Boolean tryEnter() throws InterruptedException {
		lock.lock();
		if (permits == 0) {
			lock.unlock();
			return false;
		}
		permits--;
		lock.unlock();
		return true;
	}
}
