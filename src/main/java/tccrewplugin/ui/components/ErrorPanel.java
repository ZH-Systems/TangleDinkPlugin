package tccrewplugin.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ErrorPanel extends JScrollPane
{
	private final JTextArea textArea = new JTextArea();

	public ErrorPanel()
	{
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		setBorder(BorderFactory.createTitledBorder("Status"));
		setViewportView(textArea);
	}

	public void setMessage(String message)
	{
		textArea.setText(message == null ? "" : message);
	}
}
