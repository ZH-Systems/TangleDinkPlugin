package tccrewplugin.api;

public final class ApiResult<T>
{
	private final int statusCode;
	private final T body;
	private final String error;

	public ApiResult(int statusCode, T body, String error)
	{
		this.statusCode = statusCode;
		this.body = body;
		this.error = error;
	}

	public int getStatusCode()
	{
		return statusCode;
	}

	public T getBody()
	{
		return body;
	}

	public String getError()
	{
		return error;
	}

	public boolean isSuccess()
	{
		return statusCode >= 200 && statusCode < 300;
	}
}
