package hosting;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class selenium {
    static WebDriver driver;
    static WebDriverWait wait;
    //static FirefoxProfile profile1 = new FirefoxProfile();

    String[] Getting_Enabled_Disabled_Hosts() throws InterruptedException {
        System.setProperty("webdriver.firefox.marionette","/export/geckodriver");
        driver = new FirefoxDriver();
        //wait = new WebDriverWait(driver, 30);
        //Logger logger = Logger.getLogger ("");
        //logger.setLevel (Level.OFF);
        // driver = new HtmlUnitDriver();
        driver.get("http://aurora.ru.oracle.com");
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
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
 
    String Getting_Aurora_VMSQE_Hosts() throws InterruptedException {
        System.setProperty("webdriver.firefox.marionette","/export/geckodriver");
        driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com");
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
        Thread.sleep(1000);
        // get ALL ENABLED hosts - Submit templateName=VMSQE_hosts
        driver.get("http://aurora.ru.oracle.com/faces/Submit.xhtml");
        driver.findElement(By.xpath("//*[contains(text(), 'VMSQE_hosts')]")).click();
        String AllHosts = driver.getPageSource();
        driver.close();
        return AllHosts;
    }

    String Getting_Enabled_Hosts() throws InterruptedException {
        System.setProperty("webdriver.firefox.marionette","/export/geckodriver");
        driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com");
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
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
    
    public String Getting_Enabled_Hosts_GTEE() throws InterruptedException {
        System.setProperty("webdriver.firefox.marionette","/export/geckodriver");
        driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com");
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
        
        // get ENABLED hosts - Submit templateName=EMB_generic
        driver.get("http://aurora.ru.oracle.com//faces/Submit.xhtml?templateName=AllHosts");
        String Enabled = driver.getPageSource();
        driver.close();
        return Enabled;
    }
    
    String Get_All_Hosts() throws InterruptedException {
        System.setProperty("webdriver.firefox.marionette","/export/geckodriver");
        driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com");
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
        Thread.sleep(1000);
        // get ALL ENABLED hosts - Submit templateName=EmbeddedHosts
        driver.get("http://aurora.ru.oracle.com/faces/Submit.xhtml");
        driver.findElement(By.xpath("//*[contains(text(), 'EmbeddedHosts')]")).click();
        String AllHosts = driver.getPageSource();
        driver.close();
        return AllHosts;
    }
    
    String Get_All_VMSQE_Hosts() throws InterruptedException {
        System.setProperty("webdriver.firefox.marionette","/export/geckodriver");
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("network.proxy.type", 4);
        profile.setPreference("browser.startup.homepage", "about:blank");
        profile.setPreference("startup.homepage_welcome_url", "about:blank");
        profile.setPreference("startup.homepage_welcome_url.additional", "about:blank");
        driver = new FirefoxDriver(profile);
        //driver = new FirefoxDriver();
        //wait = new WebDriverWait(driver, 50);
        driver.get("http://devops.oraclecorp.com/user/vitaly.missing/detail/hosts/");
        // Enter Credentials
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
        // get ALL ENABLED hosts - Submit templateName=EmbeddedHosts
        //driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
        String AllHosts;
        try {
        AllHosts = driver.getPageSource();
        } catch (Exception e) {AllHosts = driver.getPageSource();}
        AllHosts += getPage("http://devops.oraclecorp.com/user/anton.ivanov/detail/hosts/");
        AllHosts += getPage("http://devops.oraclecorp.com/user/leonid.mesnik/detail/hosts/");
        AllHosts += getPage("http://devops.oraclecorp.com/user/sergei.kovalev/detail/hosts/");
        AllHosts += getPage("http://devops.oraclecorp.com/user/ludmila.shikhvarg/detail/hosts/");
        AllHosts += getPage("http://devops.oraclecorp.com/user/kirill.zhaldybin/detail/hosts/");
        AllHosts += getPage("http://devops.oraclecorp.com/user/andrey.x.nazarov/detail/hosts/");
        AllHosts += getPage("http://devops.oraclecorp.com/user/sergey.goldenberg/detail/hosts/");
        AllHosts += getPage("http://devops.oraclecorp.com/user/denis.kononenko/detail/hosts/");
        driver.close();
        return AllHosts;
    }
 
    String getPage(String url){
        try {
            driver.get(url);
            return driver.getPageSource();
        } catch (UnhandledAlertException e) {
            System.out.println("warning catched");
            return driver.getPageSource();
        }
    }
    
    String Get_All_VMSQE_Hosts(String[] names) throws InterruptedException {
        System.setProperty("webdriver.firefox.marionette","/export/geckodriver");
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("network.proxy.type", 4);
        profile.setPreference("browser.startup.homepage", "about:blank");
        profile.setPreference("startup.homepage_welcome_url", "about:blank");
        profile.setPreference("startup.homepage_welcome_url.additional", "about:blank");
        driver = new FirefoxDriver(profile);
        driver.get("http://devops.oraclecorp.com/user/"+names[0]+"/detail/hosts/");
        // Enter Credentials
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
        String AllHosts = driver.getPageSource();
        for(int i = 1; i < names.length; i++){
            driver.get("http://devops.oraclecorp.com/user/"+names[i]+"/detail/hosts/");
            AllHosts = AllHosts + driver.getPageSource();
        }
        //driver.close();
        return AllHosts;
    }
    
    void DisableHost(String host,String failure) throws InterruptedException {
        System.setProperty("webdriver.firefox.marionette","/export/geckodriver");
        driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com");
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();

        driver.get("http://aurora.ru.oracle.com/faces/Host.xhtml?host="+host);
        myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("form:description")));
        element = driver.findElement(By.id("form:description"));
        element.sendKeys("Disabled by robot:"+failure);
        driver.findElement(By.name("form:j_idt241")).click();
        driver.close();
    }

    public void EnableHost(String host) throws InterruptedException {
        System.setProperty("webdriver.firefox.marionette","/export/geckodriver");
        driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com");
        // Enter Credentials
        driver.findElement(By.cssSelector("a[title*='Click to']")).click();
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();

        driver.get("http://aurora.ru.oracle.com/faces/Host.xhtml?host="+host);
        myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("form:description")));
        element = driver.findElement(By.id("form:description"));
        driver.findElement(By.name("form:j_idt242")).click();
        driver.close();
    }

    
    String GetURL_silent(String url) throws InterruptedException {
        Logger logger = Logger.getLogger ("");
        logger.setLevel (Level.OFF);        
        driver = new HtmlUnitDriver();
        driver.get(url);
        String host_page = driver.getPageSource();
        driver.close();
        return host_page;
    }

    String GetURL_silent(String url,HtmlUnitDriver driver) throws InterruptedException {
        Logger logger = Logger.getLogger ("");
        logger.setLevel (Level.OFF);        
        driver = new HtmlUnitDriver();
        driver.get(url);
        String host_page = driver.getPageSource();
        driver.close();
        return host_page;
    }
        
    public static String GetHost_silent(String host) throws InterruptedException {
        Logger logger = Logger.getLogger ("");
        logger.setLevel (Level.OFF);        
        driver = new HtmlUnitDriver();
        //System.setProperty("webdriver.firefox.marionette","/export/geckodriver");
        //driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com/faces/Host.xhtml?host="+host);
        String host_page = driver.getPageSource();
        driver.close();
        return host_page;
    }
    
    public String GetHost_silent(String host, HtmlUnitDriver driver) throws InterruptedException {
        Logger logger = Logger.getLogger ("");
        logger.setLevel (Level.OFF);        
        String host_page="";
        try {
          //driver = new HtmlUnitDriver();  
          //driver = new FirefoxDriver();
          driver.get("http://aurora.ru.oracle.com/faces/Host.xhtml?host="+host);
          host_page = driver.getPageSource();
        }  catch (Exception e) {
           System.out.println("Timeout"+e);
        }
        
        driver.close();
        return host_page;
    }
    
    String GetHostDEVOPS_silent(String host,HtmlUnitDriver driver) throws InterruptedException {
        Logger logger = Logger.getLogger ("");
        logger.setLevel (Level.OFF);        
        String host_page="";
        try {
          driver = new HtmlUnitDriver();  
          String s = "http://devops.oraclecorp.com/host/api/" + host + "/data.txt";
          //System.out.println(s);
          driver.get(s);
          host_page = driver.getPageSource();
          //System.out.println(host_page);
        }  catch (Exception e) {
           System.out.println("Timeout" + e);
        }
        if(driver != null) driver.close();
        return host_page;
    }
    
    String Find_id() throws InterruptedException {
        String info;
        Logger logger = Logger.getLogger ("");
        logger.setLevel (Level.OFF);
        driver = new HtmlUnitDriver();
        wait = new WebDriverWait(driver, 30);
        driver.get("http://aurora.ru.oracle.com");
        info = driver.getPageSource();
        //Thread.sleep(5000);
        if  (driver.getPageSource().contains("Click to login")) {
            System.out.println("login");
            driver.findElement(By.cssSelector("a[title*='Click to']")).click();
            Thread.sleep(5000);
            //info = driver.getPageSource();
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
        //driver = new HtmlUnitDriver();
        driver = new FirefoxDriver();
        driver.get("http://aurora.ru.oracle.com/faces/Host.xhtml?host=emb-spb-raspberrypi-14.ru.oracle.com");
        String info = driver.getPageSource();
        driver.close();
        // Press CTRL+S
        //Actions actions = new Actions(driver);
        //new Actions(driver).sendKeys(Keys.chord(Keys.CONTROL, "s")).perform();
        return info;
    }

}