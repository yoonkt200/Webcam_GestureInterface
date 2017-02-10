package marionette;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import org.opencv.core.Mat;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class DeliveryController implements Initializable{

	
	@FXML private BorderPane deliveryPane;
	//@FXML private HBox deliveryHbox;
	@FXML private ImageView deliveryImage;
	//@FXML private Button okDelivery;
	//Button okDelivery;
	//@FXML private Button rejectDelivery;
	
	Camera camera = new Camera();
	public static DeliveryService deliveryService;
	private MediaPlayer soundPlayer;
	
	boolean okGesture = false;
	boolean rejectGesture = false;
	static boolean deliCancelChk = false;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		System.out.println("delivery initialize");

		//deliveryHbox = new HBox();
		
		okGesture=false;
		
		File deliveryFile = new File("src/image/delivery.png");
		Image imageFile = new Image(deliveryFile.toURI().toString());
		deliveryImage.setImage(imageFile);
		
		
		
		//File rejectImgFile = new File("src/image/reject.png");
		//Image rejectBtnImg = new Image(getClass().getResourceAsStream("src/image/reject.png"));
		//rejectDelivery = new Button();
		//rejectDelivery.setGraphic(new ImageView(rejectBtnImg));
		
		File deliverySoundFile = new File("src/sound/dingDong.mp3");
		Media deliveryRingSound = new Media(deliverySoundFile.toURI().toString());
		soundPlayer = new MediaPlayer(deliveryRingSound);
		soundPlayer.setVolume(0.2);
		
		camera.cameraActive();
		
		deliveryService = new DeliveryService();
		deliveryService.start();
	}
	
	class DeliveryService extends Service<Integer>{
		@Override
		protected Task<Integer> createTask() {
			Task<Integer> deliveryTask = new Task<Integer>() {
				@Override
				protected Integer call() throws Exception {
					int result =0;
					while (Camera.cameraActive) {
						System.out.println("============delivery thread running============");
						if (isCancelled()) {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									closeDeliveryWindow();
								}
							});
							break;
						}

						Mat matToShow = camera.grabFrame();
						Main.drawnWindow.showImage(matToShow);
						
						soundPlayer.play();

						if(okDeliveryGesture()) {
							//okDelivery.fire();
						}
						if(rejectDeliveryGesture()){
							//rejectDelivery.fire();
						}
					}
					return result;
				}
				
				@Override
				public boolean isCancelled() {
					return deliCancelChk;
				}
			};
			return deliveryTask;
		}
	}
	
	private boolean okDeliveryGesture(){
		if (Detection.fingerPts != null && Detection.handCenterPoint != null) {
			if (Detection.fingerPts.length == 5) {
				if ((Detection.angleIndexBetweenThumb < 30) && ((Detection.lengthIndexToThumb / Detection.lengthCenterToIndex) < 0.5)) {
					okGesture=true;
				} else {
					checkHandShape();
				}
			} 
		}
		return okGesture;
	}
	
	private void checkHandShape() { // OK사인을 안 하고 있는 경우의 손 모양 : 엄지, 검지, 중지 확인
		if ((Detection.angleIndexBetweenThumb > 30) && (Detection.angleIndexBetweenThumb < 70)) {
			if ((Detection.angleMiddleBetweenIndex > 0) && (Detection.angleMiddleBetweenIndex < 45)) {
				if (Detection.lengthCenterToIndex > Detection.lengthCenterToThumb) { // 검지가 더 긴경우
					if (Detection.lengthCenterToIndex < (2 * Detection.lengthCenterToThumb)) {
						okGesture = false;
					}
				} else if (Detection.lengthCenterToIndex < Detection.lengthCenterToThumb) { // 엄지가 더 긴경우
					if (Detection.lengthCenterToThumb < (2 * Detection.lengthCenterToIndex)) {
						okGesture = false;
					}
				}
			}
		}
	}
	
	private boolean rejectDeliveryGesture(){
		System.out.println("reject delivery gesture");
		
		return rejectGesture;
	}
	
	private void closeDeliveryWindow(){
		try{
			//Stage stage = (Stage) rejectDelivery.getScene().getWindow();
			//stage.close();
			MainController.mainService.restart();
			MainController.mediaPlayer2.setVolume(MainController.mainSound);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
