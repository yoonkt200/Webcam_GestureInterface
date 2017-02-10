package marionette;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class Sheet extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	BufferedImage image;
	int width, height;

	public Sheet(int h, int w) {
		width = w;
		height = h;

		setSize(w, h);
	}

	public void paintSheet(BufferedImage img) {
		image = img;
		repaint();
	}

	public void paintComponent(Graphics g) {
		g.drawImage(image, 0, 0, width, height, this);
	}
}
