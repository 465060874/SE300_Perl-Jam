package src;

import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main application class
 * 
 * @author Taylor Hester, Matthew Caixeiro
 * @version 1.0
 */
public class RiddleRunAroundParking extends Application {

	private static ProcessingManager pm = new ProcessingManager(40);
	public static DisplayUI ui;
	public RiddleRunAroundParking() {
	}

	public void finalize() throws Throwable {
	}

	/**
	 * Main method: initializes the image processor and UI display.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {
		// imageProc.diffAsBinArray();

		pm.beginProcThread();

		launch(args);
		
		
	}

	/**
	 * Starts the UI display.
	 * 
	 * @param primaryStage
	 *            The stage containing the main application window
	 */
	public void start(Stage primaryStage) throws Exception {
		ui = new DisplayUI(pm);
		ui.start(primaryStage);
		pm.setUIRef(ui);
	}
}// end RiddleRunAroundParking