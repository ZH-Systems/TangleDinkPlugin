package tccrewplugin.lfg;

import org.junit.jupiter.api.Test;
import tccrewplugin.lfg.model.LfgCategory;
import tccrewplugin.lfg.model.LfgGroup;
import tccrewplugin.lfg.model.LfgGroupStatus;
import tccrewplugin.lfg.model.LfgMember;
import tccrewplugin.lfg.model.LfgPermissions;
import tccrewplugin.lfg.model.LfgSource;
import tccrewplugin.lfg.ui.LfgGroupCard;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LfgGroupCardTest
{
	@Test
	void prefersServerLeavePermissionOverLocalRsnMatch()
	{
		LfgGroupCard card = new LfgGroupCard();
		card.setGroup(buildGroup(), "Different Display Name", new NoopActionHandler());

		JButton leaveButton = findButton(card, "Leave");
		assertNotNull(leaveButton);
		assertTrue(leaveButton.isEnabled());
		assertEquals("Leave", leaveButton.getText());
	}

	private LfgGroup buildGroup()
	{
		LfgCategory category = new LfgCategory("raid-id", "raid", "Raid", "Raids", true, 10);
		LfgMember creator = new LfgMember("linked-player-1", "Creator", null, LfgSource.RUNELITE, Instant.parse("2026-08-05T12:00:00Z"));
		return new LfgGroup(
			"group-1",
			1,
			category,
			"Theatre of Blood",
			"Learner friendly",
			Instant.parse("2026-08-05T20:00:00Z"),
			5,
			LfgGroupStatus.OPEN,
			LfgSource.RUNELITE,
			creator,
			List.of(creator),
			new LfgPermissions(false, true, true),
			null,
			Instant.parse("2026-08-05T12:00:00Z"),
			Instant.parse("2026-08-05T12:00:00Z"),
			Instant.parse("2026-08-06T02:00:00Z")
		);
	}

	private JButton findButton(Container container, String label)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JButton)
			{
				JButton button = (JButton) component;
				if (label.equals(button.getText()))
				{
					return button;
				}
			}
			if (component instanceof Container)
			{
				JButton nested = findButton((Container) component, label);
				if (nested != null)
				{
					return nested;
				}
			}
		}
		return null;
	}

	private static final class NoopActionHandler implements LfgGroupCard.ActionHandler
	{
		@Override
		public void onJoin(LfgGroup group)
		{
		}

		@Override
		public void onLeave(LfgGroup group)
		{
		}

		@Override
		public void onClose(LfgGroup group)
		{
		}
	}
}
