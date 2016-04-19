

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import containers.*;
import models.*;
public class FFMain extends Application {

	/*Constants*/
	public static final String VERSION = "2.2";
	

	public static Parent root;
	public static Stage stage;
	
	private static FFController controller;

	@Override
	public void start(Stage primaryStage) throws IOException {
		// TODO Auto-generated method stub

		Font.loadFont(FFMain.class.getResourceAsStream("expressway.ttf"), 12);
		stage = primaryStage;
		root = FXMLLoader.load(getClass().getResource("FFLayout.fxml"));
		Scene scene = new Scene(root);
		stage.setTitle("FitzFitting SPROX Analysis v"+VERSION);
		stage.setScene(scene);
		stage.getIcons().add(new Image("/images/SPROXFitting.png"));
		stage.setResizable(false);
		stage.show();
	}

	public static void loadAndStart(AbstractFFModel model){
		/*
		 * Perform preliminary data checks, loads CSVs, and digests
		 */
		Thread modelThread = new Thread(){
			public void run(){
				//status on loading AbstractDataSet.load() and constructor
				FFError modelStatus = model.getStatus();

				if(modelStatus != FFError.NoError){
					model.writeError("Error loading model "+modelStatus);
					System.err.println("Terminating");
					controller.getProgressBar().progressProperty().unbind();
					model.terminate();
				}

				//If loaded, write message
				model.writeLoadedMessage();
				//start digestion (model.start() calls AbstractDataSet.digest())
				model.start();
				modelStatus = model.getStatus();
				if(modelStatus == FFError.NoError){ //Error calculating
					model.save();
					if(model.getGenerateGraphsStatus()){
						model.generateGraphs();
					}
					model.generateHTML();
					model.generateHistograms();
					model.writeCompleted();
				}
				else // on calculation error
				{
					model.writeError("Error calculating model "+modelStatus);
					System.err.println("Terminating");
				}
				model.terminate();
			}
		};

		modelThread.start();
	}

	public static void exit(){
		Platform.exit();
		System.exit(0);
	}

	//set FFController instance to controller
	public static void setController(FFController controller){
		FFMain.controller = controller;
	}

	//Backup
	public static void main(String[] args){
		launch(args);
	}
}
