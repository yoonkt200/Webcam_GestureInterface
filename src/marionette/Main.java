package marionette;

import org.opencv.core.Core;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Main extends Application {
	static int SENSITIVITY_VALUE = 20;
	static int BLUR_SIZE = 10;
	static int WIDTH = 500;
	static int HEIGHT = 400;

	static Window drawnWindow;
	
	BorderPane paneMain, panePhone, paneDelivery;
	Scene sceneMain, scenePhone, sceneDelivery;
	Stage stageMain, stagePhone, stageDelivery;
	
	@Override
	public void start(Stage stage) {
		System.out.println("1 Start");
		try {
			stageMain = stage;
			
			FXMLLoader loader1 = new FXMLLoader(getClass().getResource("MarionetteJFX.fxml"));
			paneMain = (BorderPane) loader1.load();
			
			sceneMain = new Scene(paneMain, 800, 600);
			sceneMain.getStylesheets().add(getClass().getResource("marionetteStyle.css").toExternalForm());
			
			stageMain.setTitle("Marionette Project");
			stageMain.setScene(sceneMain);

			stageMain.show();
			stageMain.setResizable(false);
			
			sceneMain.setOnKeyPressed(new EventHandler<KeyEvent>(){
				@Override
				public void handle(KeyEvent event) {
					switch (event.getCode()) {
						case C:
							try {
								MainController.mediaPlayer2.setVolume(0);
								MainController.mainService.cancel();
								Camera.cameraActive=false;
								
								FXMLLoader loader2 = new FXMLLoader(getClass().getResource("PhoneEventJFX.fxml"));
								panePhone = (BorderPane) loader2.load();
								panePhone.setVisible(true);
								scenePhone = new Scene(panePhone, 600, 450);
								
								scenePhone.addEventHandler(ActionEvent.ACTION, new EventHandler<Event>() {
									@Override
									public void handle(Event event) {
										PhoneController.phoneCancelChk = true;
									}
								});
								
								stagePhone = new Stage();
								stagePhone.setScene(scenePhone);
								stagePhone.initModality(Modality.APPLICATION_MODAL);
								
								stagePhone.setTitle("Calling...");
								stagePhone.show();
								stagePhone.setResizable(false);
							} catch (Exception e) {
								e.printStackTrace();
							}
							break;
						case D:
							try {
								MainController.mediaPlayer2.setVolume(0);
								MainController.mainService.cancel();
								Camera.cameraActive=false;
								
								FXMLLoader loader2 = new FXMLLoader(getClass().getResource("DeliveryEventJFX.fxml"));
								paneDelivery = (BorderPane) loader2.load();
								paneDelivery.setVisible(true);
								sceneDelivery = new Scene(paneDelivery, 600, 450);
								
								sceneDelivery.addEventHandler(ActionEvent.ACTION, new EventHandler<Event>() {
									@Override
									public void handle(Event event) {
										DeliveryController.deliCancelChk = true;
									}
								});
								
								stageDelivery = new Stage();
								
								HBox deliveryHbox = new HBox();
								Image okBtnImg = new Image(getClass().getResourceAsStream("src/image/okay.png"));
								Button okDelivery = new Button("okay");
								okDelivery.setGraphic(new ImageView(okBtnImg));
								deliveryHbox.getChildren().add(okDelivery);
								
								paneDelivery.getChildren().add(deliveryHbox);
								
								stageDelivery.setScene(sceneDelivery);
								stageDelivery.initModality(Modality.APPLICATION_MODAL);
								
								stageDelivery.setTitle("Delivery...");
								stageDelivery.show();
								stageDelivery.setResizable(false);
								
								
							} catch (Exception e) {
								e.printStackTrace();
							}
							break;
						default:
							break;
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		drawnWindow = new Window(WIDTH, HEIGHT);
		
		launch(args);
	}

}
