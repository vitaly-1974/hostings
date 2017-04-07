package hosting;

import java.util.Set;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class Utility 
{
    public static WebDriver getHandleToWindow(String title, FirefoxDriver driver){

        //parentWindowHandle = WebDriverInitialize.getDriver().getWindowHandle(); // save the current window handle.
        WebDriver popup = null;
        Set<String> windowIterator = driver.getWindowHandles();
        //Set<String> windowIterator = WebDriverInitialize.getDriver().getWindowHandles();
        System.err.println("No of windows :  " + windowIterator.size());
        for (String s : windowIterator) {
          String windowHandle = s; 
          popup = driver.switchTo().window(windowHandle);
          System.out.println("Window Title : " + popup.getTitle());
          System.out.println("Window Url : " + popup.getCurrentUrl());
          if (popup.getTitle().equals(title) ){
              System.out.println("Selected Window Title : " + popup.getTitle());
              return popup;
          }

        }
                System.out.println("Window Title :" + popup.getTitle());
                System.out.println();
            return popup;
        }
}