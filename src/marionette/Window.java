package marionette;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class Window extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	VideoCapture camera = new VideoCapture();
	Sheet sheet;

	int height, width;

	public Window(int length, int breadth) {
		width = length;
		height = breadth;
		sheet = new Sheet(breadth, length);

		this.setLayout(null);
		this.setSize(new Dimension(length, breadth));
		this.add(sheet);
		this.setFocusable(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
	}

	public void showImage(Mat m) {
		InputStream in = null;
		try {
			MatOfByte matOfByte = new MatOfByte();
			Highgui.imencode(".png", m, matOfByte);

			byte[] byteArray = matOfByte.toArray();
			
			in = new ByteArrayInputStream(byteArray);
			sheet.paintSheet(ImageIO.read(in));

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try{
				if(in != null){
					in.close();
				}
			}catch(Exception e){ }
		}
	}
}