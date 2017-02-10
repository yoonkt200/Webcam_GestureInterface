package marionette;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

public class Camera {
	Mat preImg = new Mat();
	
	static VideoCapture camera = new VideoCapture();
	static boolean cameraActive = false;
	
	// 카메라 켜기 --> grabFrame함수 실행
	public void cameraActive() {
		if (!cameraActive) {
			Camera.camera.open(0);
			if (Camera.camera.isOpened()) {
				System.out.println("3 camera is opened");
				cameraActive = true;
			} else {
				System.err.println("Impossible to open the camera connection...");
			}
		} else {
			cameraActive = false;

			Camera.camera.release();
		}
	}
	
	// 카메라 설정이 완료되면 (켜져있으면) handDetection, fingerDetection 함수 실행
	public Mat grabFrame() {
		System.out.println("4 grabFrame");
		Detection detection = new Detection();

		Mat handDetectImg = new Mat();
		Mat canvas = new Mat();
		Mat finalImg = new Mat();

		if (Camera.camera.isOpened()) {
			try {
				Camera.camera.read(preImg);

				if (!preImg.empty()) {
					try {
						handDetectImg = detection.handDetection(this.preImg);
						handDetectImg.copyTo(canvas);
						detection.fingerDetection(handDetectImg);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// handDetection, fingerDetection 함수의 결과물을 현재 영상에 반영
					//fingerDetectImg.copyTo(this.preImg);
					//handDetectImg.copyTo(this.preImg);

					// 현재 영상에 손 중심좌표, 손가락 좌표들 표시
					finalImg = detection.drawInfo(canvas);
				}
			} catch (Exception e) {
				System.err.println("Exception during the image elaboration: " + e);
				e.printStackTrace();
			}
		}
		return finalImg;
	}
}
