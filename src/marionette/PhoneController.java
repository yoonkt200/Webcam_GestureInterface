package marionette;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import org.opencv.core.Mat;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class PhoneController implements Initializable{

	@FXML private BorderPane phonePane;
	@FXML private ImageView phoneImage;
	@FXML private Button answerPhone;
	@FXML private Button rejectPhone;
	
	Camera camera = new Camera();
	private PhoneService phoneService;
	private MediaPlayer soundPlayer;
	
	boolean answerGesture = false;
	boolean rejectGesture = false;
	static boolean phoneCancelChk = false;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		System.out.println("phone initialize");
		
		
		
		File phoneFile = new File("src/image/phone.png");
		Image imageFile = new Image(phoneFile.toURI().toString());
		phoneImage.setImage(imageFile);
		
		File phoneSoundFile = new File("src/sound/phoneRing.mp3");
		Media phoneRingSound = new Media(phoneSoundFile.toURI().toString());
		soundPlayer = new MediaPlayer(phoneRingSound);
		soundPlayer.setVolume(0.2);
		
		camera.cameraActive();
		
		phoneService = new PhoneService();
		phoneService.start();
	}
	
	class PhoneService extends Service<Integer>{
		@Override
		protected Task<Integer> createTask() {
			Task<Integer> phoneTask = new Task<Integer>() {
				@Override
				protected Integer call() throws Exception {
					int result =0;
					while(Camera.cameraActive){
						System.out.println("***************phone thread running***********");
						if(isCancelled()){
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									closePhoneWindow();
								}
							});
							soundPlayer.stop();
							break;
						}
						
						Mat matToShow = camera.grabFrame();
						Main.drawnWindow.showImage(matToShow);
						
						if(answerPhone.isDisabled()){
							soundPlayer.stop();
						} else {
							soundPlayer.play();
						}
						
						if(answerPhoneGesture()){
							answerPhone.fire();
						}
						if(rejectPhoneGesture()){
							rejectPhone.fire();
						}
					}
					return result;
				}
				
				@Override
				public boolean isCancelled() {
					return phoneCancelChk;
				}
			};
			return phoneTask;
		}
	}

	//전화받기 제스처
	private boolean answerPhoneGesture(){
		System.out.println("answer phone gesture");
		
		answerPhone.setText("Calling...");
		answerPhone.setDisable(true);
		
		return answerGesture;
	}
	
	//전화거절 제스처
	private boolean rejectPhoneGesture(){
		System.out.println("reject phone gesture");
		
		return rejectGesture;
	}
	
	private void closePhoneWindow(){
		try{
			Stage stage = (Stage) rejectPhone.getScene().getWindow();
			stage.close();
			MainController.mainService.restart();
			MainController.mediaPlayer2.setVolume(MainController.mainSound);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
