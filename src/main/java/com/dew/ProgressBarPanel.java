package com.dew;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressBarPanel extends JPanel {
	private final JFrame frame;
	private final JProgressBar jProgressBar;
	
	public ProgressBarPanel() {
		super();
		jProgressBar = new JProgressBar();
		jProgressBar.setValue(0);
		jProgressBar.setStringPainted(true);
		add(jProgressBar);
		setSize(500, 200);
		frame = new JFrame();
		frame.setResizable(false);
		frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
	}
	
	public void showFrame() {
		frame.setVisible(true);
	}
	
	public void setValue(int value) {
		jProgressBar.setValue(value);
		if(value == 100) {
			frame.setVisible(false);
			frame.dispose();
		}
	}
}
