package tccrewplugin.collectionlog;

import java.util.ArrayList;
import java.util.List;

public class CollectionLogCacheParser
{
	public List<Integer> parseItemIds(Object scriptArguments)
	{
		List<Integer> result = new ArrayList<>();
		if (scriptArguments == null)
		{
			return result;
		}

		if (scriptArguments instanceof Object[])
		{
			for (Object argument : (Object[]) scriptArguments)
			{
				addArgument(result, argument);
			}
		}
		else if (scriptArguments instanceof int[])
		{
			for (int argument : (int[]) scriptArguments)
			{
				result.add(argument);
			}
		}
		else if (scriptArguments instanceof Iterable)
		{
			for (Object argument : (Iterable<?>) scriptArguments)
			{
				addArgument(result, argument);
			}
		}
		return result;
	}

	private void addArgument(List<Integer> result, Object argument)
	{
		if (argument instanceof Number)
		{
			result.add(((Number) argument).intValue());
			return;
		}
		if (argument instanceof String)
		{
			try
			{
				result.add(Integer.parseInt(((String) argument).trim()));
			}
			catch (NumberFormatException ignored)
			{
				// skip
			}
		}
	}
}
