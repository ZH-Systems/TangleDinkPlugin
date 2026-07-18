package tccrewplugin.clanchat;

import tccrewplugin.clanchat.model.ClanMessageRecord;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

public class ClanMessageQueue
{
	private final int capacity;
	private final Deque<ClanMessageRecord> queue = new ArrayDeque<>();

	public ClanMessageQueue(int capacity)
	{
		this.capacity = Math.max(1, capacity);
	}

	public synchronized boolean offer(ClanMessageRecord record)
	{
		if (record == null)
		{
			return false;
		}
		if (queue.size() >= capacity)
		{
			return false;
		}
		queue.addLast(record);
		return true;
	}

	public synchronized ClanMessageRecord poll()
	{
		return queue.pollFirst();
	}

	public synchronized int size()
	{
		return queue.size();
	}

	public int capacity()
	{
		return capacity;
	}

	public synchronized void clear()
	{
		queue.clear();
	}
}
