package com.suite.commons;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import com.web.webdriver_factory.WebDriverFactory;

public class SeleniumUtils {

	public static void takeScreenshot(String fileNameWithPath) {
		File scrFile = ((TakesScreenshot) WebDriverFactory.getDriver()).getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(scrFile, new File(fileNameWithPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
