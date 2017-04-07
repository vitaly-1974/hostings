/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hosting;

/**
 *
 * @author gtee
 */
import com.thoughtworks.selenium.Wait;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class selenium {

    static WebDriver driver;
    static WebDriverWait wait;
    
    public String[] Getting_Enabled_Disabled_Hosts() throws InterruptedException {
        driver = new FirefoxDriver();
        //wait = new WebDriverWait(driver, 30);
        //Logger logger = Logger.getLogger ("");
        //logger.setLevel (Level.OFF);
        // driver = new HtmlUnitDriver();
        driver.get("http://aurora.ru.oracle.com");
        WebElement myDynamicElement = (new WebDriverWait(driver, 10))
       .until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
        //System.out.println("Login pressed");
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
        
        // get ENABLED hosts - Submit templateName=EMB_generic
        driver.get("http://aurora.ru.oracle.com//faces/Submit.xhtml?templateName=EMB_generic");
        String Enabled = driver.getPageSource();
        
        // get DISABLED hosts - Submit templateName=EMB_generic
        driver.get("http://aurora.ru.oracle.com//faces/Submit.xhtml?templateName=emb_hosts_disabled");
        String Disabled = driver.getPageSource();
        
        // get ALL ENABLED hosts - Submit templateName=EmbeddedHosts
        driver.get("http://aurora.ru.oracle.com/faces/Submit.xhtml?templateName=EmbeddedHosts");
        String AllHosts = driver.getPageSource();
        // Press CTRL+S
        //Actions actions = new Actions(driver);
        //new Actions(driver).sendKeys(Keys.chord(Keys.CONTROL, "s")).perform();
        // String AllEmbedded = driver.getPageSource();
        // Press Enter
        //element.sendKeys(Keys.RETURN);
        driver.close();
        
        return new String[]{Enabled,Disabled,AllHosts};
    }
 
    public String Getting_Enabled_Hosts() throws InterruptedException {
        driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com");
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
        WebElement myDynamicElement = (new WebDriverWait(driver, 10))
  .until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
        
        // get ENABLED hosts - Submit templateName=EMB_generic
        driver.get("http://aurora.ru.oracle.com//faces/Submit.xhtml?templateName=EMB_generic");
        String Enabled = driver.getPageSource();
        driver.close();
        return Enabled;
    }
    
    public String Get_All_Hosts() throws InterruptedException {
        driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com");
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
        WebElement myDynamicElement = (new WebDriverWait(driver, 10))
  .until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
        // get ALL ENABLED hosts - Submit templateName=EmbeddedHosts
        driver.get("http://aurora.ru.oracle.com/faces/Submit.xhtml?templateName=EmbeddedHosts");
        String AllHosts = driver.getPageSource();
        driver.close();
        return AllHosts;
    }
 
    public void DisableHost(String host) throws InterruptedException {
        driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com");
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
        WebElement myDynamicElement = (new WebDriverWait(driver, 10))
  .until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();

        driver.get("http://aurora.ru.oracle.com/faces/Host.xhtml?host="+host);
        myDynamicElement = (new WebDriverWait(driver, 10))
  .until(ExpectedConditions.presenceOfElementLocated(By.id("form:description")));
        element = driver.findElement(By.id("form:description"));
        element.sendKeys("Disabled by robot: Failed nightly jobs");
        //driver.findElement(By.name("form:j_idt232"));
        driver.findElement(By.name("form:j_idt232")).click();
        driver.close();
    }

    public String GetHost_silent(String host) throws InterruptedException {
        Logger logger = Logger.getLogger ("");
        logger.setLevel (Level.OFF);        
        driver = new HtmlUnitDriver();
        driver.get("http://aurora.ru.oracle.com/faces/Host.xhtml?host="+host);
        String host_page = driver.getPageSource();
        driver.close();
        return host_page;
    }

    public String Find_id() throws InterruptedException {
        String info = "";
        Logger logger = Logger.getLogger ("");
        logger.setLevel (Level.OFF);
        driver = new HtmlUnitDriver();
        
        wait = new WebDriverWait(driver, 30);
        
        driver.get("http://aurora.ru.oracle.com");
        //driver.get("http://aurora.ru.oracle.com//faces/Submit.xhtml?templateName=EMB_generic");
        
        info = driver.getPageSource();
        //Thread.sleep(5000);
        if  (driver.getPageSource().contains("Click to login")) {
            System.out.println("login");
            driver.findElement(By.cssSelector("a[title*='Click to']")).click();
            Thread.sleep(5000);
            info = driver.getPageSource();
            WebElement element;
            element = driver.findElement(By.name("ssousername"));
            element.sendKeys("vitaly.missing@oracle.com");
            element = driver.findElement(By.name("password"));
            element.sendKeys("Vm----1974");
            element = driver.findElement(By.name("p_submit_url"));
            element.submit();
            driver.get("http://aurora.ru.oracle.com//faces/Submit.xhtml?templateName=EMB_generic");
            info = driver.getPageSource();
        } else {
            System.out.println("no sso_username");
        }
        driver.close();
        return info;
    }

    public String Getting_Host_Info() throws InterruptedException {
        driver = new HtmlUnitDriver();
        driver.get("http://aurora.ru.oracle.com/faces/Host.xhtml?host=emb-spb-raspberrypi-14.ru.oracle.com");
        String info = driver.getPageSource();
        driver.close();
        // Press CTRL+S
        //Actions actions = new Actions(driver);
        //new Actions(driver).sendKeys(Keys.chord(Keys.CONTROL, "s")).perform();
        return info;
    }

}
