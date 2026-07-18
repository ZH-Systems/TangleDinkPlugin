package tccrewplugin.sync;

import java.time.Instant;

public final class SyncStatusModel
{
	private final SyncStatus status;
	private final Instant lastAttempt;
	private final Instant lastSuccess;
	private final Integer lastHttpStatus;
	private final String sanitizedError;
	private final int pendingFieldCount;
	private final Integer manifestVersion;
	private final int retryAttempt;
	private final Instant nextRetryTime;

	public SyncStatusModel(
		SyncStatus status,
		Instant lastAttempt,
		Instant lastSuccess,
		Integer lastHttpStatus,
		String sanitizedError,
		int pendingFieldCount,
		Integer manifestVersion,
		int retryAttempt,
		Instant nextRetryTime
	)
	{
		this.status = status;
		this.lastAttempt = lastAttempt;
		this.lastSuccess = lastSuccess;
		this.lastHttpStatus = lastHttpStatus;
		this.sanitizedError = sanitizedError;
		this.pendingFieldCount = pendingFieldCount;
		this.manifestVersion = manifestVersion;
		this.retryAttempt = retryAttempt;
		this.nextRetryTime = nextRetryTime;
	}

	public SyncStatus getStatus()
	{
		return status;
	}

	public Instant getLastAttempt()
	{
		return lastAttempt;
	}

	public Instant getLastSuccess()
	{
		return lastSuccess;
	}

	public Integer getLastHttpStatus()
	{
		return lastHttpStatus;
	}

	public String getSanitizedError()
	{
		return sanitizedError;
	}

	public int getPendingFieldCount()
	{
		return pendingFieldCount;
	}

	public Integer getManifestVersion()
	{
		return manifestVersion;
	}

	public int getRetryAttempt()
	{
		return retryAttempt;
	}

	public Instant getNextRetryTime()
	{
		return nextRetryTime;
	}
}
