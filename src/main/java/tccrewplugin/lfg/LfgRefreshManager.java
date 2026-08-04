package tccrewplugin.lfg;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Singleton
public class LfgRefreshManager
{
	private final ScheduledExecutorService executor;
	private final AtomicBoolean visible = new AtomicBoolean();
	private final AtomicBoolean shutdown = new AtomicBoolean();
	private final AtomicBoolean running = new AtomicBoolean();
	private volatile Runnable refreshAction;
	private volatile ScheduledFuture<?> scheduledFuture;
	private volatile int intervalSeconds = 20;

	public LfgRefreshManager(ScheduledExecutorService executor)
	{
		this.executor = executor;
	}

	public void start(Runnable refreshAction, int intervalSeconds)
	{
		this.refreshAction = refreshAction;
		this.intervalSeconds = intervalSeconds;
		reschedule();
	}

	public void updateInterval(int intervalSeconds)
	{
		this.intervalSeconds = intervalSeconds;
		reschedule();
	}

	public void setVisible(boolean visible)
	{
		this.visible.set(visible);
		reschedule();
	}

	public void refreshNow()
	{
		Runnable action = refreshAction;
		if (shutdown.get() || action == null)
		{
			return;
		}
		executor.execute(this::runOnce);
	}

	public void shutdown()
	{
		shutdown.set(true);
		cancel();
	}

	private void reschedule()
	{
		cancel();
		if (shutdown.get() || !visible.get() || refreshAction == null)
		{
			return;
		}
		scheduledFuture = executor.scheduleWithFixedDelay(this::runOnce, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
	}

	private void cancel()
	{
		ScheduledFuture<?> future = scheduledFuture;
		if (future != null)
		{
			future.cancel(false);
		}
		scheduledFuture = null;
	}

	private void runOnce()
	{
		if (shutdown.get() || !visible.get())
		{
			return;
		}
		if (!running.compareAndSet(false, true))
		{
			return;
		}
		try
		{
			Runnable action = refreshAction;
			if (action != null)
			{
				action.run();
			}
		}
		finally
		{
			running.set(false);
		}
	}
}
