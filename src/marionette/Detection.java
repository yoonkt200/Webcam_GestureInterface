package marionette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

public class Detection {

	private static Scalar ycrcb_min_finger = new Scalar(30, 170, 70, 0);
	private static Scalar ycrcb_max_finger = new Scalar(140, 230, 130, 0);

	private static Scalar hsv_min_hand = new Scalar(100, 50, 50, 0);
	private static Scalar hsv_max_hand = new Scalar(130, 255, 255, 0);

	static FingerPoint[] fingerPts;
	static Point[] fingerPoint;
	static final String[] fingerNames = { "LITTLE", "RING", "MIDDLE", "INDEX", "THUMB" };
	static Point handCenterPoint;
	static double radius = 5;
	static int contourAxisAngle = 0;
	static boolean imageMasking = false;

	// 손 각도 및 거리
	static int angleIndexBetweenThumb = 40;
	static int angleMiddleBetweenIndex = 40;
	static double lengthIndexToThumb = 100;
	static double lengthCenterToIndex = 100;
	static double lengthCenterToMiddle = 100;
	static double lengthCenterToThumb = 100;

	// 외곽선 그리는 변수들
	static List<MatOfPoint> hullmop;
	static List<Integer> cdList;
	static MatOfInt4 defects;
	static Point data[];

	// 손 인식
	public Mat handDetection(Mat preImg) {
		Mat handImg = new Mat();
		Mat handHierachy = new Mat();
		Mat handKernel = Imgproc.getStructuringElement(Imgproc.MORPH_OPEN, new Size(3, 3));

		// hand detection : blue
		Imgproc.cvtColor(preImg, handImg, Imgproc.COLOR_BGR2HSV);
		Core.inRange(handImg, hsv_min_hand, hsv_max_hand, handImg);

		Imgproc.dilate(handImg, handImg, handKernel);
		Imgproc.morphologyEx(handImg, handImg, Imgproc.MORPH_CLOSE, handKernel);
		Imgproc.erode(handImg, handImg, handKernel);
		Imgproc.medianBlur(handImg, handImg, 9);

		// 외곽선 찾기
		List<MatOfPoint> handContours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(handImg, handContours, handHierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		// 가장 큰 외곽선 그리기
		int idx = findBiggest(handContours);
		Imgproc.drawContours(handImg, handContours, idx, new Scalar(255, 255, 255), -1, 0, handHierachy, 5,
				new Point());

		// 손 중심점 찾기
		Moments moment = new Moments();
		if (handContours.size() != 0) {
			moment = Imgproc.moments(handContours.get(idx), false);
		}

		preImg = findHandInfo(preImg, handImg, moment);

		// 손 중심점과 손바닥에 원이 그려진 상태를 반환
		return preImg;
	}

	// 손가락 인식
	public void fingerDetection(Mat preImg) {
		Mat fingerImg = new Mat();
		Imgproc.resize(preImg, fingerImg, new Size(preImg.width(), preImg.height()));
		Mat fingerHierachy = new Mat();
		Mat fingerkernel = Imgproc.getStructuringElement(Imgproc.MORPH_OPEN, new Size(7, 7));

		// finger detection : red
		Imgproc.cvtColor(preImg, fingerImg, Imgproc.COLOR_BGR2YCrCb);
		Core.inRange(fingerImg, ycrcb_min_finger, ycrcb_max_finger, fingerImg);

		Imgproc.medianBlur(fingerImg, fingerImg, 11);
		Imgproc.morphologyEx(fingerImg, fingerImg, Imgproc.MORPH_CLOSE, fingerkernel);
		Imgproc.erode(fingerImg, fingerImg, fingerkernel);

		// 손가락 외곽선 그리기
		List<MatOfPoint> fingerContours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> fingerContoursFinal = new ArrayList<MatOfPoint>();
		Imgproc.findContours(fingerImg, fingerContours, fingerHierachy, Imgproc.RETR_EXTERNAL,
				Imgproc.CHAIN_APPROX_SIMPLE);

		// fingerContours가 5개 초과일 때, 큰거 순으로 5개만 잡기
		if (fingerContours.size() > 5) {
			Map<Integer, MatOfPoint> fingerContoursMap = new HashMap<Integer, MatOfPoint>();
			Map<Integer, Double> fingerAreaMap = new HashMap<Integer, Double>();

			for (int i = 0; i < fingerContours.size(); i++) {
				double fingerArea = Imgproc.contourArea(fingerContours.get(i), false);
				fingerAreaMap.put(i, fingerArea);
				fingerContoursMap.put(i, fingerContours.get(i));
			}

			Map<Integer, Double> fingerAreaSortedResult = new LinkedHashMap<Integer, Double>();

			fingerAreaSortedResult = sortByValue(fingerAreaMap);

			// 윤곽선 index와 넓이 index 비교 후, 같은 index의 윤곽선만 finalContour로 넣기
			for (Entry<Integer, MatOfPoint> contour : fingerContoursMap.entrySet()) {
				for (Entry<Integer, Double> area : fingerAreaSortedResult.entrySet()) {
					if (contour.getKey() == area.getKey()) {
						fingerContoursFinal.add(fingerContoursMap.get(contour.getKey()));
					}
				}
			}
		} else { // 5개 초과가 아닐 때
			for (int j = 0; j < fingerContours.size(); j++) {
				fingerContoursFinal.add(fingerContours.get(j));
			}
		}

		if (fingerContoursFinal.size() != 0) {
			List<Moments> fingerMoments = new ArrayList<Moments>(fingerContoursFinal.size());

			// 손가락 윤곽선의 개수만큼 fingerPts 개수 할당
			fingerPts = new FingerPoint[fingerContoursFinal.size()];

			// 각 손가락의 무게중심 찾기
			for (int i = 0; i < fingerContoursFinal.size(); i++) {
				fingerMoments.add(i, Imgproc.moments(fingerContoursFinal.get(i), false));
				Moments point = fingerMoments.get(i);
				int finger_x = (int) (point.get_m10() / point.get_m00());
				int finger_y = (int) (point.get_m01() / point.get_m00());

				fingerPts[i] = new FingerPoint(finger_x, finger_y);
				System.out.println(i + " : " + finger_x);
			}
		}

		if (fingerPts.length != 0) {
			calculateFtsAngle(); // 손가락 각도 계산
			labelFts(); // 손가락 레이블링
			calculateGestureInfo();
		}
	}

	// 빨강이 5개 이상 잡힐 때, 넓이가 큰 순서로 정렬 후 5개(손가락) 선택
	private static <K, V extends Comparable<? super V>> Map<Integer, Double> sortByValue(
			Map<Integer, Double> fingerAreaMap) {

		List<Entry<Integer, Double>> list = new LinkedList<Entry<Integer, Double>>(fingerAreaMap.entrySet());
		// 넓이 내림차순 정렬
		Collections.sort(list, new Comparator<Entry<Integer, Double>>() {
			@Override
			public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});

		List<Entry<Integer, Double>> subList = new LinkedList<Entry<Integer, Double>>();
		// 넓이가 큰 순서로 5개 선택
		subList = list.subList(0, 5);

		Map<Integer, Double> fingerAreaSortedMap = new LinkedHashMap<Integer, Double>();

		for (Entry<Integer, Double> entry : subList) {
			fingerAreaSortedMap.put(entry.getKey(), entry.getValue());
		}

		return fingerAreaSortedMap;
	}

	// 가장 큰 손 외곽선 찾기
	private int findBiggest(List<MatOfPoint> contours) {
		double largest_area = 0;
		int largest_contour_index = 0;

		for (int i = 0; i < contours.size(); i++) {
			double area = Imgproc.contourArea(contours.get(i), false);
			if (area > largest_area) {
				largest_area = area;
				largest_contour_index = i;
			}
		}
		return largest_contour_index;
	}

	// 손 중심정보 찾기
	private Mat findHandInfo(Mat preImg, Mat handImg, Moments moment) {
		double m00 = moment.get_m00();
		double m10 = moment.get_m10();
		double m01 = moment.get_m01();
		double m11 = moment.get_m11();
		double m20 = moment.get_m20();
		double m02 = moment.get_m02();

		if (m00 != 0) { // calculate center
			int xCenter = (int) Math.round(m10 / m00) * 1;
			int yCenter = (int) Math.round(m01 / m00) * 1;
			Point newPoint = new Point(xCenter, yCenter);
			handCenterPoint = newPoint;
		}

		contourAxisAngle = calculateTilt(m11, m20, m02); // 중심축

		/*
		 * if (fingerPts.length != 0) { int yTotal = 0; for (Point pt :
		 * fingerPts) yTotal += pt.y; int avgYFinger = yTotal /
		 * fingerPts.length; if (avgYFinger > handCenterPoint.y) // fingers
		 * below COG contourAxisAngle += 180; }
		 */

		contourAxisAngle = 180 - contourAxisAngle;

		Mat distImg = new Mat();
		Imgproc.distanceTransform(handImg, distImg, Imgproc.CV_DIST_L2, 5);
		MinMaxLocResult mmp = Core.minMaxLoc(distImg);
		radius = mmp.maxVal;

		Mat maskImg = new Mat();

		try {
			Rect roi = new Rect((int) handCenterPoint.x - (int) (radius * 2.7),
					(int) handCenterPoint.y - (int) (radius * 2.7), (int) (radius * 2.7) * 2, (int) (radius * 2.7) * 2);
			maskImg = preImg.submat(roi).clone();
			handCenterPoint.x = radius * 2.7;
			handCenterPoint.y = radius * 2.7;
		} catch (Exception e) {
			imageMasking = false;
			return preImg;
		}

		imageMasking = true;

		return maskImg;
	}

	// 손 축 찾기
	private int calculateTilt(double m11, double m20, double m02) {

		double diff = m20 - m02;
		if (diff == 0) {
			if (m11 == 0)
				return 0;
			else if (m11 > 0)
				return 45;
			else
				return -45;
		}

		double theta = 0.5 * Math.atan2(2 * m11, diff);
		int tilt = (int) Math.round(Math.toDegrees(theta));

		if ((diff > 0) && (m11 == 0))
			return 0;
		else if ((diff < 0) && (m11 == 0))
			return -90;
		else if ((diff > 0) && (m11 > 0))
			return tilt;
		else if ((diff > 0) && (m11 < 0))
			return (180 + tilt);
		else if ((diff < 0) && (m11 > 0))
			return tilt;
		else if ((diff < 0) && (m11 < 0))
			return (180 + tilt);

		System.out.println("Error in moments for tilt angle");
		return 0;
	}

	// 손가락 각도 계산
	private void calculateFtsAngle() {
		for (FingerPoint finger : fingerPts) {
			double yOffset = handCenterPoint.y - finger.y;
			double xOffset = finger.x - handCenterPoint.x;

			double theta = Math.atan2(yOffset, xOffset);
			int angleTip = (int) Math.round(Math.toDegrees(theta));
			int offsetAngleTip = angleTip + (90 - contourAxisAngle);
			finger.setAngle(offsetAngleTip);
		}

	}

	// 손가락 레이블링
	private static void labelFts() {
		Arrays.sort(fingerPts);

		for (int i = 0; i < fingerPts.length; i++) {
			fingerPts[i].setFingerName(fingerNames[i]);
		}
	}

	// 엄지, 검지 각도 및 거리 계산
	private void calculateGestureInfo() {
		angleIndexBetweenThumb = fingerPts[4].getAngle() - fingerPts[3].getAngle();
		angleMiddleBetweenIndex = fingerPts[3].getAngle() - fingerPts[2].getAngle();
		lengthCenterToIndex = Math.sqrt(Math.pow(Math.abs(fingerPts[3].x - handCenterPoint.x), 2)
				+ Math.pow(Math.abs(fingerPts[3].y - handCenterPoint.y), 2));
		lengthCenterToMiddle = Math.sqrt(Math.pow(Math.abs(fingerPts[2].x - handCenterPoint.x), 2)
				+ Math.pow(Math.abs(fingerPts[2].y - handCenterPoint.y), 2));
		lengthCenterToThumb = Math.sqrt(Math.pow(Math.abs(fingerPts[4].x - handCenterPoint.x), 2)
				+ Math.pow(Math.abs(fingerPts[4].y - handCenterPoint.y), 2));
		lengthIndexToThumb = Math.sqrt(Math.pow(Math.abs(fingerPts[4].x - fingerPts[3].x), 2)
				+ Math.pow(Math.abs(fingerPts[4].y - fingerPts[3].y), 2));
	}

	// 손 중심점, 손바닥 원, 손가락 무게중심, 손가락 라벨, 손 중심점과 손끝사이 선 그리고 결과 프레임 반환
	public Mat drawInfo(Mat finalImg) {
		System.out.println("5 drawInfo");
		if ((fingerPts != null) && (handCenterPoint != null)) {
			System.out.println("drawing now");

			Core.circle(finalImg, handCenterPoint, 9, new Scalar(0), 4);
			Core.circle(finalImg, handCenterPoint, (int) (radius * 1.4), new Scalar(0), 4);

			for (int i = 0; i < fingerPts.length; i++) {
				if (fingerPts[i] != null) {
					try {
						Core.putText(finalImg, fingerPts[i].getFingerName(), fingerPts[i], Core.FONT_HERSHEY_SIMPLEX,
								0.5, new Scalar(255, 124, 113, 0));
						Core.circle(finalImg, fingerPts[i], 4, new Scalar(255, 124, 113, 0), 4);

						Core.line(finalImg, handCenterPoint, fingerPts[i], new Scalar(0), 4);
					} catch (Exception e) {
						System.err.println("Error in drawing....");
						e.printStackTrace();
					}
				}
			}
		}

		return finalImg;
	}
}
