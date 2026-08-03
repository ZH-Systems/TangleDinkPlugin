package tccrewplugin.clanchat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClanMessageEvent
{
	private String author;
	private String content;
	private AccountType accountType;
	private SystemMessageType systemMessageType;
	private Integer timestamp;
	private String clanTitle;
	private String clanName;
	private String chatMessageType;
	private String pluginVersion;
	private String runeliteVersion;
	private String eventId;

	public ClanMessageEvent(String author, String content, AccountType accountType, SystemMessageType systemMessageType, String clanTitle, int timestamp)
	{
		this.author = author;
		this.content = content;
		this.accountType = accountType;
		this.systemMessageType = systemMessageType;
		this.clanTitle = clanTitle;
		this.timestamp = timestamp;
	}
}
