package tccrewplugin.sync;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SyncScheduler
{
	private final ScheduledExecutorService executor;
	private final AtomicReference<ScheduledFuture<?>> periodic = new AtomicReference<>();
	private final AtomicReference<ScheduledFuture<?>> oneShot = new AtomicReference<>();

	public SyncScheduler(ScheduledExecutorService executor)
	{
		this.executor = executor;
	}

	public synchronized void schedulePeriodic(Runnable task, long initialDelay, long period, TimeUnit unit)
	{
		cancelPeriodic();
		periodic.set(executor.scheduleWithFixedDelay(task, initialDelay, period, unit));
	}

	public synchronized void scheduleOnce(Runnable task, long delay, TimeUnit unit)
	{
		cancelOneShot();
		oneShot.set(executor.schedule(task, delay, unit));
	}

	public synchronized void cancelPeriodic()
	{
		ScheduledFuture<?> future = periodic.getAndSet(null);
		if (future != null)
		{
			future.cancel(false);
		}
	}

	public synchronized void cancelOneShot()
	{
		ScheduledFuture<?> future = oneShot.getAndSet(null);
		if (future != null)
		{
			future.cancel(false);
		}
	}

	public synchronized void cancelAll()
	{
		cancelPeriodic();
		cancelOneShot();
	}
}
