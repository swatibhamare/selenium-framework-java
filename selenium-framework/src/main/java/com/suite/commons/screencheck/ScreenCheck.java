package com.suite.commons.screencheck;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.suite.common.reporting.ExtentReportTestFactory;
import com.web.webdriver_factory.WebDriverFactory;

public class ScreenCheck {

	private String path = "";

	private String fileName = "";

	private String fullExpectedName = "";
	private String fullActualName = "";
	private String fullIgnoreName = "";
	private String fullDifferenceName = "";
	private String fullExpectedWithDifferenceHighlitedName = "";

	private boolean isExpectedFileExists = false;
	private boolean isActualFileExists = false;
	private boolean isIgnoreFileExists = false;
	private boolean isDiffFileExists = false;
		
	static {
		System.load("E:\\Vaibhav\\Software\\opencv\\build\\java\\x64\\opencv_java411.dll");
	}

	public void checkScreen(String fileName) throws IOException {

		ExtentReportTestFactory.getTest().info("Screen check analysis starting for - "+Thread.currentThread().getStackTrace()[2].getClassName()+Thread.currentThread().getStackTrace()[2].getMethodName());

		
		
		this.fileName = fileName;// fileName+".png";
		this.fileName=new Integer((Thread.currentThread().getStackTrace()[2].getClassName()+Thread.currentThread().getStackTrace()[2].getMethodName()).hashCode()).toString();
		this.path = "E:\\ImagesToDelete\\"; //to make dynamic
		this.fullExpectedName = this.path + this.fileName + "_expected.png";
		this.fullActualName = this.path + this.fileName + "_actual.png";
		this.fullIgnoreName = this.path + this.fileName + "_ignore.png";
		this.fullDifferenceName = this.path + this.fileName + "_diff.png";
		this.fullExpectedWithDifferenceHighlitedName  = this.path + this.fileName + "_expected_diff_highlited.png";

		this.isExpectedFileExists = this.isFileExists(fullExpectedName);
		this.isIgnoreFileExists = this.isFileExists(fullIgnoreName);

		deleteFileIfExists(fullActualName);
		deleteFileIfExists(fullDifferenceName);
		deleteFileIfExists(fullExpectedWithDifferenceHighlitedName);
		
// Step 1 - Check if expected file present, if not capture it and exit function
		if (!isFileExists(fullExpectedName)) {

			takeScreenshot(fullExpectedName);
			return;
		}

// Step 2 - take screenshot for actual screen
		if(deleteFileIfExists(fullActualName))
			ExtentReportTestFactory.getTest().info("Actual file exists, deleted succssfully...");
		takeScreenshot(fullActualName);
//Step 3 - Check if there is difference between actual and expected image, if no this exist function else proceed for analysis
		if (this.getDifferencePercentage(fullExpectedName, fullActualName) == 1.0) {
			ExtentReportTestFactory.getTest().info("Images are same, no need to do analysis....");
			return;
		} else {
			ExtentReportTestFactory.getTest().info("Images are different, proceeding for more analysis....");
		}

// Step 4 - Get the difference image and write it on disk

		BufferedImage diffImage = getDifferenceImage(fullExpectedName, fullActualName);
	
		if(deleteFileIfExists(fullDifferenceName))
			ExtentReportTestFactory.getTest().info("difference file exists, deleted succssfully...");
		ImageIO.write(diffImage, "png", new File(fullDifferenceName));

// Step 5 - Check if ignore file is present, if present then check difference berween diffImage and ignore file
		if (this.isIgnoreFileExists) {
			if (this.getDifferencePercentage(fullDifferenceName, fullIgnoreName) == 1.0) {
// Step 5.1 - If ignore file present and there is no difference between ifnore file and difference file then return to function
				ExtentReportTestFactory.getTest().info("Differences Ignored... ");
				return;
			} else {
// Step 5.2 - If ignore file is present and difference is more then, create another difference file ignoring difference in difference file
				diffImage = getDifferenceImage(fullDifferenceName,fullIgnoreName );
				if(deleteFileIfExists(fullDifferenceName))
					ExtentReportTestFactory.getTest().info("difference file exists, deleted again succssfully...");
				ImageIO.write(diffImage, "png", new File(fullDifferenceName));
			}
		}

// Step 6 - Draw differences on expected image.

		Mat originalImageToRect = Imgcodecs.imread(fullExpectedName);

		drawAndGetRectedImage(originalImageToRect);

// Step 7 - Drwo expected file with difference highlited
		Imgcodecs.imwrite(fullExpectedWithDifferenceHighlitedName,originalImageToRect);

// Step 8 - Debug
		//ImShow.show(originalImageToRect);

	}

	private double getDifferencePercentage(String image1FullName, String image2FullName) {

		Mat originalGrayImage = Imgcodecs.imread(image1FullName);
		Mat modifiedGrayImage = Imgcodecs.imread(image2FullName);

		Mat hist_1 = new Mat();
		Mat hist_2 = new Mat();

		MatOfFloat ranges = new MatOfFloat(0f, 256f);
		MatOfInt histSize = new MatOfInt(25);

		Imgproc.calcHist(Arrays.asList(originalGrayImage), new MatOfInt(0), new Mat(), hist_1, histSize, ranges);
		Imgproc.calcHist(Arrays.asList(modifiedGrayImage), new MatOfInt(0), new Mat(), hist_2, histSize, ranges);

		return Imgproc.compareHist(hist_1, hist_2, Imgproc.CV_COMP_CORREL);

	}

	private BufferedImage getDifferenceImage(String image1FullName, String image2FullName) throws IOException {

		BufferedImage img1 = ImageIO.read(new File(image1FullName));
		BufferedImage img2 = ImageIO.read(new File(image2FullName));

		int width1 = img1.getWidth();
		int height1 = img1.getHeight();

		int width2 = img2.getWidth();
		int height2 = img2.getHeight();

		BufferedImage outImage = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_RGB);

		if ((width1 != width2) || (height1 != height2)) {
			ExtentReportTestFactory.getTest().info("Hight/width is not equal");
			return null;
		}

		int diff;
		int result;

		for (int i = 0; i < height1; i++) {
			for (int j = 0; j < width1; j++) {
				int rgb1 = img1.getRGB(j, i);
				int rgb2 = img2.getRGB(j, i);

				int r1 = (rgb1 >> 16) & 0xff;
				int g1 = (rgb1 >> 8) & 0xff;
				int b1 = (rgb1) & 0xff;

				int r2 = (rgb2 >> 16) & 0xff;
				int g2 = (rgb2 >> 8) & 0xff;
				int b2 = (rgb2) & 0xff;

				diff = Math.abs(r1 - r2);
				diff += Math.abs(g1 - g2);
				diff += Math.abs(b1 - b2);

				diff /= 3;

				result = (diff << 16) | (diff << 8) | (diff);

				outImage.setRGB(j, i, result);

			}
		}
		return outImage;

	}

	private List<MatOfPoint> getContours(Mat grayImage) {

		Mat hierarchy = new Mat();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

		Imgproc.findContours(grayImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		return contours;

	}

	private boolean isFileExists(String fileFullName) {

		File file = new File(fileFullName);

		return file.exists();

	}
	
	private boolean deleteFileIfExists(String fileFullName) {

		File file = new File(fileFullName);

		return file.delete();

	}

	private void drawAndGetRectedImage(Mat originalImageToRect) {
		Mat diff = Imgcodecs.imread(fullDifferenceName);
		Mat originalGrayImage = new Mat();

		Imgproc.cvtColor(diff, originalGrayImage, Imgproc.COLOR_BGR2GRAY);

		List<MatOfPoint> contours = getContours(originalGrayImage);

		for (MatOfPoint m : contours) {
			Rect r = Imgproc.boundingRect(m);
			Imgproc.rectangle(originalImageToRect, r.tl(), r.br(), new Scalar(0, 0, 255), 2);

		}

	}



	public void takeScreenshot(String fileNameWithPath) {
		File scrFile = ((TakesScreenshot) WebDriverFactory.getDriver()).getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(scrFile, new File(fileNameWithPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
