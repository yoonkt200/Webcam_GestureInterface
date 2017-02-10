package marionette;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Stack;

import org.opencv.core.Mat;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class MainController implements Initializable{
	@FXML private BorderPane mainPane;
	@FXML private HBox mainHbox;
	@FXML private MediaView mediaView1;
	@FXML private MediaView mediaView2;
	@FXML private MediaView mediaView3;
	@FXML private MediaView mediaView4;
	@FXML private MediaView mediaView5;
	@FXML private Label mediaText1;
	@FXML private Label mediaText2;
	@FXML private Label mediaText3;
	@FXML private Label mediaText4;
	@FXML private Label mediaText5;
	public static List<MediaView> mediaViews = new ArrayList<MediaView>();
	@FXML
	public MediaView resultView = new MediaView();
	
	private MediaPlayer mediaPlayer;
	private Media media;
	public int idx;

	private MediaView selectedView = new MediaView();
	public static MediaPlayer mediaPlayer2;
	
	static boolean volumeGesture = false;

	static Stack<Double> volume = new Stack<Double>();
	static Stack<Integer> channel = new Stack<Integer>();
	
	@FXML private CheckBox activatedChk = new CheckBox(); // (JAVAFX) activated checkbox
	@FXML private Slider volumeSlider = new Slider(); // (JAVAFX) volume slider
	
	Camera camera = new Camera();
	
	public static MainService mainService;
	
	static Double mainSound=50.0;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		System.out.println("2 initialize");	
		
		mainHbox = new HBox();
		
		mediaViews.add(mediaView1);
		mediaViews.add(mediaView2);
		mediaViews.add(mediaView3);
		mediaViews.add(mediaView4);
		mediaViews.add(mediaView5);
		
		mediaText1.setText("sample1");
		mediaText2.setText("sample2");
		mediaText3.setText("sample3");
		mediaText4.setText("sample4");
		mediaText5.setText("sample5");
		
		for (int i = 0; i < mediaViews.size(); i++) {
			String path = new File("src/media/sample" + (i + 1) + ".mp4").getAbsolutePath();
			media = new Media(new File(path).toURI().toString());
			mediaPlayer = new MediaPlayer(media);
			mediaViews.get(i).setMediaPlayer(mediaPlayer);
			mediaPlayer.setAutoPlay(true);
			mediaPlayer.setVolume(0);
		}
		camera.cameraActive();
		
		mainService = new MainService();
		mainService.start();
	}
	
	class MainService extends Service<Integer>{
		@Override
		protected Task<Integer> createTask() {
			Task<Integer> mainTask = new Task<Integer>() {
				@Override
				protected Integer call() throws Exception {
					int result =0;
					
					while(Camera.cameraActive){
						System.out.println("============main thread running============");
						
						Mat matToShow = camera.grabFrame();
						Main.drawnWindow.showImage(matToShow);

						if (resultView.getMediaPlayer() != null) {
							// 볼륨 제스처 체크
							checkVolumeGesture(activatedChk, volumeSlider);
						}
						// 채널 제스처 체크
						checkChannelGesture();
					}
					return result;
				}
			};
			return mainTask;
		}
	}
	
	// 채널 조절 제스처
	public void checkChannelGesture() {
		System.out.println("7 checkchannel");
		if (Detection.fingerPts != null && Detection.handCenterPoint != null) {
			if (Detection.fingerPts.length == 5) {
				if (!channel.isEmpty()) {
					int prevHandCenterPoint = channel.pop();

					if (Detection.handCenterPoint.x < prevHandCenterPoint - 10) {
						if (idx + 1 < mediaViews.size()) {
							mediaViews.get(idx).setFitWidth(90);
							mediaViews.get(idx).setFitHeight(70);
							mediaViews.get(idx + 1).setFitWidth(120);
							mediaViews.get(idx + 1).setFitHeight(100);
							selectedView = mediaViews.get(idx + 1);
							idx++;
						}
					} else if (Detection.handCenterPoint.x > prevHandCenterPoint + 10) {
						if (idx - 1 >= 0) {
							mediaViews.get(idx).setFitWidth(90);
							mediaViews.get(idx).setFitHeight(70);
							mediaViews.get(idx - 1).setFitWidth(120);
							mediaViews.get(idx - 1).setFitHeight(100);
							selectedView = mediaViews.get(idx - 1);
							idx--;
						}
					}

					channel.push((int) Detection.handCenterPoint.x);
				} else {
					channel.push((int) Detection.handCenterPoint.x);
				}
			} else {
				for (int k = 0; k < Detection.fingerPts.length; k++) {
					Double distance = Math.sqrt(Math.pow(Detection.handCenterPoint.x - Detection.fingerPts[k].x, 2)
							+ Math.pow(Detection.handCenterPoint.y - Detection.fingerPts[k].y, 2));
					// 손 중심점과 손가락 사이의 거리가 60이하이면 선택 된 것으로 판정
					// 선택한 화면 영상 메인 영상화면에 뿌리기
					if (distance < 60) {
						if (!(selectedView.getId()).equals(resultView.getId()) || resultView.getId().equals("resultView")) {
							resultView.setId(selectedView.getId());

							if (resultView.getMediaPlayer() != null) {
								mediaPlayer2.stop();
							}

							mediaPlayer2 = new MediaPlayer(selectedView.getMediaPlayer().getMedia());
							resultView.setMediaPlayer(mediaPlayer2);

							Duration selectedCurrentTime = selectedView.getMediaPlayer().getCurrentTime();
							resultView.getMediaPlayer().setStartTime(selectedCurrentTime);

							mediaPlayer2.setAutoPlay(true);
							mediaPlayer2.play();
							
							// 볼륨 조정
							volumeSlider.valueProperty().addListener(new InvalidationListener() {
								@Override
								public void invalidated(Observable observable) {
									if (volumeSlider.isValueChanging()) {
										mediaPlayer2.setVolume(volumeSlider.getValue() / 100);
									}
								}
							});
						}
					}
				}
			}
		}
	}
		
	// 볼륨 조절 제스처
	private void checkVolumeGesture(CheckBox activatedChk, Slider volumeSlider) { // 엄지와 검지를 놨다 떼는 순간의 변수정보들을 이용하여 제스처 판단
		System.out.println("6 checkvolume");
		if (Detection.fingerPts != null && Detection.handCenterPoint != null) {
			if (Detection.fingerPts.length == 5) {
				if ((Detection.angleIndexBetweenThumb < 30) && ((Detection.lengthIndexToThumb / Detection.lengthCenterToIndex) < 0.5)) {
					volumeGesture = true;
				} else {
					checkHandShape();
				}
			} 

			if (volumeGesture) {
				activatedChk.setSelected(true);
				volumeSlider.setValueChanging(true);
				// volume slider 조절 부분 : Stack(데이터 타입) 이용

				if (!volume.isEmpty()) { // 볼륨이 조절이 됬었다면
					Double prevHandCenterPoint = volume.pop(); // 이전에 Stack에 있던 volume값
					Double now = volumeSlider.getValue(); // 현재의 volume slide값
					System.out.println("now : " + now);

					// 손이 위아래로 움직임에 따라 1씩 볼륨 증가or감소
					if (Detection.handCenterPoint.y < prevHandCenterPoint) {
						volumeSlider.setValue(now + 5.0);
					} else if (Detection.handCenterPoint.y > prevHandCenterPoint) {
						volumeSlider.setValue(now - 5.0);
					}
					volume.push(Detection.handCenterPoint.y); // 변경된 volume값 Stack에 push
					mainSound = now;
					System.out.println("mainsound : " + mainSound);
				} else { // 볼륨값이 default 값(50)이면
					volume.push(Detection.handCenterPoint.y);
				}
			} else {
				if(activatedChk.isSelected()){
					activatedChk.setSelected(false);
				}
			}
		}
	}
	
	private void checkHandShape(){ // OK사인을 안 하고 있는 경우의 손 모양 : 엄지, 검지, 중지 확인
		
		if((Detection.angleIndexBetweenThumb > 30) && (Detection.angleIndexBetweenThumb < 70)){
			if((Detection.angleMiddleBetweenIndex > 0) && (Detection.angleMiddleBetweenIndex < 45)){
				if(Detection.lengthCenterToIndex > Detection.lengthCenterToThumb){ // 검지가 더 긴경우
					if(Detection.lengthCenterToIndex < (2*Detection.lengthCenterToThumb)){
						volumeGesture = false;
					}
				}
				else if(Detection.lengthCenterToIndex < Detection.lengthCenterToThumb){ // 엄지가 더 긴경우
					if(Detection.lengthCenterToThumb < (2*Detection.lengthCenterToIndex)){
						volumeGesture = false;
					}
				}
			}
		}
	}

}
