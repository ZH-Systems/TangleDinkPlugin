package tccrewplugin.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingConstants;

public class FolderHeader extends JButton
{
	public FolderHeader(String text)
	{
		super(text);
		setHorizontalAlignment(SwingConstants.LEFT);
		setFocusPainted(false);
		setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		setContentAreaFilled(false);
	}
}
