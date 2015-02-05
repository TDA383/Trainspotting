import java.util.concurrent.locks.*;

public class TrainMonitor {
	private final Lock lock = new ReentrantLock(true);
	
	static Boolean inCritical = false;
	
	private final Condition occupied = lock.newCondition();
	
	/** Tries to enter a critical section.
	 *  @throws InterruptedException 
	 */
	public void enter() throws InterruptedException {
		lock.lock();
		
		try {
			if (inCritical) {
				occupied.await();
			}
			inCritical = true;
		} finally {
			lock.unlock();	
		}
	}
	
	/** Exits a critical section.
	 */
	public void leave(){
		lock.lock();
		
		try {
			occupied.signal();
			inCritical = false;
		} finally {
			lock.unlock();	
		}
	}
	
	/** Tries to lock the monitor.
	 * 
	 *  @return 'true' if the locking was successful, 'false' otherwise
	 */
	public Boolean tryLock() {
		if (inCritical) {
			return false;
		} else {
			lock.lock();
			return true;
		}
	}
}
