package src;

import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvAbsDiff;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.CV_GAUSSIAN;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;

/**
 * Class that manages all image processing and comparison. Handles access and
 * manipulation of frames for comparison.
 * 
 * @author Austin Musser, Kyle Cochran, Matthew Caixeiro
 * @version 1.0
 */
public class ImageProcessor {
	private IplImage lotIplImage;
	private IplImage lotIplImage_gray;
	private IplImage refPic;
	private IplImage diff;
	private Mat matDiff;
	private OpenCVFrameConverter.ToIplImage iplConverter;
	private OpenCVFrameConverter.ToMat matConverter;

	private int[][] binaryArray = new int[1440][1080]; // these values may need
														// to be change later if
														// we crop the pic
	private CameraDriver cameraDriver = new CameraDriver();
	private MatToBinary matToBinary = new MatToBinary();

	/**
	 * Constructor initializes image converter objects and loads reference
	 * image.
	 */
	public ImageProcessor() {

		// initialize necessary image converters
		iplConverter = new OpenCVFrameConverter.ToIplImage();
		matConverter = new OpenCVFrameConverter.ToMat();

		// load reference image from file as greyscale
		refPic = cvLoadImage("src/media/frame1_edited_all_empty.jpg", CV_LOAD_IMAGE_GRAYSCALE);
	}

	/**
	 * A method to unify the processing methods of this class.
	 * 
	 * @return an array of integers that represents the current lot state
	 */
	public int[] returnCurrentSpots() {
		return generateIsEmptyMatrix(diffAsBinArray(), getSpotMatrix());
	}

	/**
	 * Takes in the current lot frame as an image and compares it to the lot
	 * reference image. Converts difference to a binary array that shows which
	 * pixels match.
	 * 
	 * @return an array of integers that represent the state of each pixel
	 *         (black/white) of the lot image difference with the reference
	 *         image.
	 */
	public int[][] diffAsBinArray() {

		lotIplImage = iplConverter.convert(cameraDriver.getImage());

		// add a blur to lot image and reference image to eliminate jitter
		// effects
		cvSmooth(lotIplImage, lotIplImage, CV_GAUSSIAN, 9, 9, 2, 2);
		cvSmooth(refPic, refPic, CV_GAUSSIAN, 9, 9, 2, 2);

		// create image containers for the greyscale lot picture and the b/w
		// difference picture
		lotIplImage_gray = IplImage.create(lotIplImage.width(), lotIplImage.height(), IPL_DEPTH_8U, 1);
		diff = IplImage.create(lotIplImage.width(), lotIplImage.height(), IPL_DEPTH_8U, 1);

		// convert lot image to greyscale
		cvCvtColor(lotIplImage, lotIplImage_gray, CV_RGB2GRAY);

		// compare lot image with reference and store difference in diff
		cvAbsDiff(lotIplImage_gray, refPic, diff);

		// modify difference image to ignore some minor changes details
		cvThreshold(diff, diff, 25, 250, CV_THRESH_BINARY);

		// convert to mat object, then to custom binary array
		matDiff = matConverter.convert(iplConverter.convert(diff));
		binaryArray = matToBinary.toBinaryArray(matDiff);

		return binaryArray;

	}

	/**
	 * Cycles through binary array picture and looks for objects present in
	 * expected lot positions. If objects are present in a certain threshold
	 * percentage of the lot, the spot is marked full. This state is saved in an
	 * ordered array of spot states in the lot.
	 * 
	 * @param binaryArray
	 *            an array of integers that represents the two-tone lot
	 *            difference image
	 * @param lines
	 *            an array of ordered coordinate pairs that represent the
	 *            parking spot divisor lines
	 * @return an array that represents the state of each parking spot in a
	 *         given lot
	 */
	public int[] generateIsEmptyMatrix(int[][] binaryArray, int[][] lines) {
		int[] isEmpty = new int[28];
		int[] count = new int[28];

		for (int i = 0; i <= count.length - 1; i++) {
			count[i] = 0;
		}

		// For the first block of cars
		for (int i = 0; i <= 3; i++) {
			// Go from the lower x bound to the higher x bound
			for (int j = lines[i][0]; j <= lines[i + 1][3]; j++) {
				// And from the lower y bound to the higher y bounf
				for (int k = lines[i][2]; k <= lines[i + 1][3]; k++) {
					// And add them up into count
					count[i] += binaryArray[j][k];
				}
			}

			// If that is less than 60% of the max value for spaces, the spot is
			// empty
			if (count[i] < .6 * ((int) (Math.abs((double) (lines[i][0] - lines[i + 1][3]))
					* Math.abs((double) (lines[i][2] - lines[i][3]))))) {
				isEmpty[i] = 1;
				// If that is greater than 60% of the max value for spaces, the
				// spot is full
			} else {
				isEmpty[i] = 0;
			}
		}

		// Repeat for all other blocks of spots

		for (int i = 5; i <= 10; i++) {
			// Go from the lower x bound to the higher x bound
			for (int j = lines[i][0]; j <= lines[i + 1][3]; j++) {
				// And from the lower y bound to the higher y bounf
				for (int k = lines[i][2]; k <= lines[i + 1][3]; k++) {
					// And add them up into count
					count[i - 1] += binaryArray[j][k];
				}
			}

			// If that is less than 60% of the max value for spaces, the spot is
			// empty
			if (count[i - 1] < .6 * ((int) (Math.abs((double) (lines[i][0] - lines[i + 1][3]))
					* Math.abs((double) (lines[i][2] - lines[i][3]))))) {
				isEmpty[i - 1] = 1;
				// If that is greater than 60% of the max value for spaces, the
				// spot is full
			} else {
				isEmpty[i - 1] = 0;
			}
		}

		for (int i = 12; i <= 24; i++) {
			// Go from the lower x bound to the higher x bound
			for (int j = lines[i][0]; j <= lines[i + 1][3]; j++) {
				// And from the lower y bound to the higher y bounf
				for (int k = lines[i][2]; k <= lines[i + 1][3]; k++) {
					// And add them up into count
					count[i - 2] += binaryArray[j][k];
				}
			}

			// If that is less than 60% of the max value for spaces, the spot is
			// empty
			if (count[i - 3] < .6 * ((int) (Math.abs((double) (lines[i][0] - lines[i + 1][3]))
					* Math.abs((double) (lines[i][2] - lines[i][3]))))) {
				isEmpty[i - 2] = 1;
				// If that is greater than 60% of the max value for spaces, the
				// spot is full
			} else {
				isEmpty[i - 2] = 0;
			}
		}

		for (int i = 26; i <= 30; i++) {
			// Go from the lower x bound to the higher x bound
			for (int j = lines[i][0]; j <= lines[i + 1][3]; j++) {
				// And from the lower y bound to the higher y bounf
				for (int k = lines[i][2]; k <= lines[i + 1][3]; k++) {
					// And add them up into count
					count[i - 3] += binaryArray[j][k];
				}
			}

			// If that is less than 60% of the max value for spaces, the spot is
			// empty
			if (count[i - 3] < .6 * ((int) (Math.abs((double) (lines[i][0] - lines[i + 1][3]))
					* Math.abs((double) (lines[i][2] - lines[i][3]))))) {
				isEmpty[i - 3] = 1;
				// If that is greater than 60% of the max value for spaces, the
				// spot is full
			} else {
				isEmpty[i - 3] = 0;
			}
		}

		return isEmpty;
	}

	/**
	 * @ignore
	 */
	private boolean[] generateSpotMatrix() {
		boolean[] temp = { false, false, false };
		return temp;
	}

	/**
	 * Identify where divisor lines are in current lot view.
	 * 
	 * @return an array of coordinate pairs that represents the pixel location
	 *         of parking spots divisor lines
	 */
	public int[][] getSpotMatrix() {
		int[][] lines = new int[32][4];

		int offset = 0;

		lines[0][0] = 198;
		lines[0][1] = 224 + offset;
		lines[0][2] = 191;
		lines[0][3] = 253 + offset;

		lines[1][0] = 223;
		lines[1][1] = 225 + offset;
		lines[1][2] = 219;
		lines[1][3] = 253 + offset;

		lines[2][0] = 262;
		lines[2][1] = 228 + offset;
		lines[2][2] = 260;
		lines[2][3] = 258 + offset;

		lines[3][0] = 300;
		lines[3][1] = 231 + offset;
		lines[3][2] = 300;
		lines[3][3] = 261 + offset;

		lines[4][0] = 336;
		lines[4][1] = 231 + offset;
		lines[4][2] = 341;
		lines[4][3] = 261 + offset;

		// Grass area between these lines

		lines[5][0] = 379;
		lines[5][1] = 234 + offset;
		lines[5][2] = 388;
		lines[5][3] = 265 + offset;

		lines[6][0] = 414;
		lines[6][1] = 234 + offset;
		lines[6][2] = 426;
		lines[6][3] = 266 + offset;

		lines[7][0] = 445;
		lines[7][1] = 240 + offset;
		lines[7][2] = 460;
		lines[7][3] = 268 + offset;

		lines[8][0] = 478;
		lines[8][1] = 242 + offset;
		lines[8][2] = 495;
		lines[8][3] = 270 + offset;

		lines[9][0] = 504;
		lines[9][1] = 242 + offset;
		lines[9][2] = 525;
		lines[9][3] = 271 + offset;

		lines[10][0] = 532;
		lines[10][1] = 245 + offset;
		lines[10][2] = 558;
		lines[10][3] = 273 + offset;

		lines[11][0] = 561;
		lines[11][1] = 245 + offset;
		lines[11][2] = 591;
		lines[11][3] = 272 + offset;

		// New row

		lines[12][0] = 200;
		lines[12][1] = 275 + offset;
		lines[12][2] = 189;
		lines[12][3] = 322 + offset;

		lines[13][0] = 240;
		lines[13][1] = 278 + offset;
		lines[13][2] = 233;
		lines[13][3] = 328 + offset;

		lines[14][0] = 280;
		lines[14][1] = 279 + offset;
		lines[14][2] = 280;
		lines[14][3] = 329 + offset;

		lines[15][0] = 319;
		lines[15][1] = 282 + offset;
		lines[15][2] = 327;
		lines[15][3] = 331 + offset;

		lines[16][0] = 364;
		lines[16][1] = 283 + offset;
		lines[16][2] = 374;
		lines[16][3] = 332 + offset;

		lines[17][0] = 402;
		lines[17][1] = 285 + offset;
		lines[17][2] = 418;
		lines[17][3] = 333 + offset;

		lines[18][0] = 440;
		lines[18][1] = 286 + offset;
		lines[18][2] = 459;
		lines[18][3] = 333 + offset;

		lines[19][0] = 474;
		lines[19][1] = 286 + offset;
		lines[19][2] = 500;
		lines[19][3] = 334 + offset;

		lines[20][0] = 509;
		lines[20][1] = 289 + offset;
		lines[20][2] = 536;
		lines[20][3] = 332 + offset;

		lines[21][0] = 543;
		lines[21][1] = 290 + offset;
		lines[21][2] = 557;
		lines[21][3] = 330 + offset;

		lines[22][0] = 571;
		lines[22][1] = 292 + offset;
		lines[22][2] = 600;
		lines[22][3] = 331 + offset;

		lines[23][0] = 606;
		lines[23][1] = 290 + offset;
		lines[23][2] = 632;
		lines[23][3] = 329 + offset;

		lines[24][0] = 632;
		lines[24][1] = 294 + offset;
		lines[24][2] = 662;
		lines[24][3] = 332 + offset;

		lines[25][0] = 657;
		lines[25][1] = 290 + offset;
		lines[25][2] = 685;
		lines[25][3] = 328 + offset;

		// New line

		lines[26][0] = 118;
		lines[26][1] = 405 + offset;
		lines[26][2] = 100;
		lines[26][3] = 475 + offset;

		lines[27][0] = 173;
		lines[27][1] = 408 + offset;
		lines[27][2] = 161;
		lines[27][3] = 480 + offset;

		lines[28][0] = 228;
		lines[28][1] = 412 + offset;
		lines[28][2] = 224;
		lines[28][3] = 479 + offset;

		lines[29][0] = 283;
		lines[29][1] = 414 + offset;
		lines[29][2] = 287;
		lines[29][3] = 480 + offset;

		lines[30][0] = 342;
		lines[30][1] = 413 + offset;
		lines[30][2] = 354;
		lines[30][3] = 481 + offset;

		lines[31][0] = 396;
		lines[31][1] = 413 + offset;
		lines[31][2] = 415;
		lines[31][3] = 480 + offset;
		// End

		return lines;
	}

	/**
	 * Converts a JavaCV IPLImage object to a JavaFX WritableImage object to
	 * allow compatibility with JavaFX graphics. Code is a hybrid from the
	 * following sources:
	 * 
	 * https://blog.idrsolutions.com/2012/11/convert-bufferedimage-to-javafx-
	 * image/ http://stackoverflow.com/questions/31873704/javacv-how-to-convert-
	 * iplimage-tobufferedimage
	 * 
	 * @param src
	 *            an IPLImage from the OpenCV/JavaCV library
	 * @return a WritableImage object (child of Java Image object) from the
	 *         JavaFX library
	 */

	public static void IplImageToWritableImage(Frame framesrc) {

		Java2DFrameConverter paintConverter = new Java2DFrameConverter();
		BufferedImage bf = paintConverter.getBufferedImage(framesrc, 1);

		WritableImage wr = null;

		if (bf != null) {
			wr = new WritableImage(bf.getWidth(), bf.getHeight());
			PixelWriter pw = wr.getPixelWriter();
			for (int x = 0; x < bf.getWidth(); x++) {
				for (int y = 0; y < bf.getHeight(); y++) {
					pw.setArgb(x, y, bf.getRGB(x, y));
				}
			}
		}

		DisplayUI.pane.setBackground(
				new Background(new BackgroundImage(wr, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
						BackgroundPosition.DEFAULT, new BackgroundSize(100, 100, true, true, true, true))));
	}
}// end ImageProcessor
