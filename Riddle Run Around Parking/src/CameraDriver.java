import java.awt.Image;
import java.io.DataInputStream;


/**
 * @author Kyle, Taylor, Matt and Austin
 * @version 1.0
 * @created 18-Feb-2016 11:36:16 AM
 */
public class CameraDriver {

	private Image lotImage;
	private DataInputStream lotVideoFeed;

	public CameraDriver(){

	}

	public void finalize() throws Throwable {

	}
	private boolean acquireFeed(){
		return false;
	}

	public Image getImage(){
		return null;
	}

	public DataInputStream streamFeed(){
		return null;
	}

	private void takeImage(){

	}
}//end CameraDriver