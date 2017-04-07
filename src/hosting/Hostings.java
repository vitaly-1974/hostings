package hosting;

import com.jcraft.jsch.JSch;
import static hosting.selenium.driver;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.sql.Date;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.Proxy.ProxyType;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Hostings {

    PreparedStatement pstm = null;
    Statement stm = null;
    ResultSet rs = null;
    //final static String host = "sette0.ru.oracle.com";
    final static String host = "localhost";
    //con = DriverManager.getConnection(url, user, password);
    final static String url = "jdbc:mysql://" + host + ":3306/mysql";
    final static String user = "root";
    final static String password = "atari";
    static String ldap_user = "vitaly.missing@oracle.com";
    static String ldap_password = "Vm----1974";
    static Connection con;
    ArrayList<host> hosts = new ArrayList<>();
    ArrayList<host> hosts_to_disable = new ArrayList<>();

    int processors = Runtime.getRuntime().availableProcessors();
    final int MAX_THREADS = 10; //processors - 1;
    boolean ok;

    public Hostings() {
        //this.hosts = new ArrayList<>();
        //this.hosts_to_disable = new ArrayList<>();
    }

    // Check ssh connection 
    boolean ssh_connect(String host) {
        String[] user = {"gtee", "root", "aginfra", "aurora"};
        String[] password = {"Gt33acct", "atari", "aurora", "aurora"};
        int port = 22;
        com.jcraft.jsch.Session session = null;
        JSch jsch = new JSch();
        boolean ssh = false;
        for (int i = 0; i < user.length && ssh == false; i++) {
            try {
                session = jsch.getSession(user[i], host, port);
                session.setPassword(password[i]);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect(20000);
                ssh = true;
                System.out.print("user>" + i + "." + user[i] + ">");
                break;
            } catch (Exception e) {
            } finally {
                session.disconnect();
            }
        }
        return ssh;
    }

    void Fill_Hosts_Massiv_Init(ArrayList<host> hosts, String AllHosts) throws IOException, SQLException {
        File file = new File("/tmp/hosts.exclude").getAbsoluteFile();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String excl, excl_list = "";
            while ((excl = br.readLine()) != null) {
                if (excl.contains("ru") || excl.contains("us") || excl.indexOf(".") > 0) {
                    excl_list = excl_list + " " + excl;
                    Delete_Excluded(excl);
                }
            }
            String line, host;
            line = AllHosts;
            line = line.substring(line.lastIndexOf("multiple=\"multiple\"") + 1);
            line = line.substring(line.indexOf(">") + 1);
            line = line.substring(0, line.indexOf("selected") - 8).trim();
            while (!line.isEmpty()) {
                host = line.substring(line.indexOf("<option value") + 15, line.indexOf(">") - 1);
                line = line.substring(line.indexOf("option>") + 7).trim();
                //check if it is not in exclude list
                if (!excl_list.contains(host.trim())) {
                    hosts.add(new host(host));
                } else {
                    System.out.println(" exclude:" + host);
                }
            }
        }
    }

    void Fill_Hosts_Massiv_VMSQE(ArrayList<host> hosts, String AllHosts) {
        String line, host;
        line = AllHosts;
        line = line.substring(line.lastIndexOf("multiple=\"multiple\"") + 1);
        line = line.substring(line.indexOf(">") + 1);
        line = line.substring(0, line.indexOf("selected") - 8).trim();
        while (!line.isEmpty()) {
            host = line.substring(line.indexOf("<option value") + 15, line.indexOf(">") - 1);
            line = line.substring(line.indexOf("option>") + 7).trim();
            hosts.add(new host(host));
        }
    }

    /*
    ArrayList<String> Fill_Hosts_Massiv_Init_VMSQE() throws IOException, SQLException {
        File file = new File("/tmp/vmsqepage").getAbsoluteFile();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line = "", host = "", os = "", ram;
            int i = 0, td, ours = 0, all = 0;
            boolean add;
            String log, request = "?", pinging;
            while ((line = br.readLine()) != null) {
                if (line.contains("anton.ivanov(") || line.contains("vitaly.missing(") || line.contains("leonid.mesnik(")) {
                    if (line.contains("anton.ivanov(")) {
                        request = "anton.ivanov";
                    } else {
                        if (line.contains("vitaly.missing(")) {
                            request = "vitaly.missing";
                        } else {
                            if (line.contains("leonid.mesnik(")) {
                                request = "leonid.mesnik";
                            }
                        }
                    }

                    add = true;
                    ours++;
                    host = line.substring(line.indexOf('!') + 4, line.indexOf(':'));
                    if (!host.startsWith("emb")) {
                        td = 0;
                        ram = "";
                        // scan OS
                        while ((line = br.readLine()) != null) {
                            // RAM  
                            if (line.contains("(Physical)")) {
                                ram = (line.substring(line.indexOf("-") + 1, line.indexOf("(Physical)") - 1)).trim();
                            }
                            if (line.contains("<td>")) {
                                td++;
                            }
                            if (td == 2) {
                                os = (line.trim().substring(4));
                                break;
                            }
                        }
                        // scan Filter SQE HS Nightly Testing
                        while ((line = br.readLine()) != null) {
                            if (line.contains("/host/")) {
                                line = br.readLine().trim();
                                if (line.contains("SQE;HS;")) {
                                    for (i = 0; i < hosts.size(); i++) {
                                        if (hosts.get(i).contains(host)) {
                                            add = false;
                                            System.out.println("no add=" + hosts.get(i));
                                            break;
                                        }
                                    }
                                    if (add) {
                                        all++;
                                        hosts.add(host);
                                        oss.add(os.trim());
                                        rams.add(ram);
                                        requested.add(request);
                                        if (line.contains("Performance")) {
                                            group.add("Performance");
                                        } else {
                                            if (line.contains("Build")) {
                                                group.add("Build");
                                            } else {
                                                if (line.contains("TechRefresh") || line.contains("techrefresh")) {
                                                    group.add("TechRefresh");
                                                } else {
                                                    if (line.contains("BigApps")) {
                                                        group.add("BigApps");
                                                    } else {
                                                        group.add("");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }

                }
            }
            System.out.println("Size=" + hosts.size());
            System.out.println("Started: Definition of domain");
            connect();
            stm = con.createStatement();
            String hosts_from_vmsqe_hosts = "";
            rs = stm.executeQuery("select name from vmsqe_hosts where name like '%" + hosts.get(i) + "%'");
            while (rs.next()) {
                hosts_from_vmsqe_hosts = hosts_from_vmsqe_hosts + rs.getString(1);
            }
            rs.close();
            //con.close();

            boolean is;
            for (i = 0; i < hosts.size(); i++) {
                is = false;
                if (hosts.get(i).contains("spbef13")) {
                    hosts.set(i, hosts.get(i).replace("spbef13", "vmsqe-x4170-18"));
                }
                if (hosts.get(i).contains("spbef12")) {
                    hosts.set(i, hosts.get(i).replace("spbef12", "vmsqe-x4170-17"));
                }
                if (hosts.get(i).contains("spbef10")) {
                    hosts.set(i, hosts.get(i).replace("spbef10", "vmsqe-x4170-15"));
                }
                if (hosts_from_vmsqe_hosts.contains(hosts.get(i))) {
                    is = true;
                }
                if (!is) {
                    log = RunCmd("nslookup " + hosts.get(i) + ".us.oracle.com");
                    if (log.contains("t find")) {
                        hosts.set(i, hosts.get(i) + ".ru.oracle.com");
                    } else {
                        hosts.set(i, hosts.get(i) + ".us.oracle.com");
                    }
                }
                System.out.println((i + 1) + "." + hosts.get(i) + ">" + oss.get(i) + ">" + rams.get(i));
            }
            System.out.println("Finished: Definition of domain");
            return hosts;
        }
    }
     */
    void Check_Status_Init_VMSQE() throws IOException, SQLException {
        File file = new File("/tmp/check_status").getAbsoluteFile();
        //con = DriverManager.getConnection(url, user, password);
        connect();
        stm = con.createStatement();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line = "", host;
            int i = 1;
            while ((line = br.readLine()) != null) {
                host = line.substring(line.indexOf(".") + 1).trim();
                System.out.println(i + "." + host);
                rs = stm.executeQuery("select name from vmsqe_hosts where name like '%" + host + "%'");
                if (rs.next()) {
                    hosts.add(new host(rs.getString(1)));
                }
                i++;
            }
            System.out.println("Size=" + hosts.size());
        }
        rs.close();
        stm.close();
    }

    void Fill_Hosts_Massiv_Init_DEVOPS(String[] names) throws IOException {
        File file = new File("/tmp/vmsqepage").getAbsoluteFile();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line, host, request = "";
            int i = 0;
            //String[] names = {"anton.ivanov","vitaly.missing","leonid.mesnik","sergei.kovalev","ludmila.shikhvarg","kirill.zhaldybin",
            //                  "andrey.x.nazarov","sergey.goldenberg"};
            boolean contains = false, personal = false;
            while ((line = br.readLine()) != null) {
                contains = false;
                personal = false;
                for (i = 0; i < names.length; i++) {
                    if (line.contains(names[i] + "(")) {
                        contains = true;
                        request = names[i];
                        line = line.trim();
                        if (line.contains("ntitlement")) {
                            personal = true;
                        }
                        break;
                    }
                }
                if (contains) {
                    host = line.substring(line.indexOf('!') + 4, line.indexOf(':'));
                    while ((line = br.readLine()) != null) {
                        if (line.contains("Purpose:")) {
                            line = br.readLine();
                            line = br.readLine();
                            line = br.readLine().trim();

                            if (personal) {
                                hosts.add(new host(host, "Personal entitlement", request));
                            } else {
                                hosts.add(new host(host, line, request));
                            }
                            break;
                        }
                    }
                }
            }
            System.out.println("Size=" + hosts.size());
        }
    }

    String RunCmd(String cmd) throws IOException {
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String sin, serr, sout = "";
        while ((sin = stdInput.readLine()) != null) {
            sout = sout + sin;
        }
        while ((serr = stdError.readLine()) != null) {
            sout = sout + serr;
        }
        return sout;
    }

    void Delete_Excluded(String host) throws SQLException {
        try {
            pstm = con.prepareStatement("delete from hosts where name=?");
            pstm.setString(1, host);
            pstm.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (pstm != null) {
                    pstm.close();
                }
            } catch (SQLException e) {
                System.out.println("On close: " + e.toString());
            }
        }
    }

    void test3() throws ClassNotFoundException, SQLException, InterruptedException {
        final String ldap_user = "vitaly.missing@oracle.com";
        final String ldap_password = "Vm----1974";
//Class.forName("com.mysql.jdbc.Driver");
//Connection con;
        String url = "jdbc:mysql://" + host + ":3306/mysql", uid = "root", psw = "atari";
//con = DriverManager.getConnection(url, uid, psw);
        connect();
        Statement stm;
        stm = con.createStatement();
        int i = 0;
        String h = "";
        ResultSet rs;
        System.setProperty("webdriver.firefox.marionette", "/export/geckodriver");
        FirefoxProfile ff = new FirefoxProfile();
        ff.setPreference("network.proxy.type", ProxyType.AUTODETECT.ordinal());
        ff.setPreference("browser.startup.homepage", "about:blank");
        ff.setPreference("startup.homepage_welcome_url", "about:blank");
        ff.setPreference("startup.homepage_welcome_url.additional", "about:blank");
        FirefoxDriver driver = new FirefoxDriver(ff);
        driver.get("http://devops.oraclecorp.com/host/vmsqe/detail/");
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys(ldap_user);
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys(ldap_password);
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
//outf.println("Reboot next hosts:<br>");
        String sele = "SELECT * FROM vmsqe_hosts where state=\"YELLOW\" and requested like '%" + ldap_user.substring(0, ldap_user.indexOf("@")) + "%'";
        rs = stm.executeQuery(sele);
        PreparedStatement pstm = null;
//String redirectURL = "http://localhost:8080/web1/faces/reboot.jsp";
//response.sendRedirect(redirectURL);
        while (rs.next()) {
            i++;
            if (i > 5) {
                break;
            }
            h = rs.getString(1).substring(0, rs.getString(1).indexOf("."));
            //out.print(i+"."+h+"<br>");
            driver.get("http://devops.oraclecorp.com/host/" + h + "/detail/");
            element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href,'/host/" + h + "/actions/')]")));
            element.findElement(By.xpath("//a[contains(@href,'/host/" + h + "/actions/')]"));
            element.click();
            try {
                element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href,'/host/" + h + "/reboot/modal/')]")));
                element.findElement(By.xpath("//a[contains(@href,'/host/" + h + "/reboot/modal/')]"));
                element.click();
            } catch (Exception e) {
            }
            element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(text(),'Close')]")));
            element.findElement(By.xpath("//a[contains(text(),'Close')]"));
            element.click();
            pstm = con.prepareStatement("update devops set reboot=? where name=?");
            pstm.setString(1, "Y");
            pstm.setString(2, h);
            pstm.executeUpdate();
            pstm.close();
        }
        rs.close();
        stm.close();
        driver.close();
        /*
       connect();
       stm = con.createStatement();
       rs = stm.executeQuery("select name from devops where reboot=\"Y\"");
        rs.last();
        if(!old.contains(rs.getString(1))) {
           System.out.println(rs.getString(1));
           old = rs.getString(1);
        } 
         */
        stm.close();
    }

    void UNO() throws ClassNotFoundException, SQLException, InterruptedException {
        final String ldap_user = "vitaly.missing@oracle.com";
        final String ldap_password = "Vm----1974";
        System.setProperty("webdriver.firefox.marionette", "/export/geckodriver");
        FirefoxProfile ff = new FirefoxProfile();
        ff.setPreference("network.proxy.type", ProxyType.AUTODETECT.ordinal());
        ff.setPreference("browser.startup.homepage", "about:blank");
        ff.setPreference("startup.homepage_welcome_url", "about:blank");
        ff.setPreference("startup.homepage_welcome_url.additional", "about:blank");
        FirefoxDriver driver = new FirefoxDriver(ff);
        driver.get("https://uno.oraclecorp.com/uno/hostdevices");
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys(ldap_user);
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys(ldap_password);
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
        myDynamicElement = (new WebDriverWait(driver, 50)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[contains(text(),'Actions')]")));
        connect();
        Statement stm;
        stm = con.createStatement();
        ResultSet rs;
        String sele = "SELECT name FROM vmsqe_hosts where state like '%YELLOW%' and groups not like '%Tech%' and groups not like '%Build%'";
        rs = stm.executeQuery(sele);
        PreparedStatement pstm = null;
        String host;
        int i = 0;
        //driver.manage().window().maximize();
        while (rs.next()) {
            host = rs.getString(1);
            i++;
            myDynamicElement = (new WebDriverWait(driver, 30)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(@id, 'hostQuickSearchId')]")));
            element = driver.findElement(By.xpath("//*[contains(@id, 'hostQuickSearchId')]"));
            element.sendKeys(host.substring(0, host.indexOf(".")));
            element.sendKeys(Keys.TAB);
            element.sendKeys(Keys.ENTER);
            try {
                myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[text()='" + host.substring(0, host.indexOf(".")) + "']")));
                element = driver.findElement(By.xpath("//a[text()='" + host.substring(0, host.indexOf(".")) + "']"));
                p(i + "." + element.getText());
                myDynamicElement = (new WebDriverWait(driver, 20)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[text()='Actions']")));
                element = driver.findElement(By.xpath("//button[text()='Actions']"));
                element.click();
                element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(@data-bind, 'actionsReboot')]")));
                element = driver.findElement(By.xpath("//*[contains(@data-bind, 'actionsReboot')]"));
                p("pressing " + element.getText());
                element.click();
                Thread.sleep(1000);
                element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(@data-bind, 'click:confirm_ok')]")));
                element = driver.findElement(By.xpath("//*[contains(@data-bind, 'click:confirm_ok')]"));
                p("pressing " + element.getText());
                element.click();
                Thread.sleep(1000);
                //element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(@class, 'oj-clickable-icon')]")));
                //element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(text(),'before')]")));
                //p("vah");
                //element = driver.findElement(By.xpath("//*[contains(@class, 'oj-clickable-icon')]"));
                //element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@title, 'Delete')]")));
                //element = driver.findElement(By.xpath("//a[contains(@title, 'Delete')]"));
                //p("vah");
                //element.click();
                //
                /*
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript(
                        "var elements = document.getElementsByClassName('oj-clickable-icon oj-fwk-icon oj-fwk-icon-cross');"
                        + "var evObj = document.createEvent('MouseEvents');"
                        + "evObj.initEvent( 'click', true, false );"
                        + "elements[0].dispatchEvent(evObj);"
                );
                JavascriptExecutor executor = (JavascriptExecutor) driver;
                executor.executeScript("documentgetElementsByClassName('oj-clickable-icon oj-fwk-icon oj-fwk-icon-cross').setAttribute('visibility', 'true');");
                element = driver.findElement(By.xpath("//*[@title='Delete']"));
                element.click();
                */
                //
                /*
                JavascriptExecutor executor = (JavascriptExecutor)driver;
                executor.executeScript("document.getElementById('id').setAttribute('visibility', 'true');");
                driver.findElement(By.id("id")).click(); 
                 */
 /*
                Actions build = new Actions(driver); // heare you state ActionBuider
                build.moveToElement(element).build().perform(); // Here you perform hover mouse over the needed elemnt to triger the visibility of the hidden
                WebElement m2m= driver.findElement(By.xpath(""));//the previous non visible element
                Thread.sleep(5000);
                m2m.click();
                 */
                //System.out.println(element.getLocation());
                //p("pressing " + element.getTagName() + ">" + element.isEnabled() + ">" + element.isDisplayed() + ">" + element.isSelected());
                //element.sendKeys(Keys.ENTER);
                //element.submit();
                //element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(@title, 'Delete')]")));
                //element = driver.findElement(By.xpath("//*[contains(@title, 'Delete')]"));
                //element.click();

                //WebDriver childDriver = Utility.getHandleToWindow("Delete", driver);
                //childDriver.close();
                //element = driver.findElement(By.xpath("//*[contains(@title, 'Delete')]"));
                //p(element.getText());
                //element.click();
                //Thread.sleep(1000);
            } catch (Exception e) {
                p(i + "." + host + "-> not found" + e);
                /*
                try {
                    element = driver.findElement(By.xpath("//*[contains(@href, '#')]"));
                    p("pressing " + element.getText());
                    element.click();
                } catch (Exception ex) {
                    p("close not found");
                }
                */
            }
            driver.get("https://uno.oraclecorp.com/uno/hostdevices");
            //element = driver.findElement(By.xpath("//*[contains(@id, 'hostquicksearch_id')]"));
            //element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            //element.sendKeys(Keys.DELETE);
        }
        rs.close();
        stm.close();
        driver.close();
        stm.close();
        close_connect();
    }

    static void p(String text) {
        System.out.println(text);
    }

    void Delete_Trash() throws SQLException {
        try {
            System.out.println("\nStarted: Delete_Trash");
            stm = con.createStatement();
            rs = stm.executeQuery("select name from hosts");
            int size = hosts.size();
            System.out.println(size);
            boolean delete = true;
            while (rs.next()) {
                delete = true;
                for (int i = 0; i < size; i++) {
                    if (hosts.get(i).name.contains(rs.getString(1)) || rs.getString(1).contains(hosts.get(i).name)) {
                        delete = false;
                        i = size;
                    }
                }
                if (delete == true) {
                    System.out.println("Need to delete:" + rs.getString(1));
                    Delete_Excluded(rs.getString(1));
                } else {
                    System.out.println("no need to delete:" + rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (stm != null) {
                    stm.close();
                }
            } catch (SQLException e) {
                System.out.println("On close: " + e.toString());
            }
        }
    }

    void Trash() throws SQLException {
        try {
            System.out.println("\nStarted: VMSQE -> not used ");
            //con = DriverManager.getConnection(url, user, password);
            connect();
            stm = con.createStatement();
            rs = stm.executeQuery("select name from vmsqe_hosts");
            int size = hosts.size();
            System.out.println(size);
            boolean delete = false;
            while (rs.next()) {
                delete = false;
                for (int i = 0; i < hosts.size(); i++) {
                    if (hosts.get(i).name.contains(rs.getString(1)) || rs.getString(1).contains(hosts.get(i).name)) {
                        delete = true;
                        hosts.remove(i);
                        i = size;
                    }
                }
            }
            for (int i = 0; i < hosts.size(); i++) {
                System.out.println((i + 1) + "." + hosts.get(i).name);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //if(con != null) con.close();
                if (stm != null) {
                    stm.close();
                }
            } catch (SQLException e) {
                System.out.println("On close: " + e.toString());
            }
        }
    }

    public boolean ping(String host) throws IOException, InterruptedException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder processBuilder = new ProcessBuilder("ping", isWindows ? "-n" : "-c", "1", host);
        Process proc = processBuilder.start();
        int returnVal = proc.waitFor();
        return returnVal == 0;
    }

    void CheckPING_SSH_threads() {
        int size = hosts.size();
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
        System.out.println("MAX_THREADS=" + MAX_THREADS);
        for (int i = 0; i < size; i++) {
            Runnable worker = new MyRunnable(hosts.get(i), i);
            executor.execute(worker);
        }
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {
        }
        System.out.println("\nFinished all threads");
    }

    void CheckPING_threads() {
        int size = hosts.size();
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
        System.out.println("MAX_THREADS=" + MAX_THREADS);
        for (int i = 0; i < size; i++) {
            Runnable worker = new MyRunnableVMSQE(hosts.get(i), i);
            //executor.execute(worker);
            executor.submit(worker);
        }
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {
        }
    }

    String get_attribute(String attr, String page) {
        String look = page;
        if (look.indexOf(attr) > 0) {
            look = look.substring(look.indexOf(attr) + attr.length());
            look = look.substring(look.indexOf('>') + 1).trim();
            look = look.substring(look.indexOf('>') + 1).trim();
            look = look.substring(0, look.indexOf(" ")).trim();
        } else {
            look = "-";
        }
        return look;
    }

    void Error2() throws SQLException, ClassNotFoundException, IOException {
        Connection con = null;
        try {
            //Class.forName("com.mysql.jdbc.Driver");
            connect();
            //con = DriverManager.getConnection(url, user, password);
            Statement statement = con.createStatement();
            String s = "SELECT name FROM vmsqe_hosts where ping like '%+%' and (os like '%Linux%' or os like '%Ubuntu%' or os like '%openSUSE%')";
            ResultSet rs = statement.executeQuery(s);
            HtmlUnitDriver driver[] = new HtmlUnitDriver[500];
            ExecutorService executor1 = Executors.newFixedThreadPool(1);
            int i = 0;
            String host = "", cmd = "", loc = "";
            while (rs.next()) {
                host = rs.getString(1);
                System.out.println(i + ">" + host);
                /*
            state.add(i,"");
            cpu_name.add(i,"");
            cpu_features.add(i,"");
            swap.add(i,"");
            failed_jobs.add(i,0);
            mem_tot.add(i,"");
            disk_ag_root_free.add(i,"");
            Runnable worker1 = new MyRunnable_vmsqe_attr(host,i,driver[i]);
            executor1.execute(worker1);
                 */
                i++;

            }
            executor1.shutdown();
            while (!executor1.isTerminated()) {
            }
            System.out.println("\nFinished all threads");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    void Check_Attributes_threads() throws InterruptedException, IOException {
        int size = hosts.size();
        ExecutorService executor1 = Executors.newFixedThreadPool(size);
        System.out.println("size=" + size);
        ok = false;
        //HtmlUnitDriver driver[] = new HtmlUnitDriver[size];
        for (int i = 0; i < size; i++) {
            Runnable worker1 = new MyRunnable_attr(hosts.get(i), i);
            executor1.execute(worker1);
        }
        executor1.shutdown();
        // Wait until all threads are finish
        while (!executor1.isTerminated()) {
        }
        System.out.println("\nFinished all threads");
        if (!ok) {
            System.out.println("Get attributes method failed, no host with swap is found -> force to exit");
            System.exit(-1);
        }
    }

    void Check_vmsqe_threads() throws InterruptedException, IOException {
        ExecutorService executor1 = Executors.newFixedThreadPool(MAX_THREADS);
        int size = hosts.size();
        System.out.println("size=" + size);
        ok = false;
        for (int i = 0; i < size; i++) {
            if (hosts.get(i).name.contains("spbeg01")) {
                hosts.get(i).set_name("spbeg1.ru.oracle.com");
            }
            Runnable worker1 = new MyRunnable_vmsqe_attr(hosts.get(i), i);
            executor1.execute(worker1);
        }
        executor1.shutdown();
        // Wait until all threads are finish
        while (!executor1.isTerminated()) {}
        
        System.out.println("\nLess than 21G on /export:");
        int j = 0;
        for (int i = 0; i < size; i++) {
            if (Double.parseDouble(hosts.get(i).disk_ag_root_free) / (1024 * 1024 * 1024) < 21 && !hosts.get(i).disk_ag_root_free.equals("0")) {
                System.out.println((j + 1) + "." + hosts.get(i).name + ":" + Double.parseDouble(hosts.get(i).disk_ag_root_free) / (1024 * 1024 * 1024));
                j++;
            }
        }

        System.out.println("\nNeed to add swap:");
        String swaps, mem;
        // check Linux.swap
        for (int i = 0; i < size; i++) {
            if (hosts.get(i).oss.contains("Linux") || hosts.get(i).oss.contains("Ubuntu") || hosts.get(i).oss.contains("openSUSE") || hosts.get(i).oss.contains("Solaris")) {
                swaps = hosts.get(i).swap;
                try {
                    Double.parseDouble(swaps);
                } catch (NumberFormatException e) {
                    swaps = "0";
                }
                mem = hosts.get(i).mem_tot;
                try {
                    Double.parseDouble(mem);
                } catch (NumberFormatException e) {
                    mem = "0";
                }
                if (Double.parseDouble(swaps) / (1024 * 1024 * 1024) < Double.parseDouble(mem) * 2 / (1024 * 1024 * 1024)) {
                    System.out.print(hosts.get(i).name + "> mem:");
                    System.out.print(String.format("%8.1f", Double.parseDouble(mem) / (1024 * 1024 * 1024)));
                    System.out.print("> swap:");
                    System.out.println(String.format("%8.1f", Double.parseDouble(swaps) / (1024 * 1024 * 1024)));
                }
            }
        }

        // check cygwin.version
        System.out.println("\nNeed to upgrade cygwin on:");
        for (int i = 0; i < size; i++) {
            if (hosts.get(i).oss.contains("Windows")) {
                System.out.println(hosts.get(i).name + ">> cygwin.version:" + hosts.get(i).cygw_vers);
            }
        }
    }

    void Check_vmsqe_threads2() throws InterruptedException, IOException {
        ExecutorService executor1 = Executors.newFixedThreadPool(MAX_THREADS);
        int size = hosts.size();
        System.out.println("size=" + size);
        ok = false;
        for (int i = 0; i < size; i++) {
            if (hosts.get(i).name.contains("spbeg01")) {
                hosts.get(i).name = "spbeg1.ru.oracle.com";
            }
            Runnable worker1 = new MyRunnable_vmsqe_attr(hosts.get(i), i);
            executor1.execute(worker1);
        }
        executor1.shutdown();
        // Wait until all threads are finish
        while (!executor1.isTerminated()) {}
    }

    void Trash_threads() throws InterruptedException, IOException {
        int size = hosts.size();
        ExecutorService executor1 = Executors.newFixedThreadPool(size);
        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).name.contains("spbeg1")) {
                hosts.get(i).name = "spbeg01.ru.oracle.com";
            }
            Runnable worker1 = new MyRunnable_Trash(hosts.get(i), i);
            executor1.execute(worker1);
        }
        executor1.shutdown();
        // Wait until all threads are finish
        while (!executor1.isTerminated()) {}
        int k = 0;
        for (int i = 0; i < hosts.size(); i++) {
            if (!hosts.get(i).requested.contains(".")) {
                k++;
                System.out.println(k + ". remove:" + hosts.get(i).name);
                hosts.remove(i);
                i--;
            }
        }
        System.out.println("Removed hosts: " + k);
        System.out.println("\nNew size=" + hosts.size());
    }

    void Init_VMSQE() throws FileNotFoundException, IOException, InterruptedException, Exception {
        try {
            long start_time = System.currentTimeMillis();
            connect();
            System.out.println("connect:" + (System.currentTimeMillis() - start_time));
            start_time = System.currentTimeMillis();
            //Getting All VMSQE hosts
            // devops moved to uno
            /*
            selenium get_hosts = new selenium();
            String AllHosts =  get_hosts.Get_All_VMSQE_Hosts();
            File file = new File("/tmp/vmsqepage").getAbsoluteFile(); 
            PrintWriter out = new PrintWriter(file);
            //PrintWriter out;
            out.println(AllHosts);
            out.close();
            // Fill hosts massiv
            hosts = Fill_Hosts_Massiv_Init_VMSQE();
             */

            // new method of getting all vmsqe hosts from aurora
            selenium get_hosts = new selenium();
            String AllHosts = get_hosts.Getting_Aurora_VMSQE_Hosts();
            File file = new File("/tmp/vmsqepage").getAbsoluteFile();
            PrintWriter out = new PrintWriter(file);
            out.println(AllHosts);
            out.close();
            System.out.println("selenium:" + (System.currentTimeMillis() - start_time));
            start_time = System.currentTimeMillis();
            
            // Fill hosts massiv
            hosts = new ArrayList<>();
            Fill_Hosts_Massiv_VMSQE(hosts, AllHosts);
            System.out.println("Size:" + hosts.size());
            System.out.println("Fill_Hosts_Massiv_VMSQE:" + (System.currentTimeMillis() - start_time));
            start_time = System.currentTimeMillis();

            if (hosts.size() < 100) {
                System.out.println("<100 hosts -> selenium failed to scan devops hosts");
                System.exit(0);
            }
            // Delete from vmsqe_hosts which are not in devops list
            //Delete_vmsqe();
            Trash_threads();
            System.out.println("Trash_threads:" + (System.currentTimeMillis() - start_time));
            start_time = System.currentTimeMillis();

            Delete_vmsqe_not_DEVOPS();
            System.out.println("Delete_vmsqe_not_DEVOPS:" + (System.currentTimeMillis() - start_time));
            start_time = System.currentTimeMillis();

            // Fill Location
            FillLocation_vmsqe();
            System.out.println("FillLocation_vmsqe:" + (System.currentTimeMillis() - start_time));
            start_time = System.currentTimeMillis();

            // Check ping and others attributes
            System.out.println("\nStarted: check ping");
            CheckPING_threads();
            System.out.println("\nStarted: check status, Failed jobs");
            System.out.println("CheckPING_threads:" + (System.currentTimeMillis() - start_time));
            start_time = System.currentTimeMillis();

            Check_vmsqe_threads();
            System.out.println("Finished.");
            System.out.println("Check_vmsqe_threads:" + (System.currentTimeMillis() - start_time));
            start_time = System.currentTimeMillis();

            //System.out.println("\nStarted: Delete not GTEE hosts");
            //Delete_vmsqe_not_GTEE();
            //System.out.println("Finished.");
            // Update DB
            System.out.println("\nStarted: Update DB");
            UpdateDB_vmsqe();
            System.out.println("Finished.");
            
            /*out = new PrintWriter(new File("/tmp/last_update").getAbsoluteFile());
            java.util.Date date = new java.util.Date();
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());
            System.out.println(sqlDate);
            out.println(sqlDate);
            out.close();
            */
        } catch (Exception e) {
            System.out.println(e.toString());
        } finally {
            //close_connect();
        }
    }

    void Check_Status_VMSQE() throws FileNotFoundException, IOException, InterruptedException, Exception {
        // Fill hosts massiv
        Check_Status_Init_VMSQE();
        System.out.println("\nStarted: check status");
        Check_vmsqe_threads2();
        for (int i = 0; i < hosts.size(); i++) {
            System.out.println(hosts.get(i) + " " + hosts.get(i).state);
        }
        // Update DB
        System.out.println("\nStarted: Update DB");
        //UpdateDB_vmsqe();
        System.out.println("Finished.");
    }

    void Get_DEVOPS_hosts_with_fields(int fields) throws FileNotFoundException, IOException, InterruptedException, Exception {
        String use, em_status, to_file = "";
        java.awt.datatransfer.Clipboard clipboard;
        Actions action;
        Toolkit toolkit;
        //String[] names = {"anton.ivanov","vitaly.missing","leonid.mesnik","sergei.kovalev","ludmila.shikhvarg","kirill.zhaldybin",
        //                   "andrey.x.nazarov","sergey.goldenberg","yuri.nesterenko"};        
        String[] names = {"yuri.nesterenko"};

        File file = new File("/tmp/vmsqepage").getAbsoluteFile();
        PrintWriter out = new PrintWriter(file);
        selenium get_hosts = new selenium();
        System.out.println("Collect DEVOPS hosts");
        String AllHosts = get_hosts.Get_All_VMSQE_Hosts(names);
        out.println(AllHosts);
        out.close();
        System.out.println("Collected in -> /tmp/vmsqepage");

        System.out.println("Extract hosts from /tmp/vmsqepage");
        Fill_Hosts_Massiv_Init_DEVOPS(names);

        for (int i = 0; i < hosts.size(); i++) {
            if (fields == 1) {
                //System.out.println(String.format("%-30s%-60s", (i+1)+"."+hosts.get(i),uses.get(i))); 
                to_file = to_file + String.format("%-30s", (i + 1) + "." + hosts.get(i)) + String.format("%-60s", hosts.get(i).uses) + "\n";
            }
        }
        //System.out.println(to_file); 
        System.out.print("Write results to /tmp/devops");
        if (fields == 1) {
            file = new File("/tmp/devops").getAbsoluteFile();
            out = new PrintWriter(file);
            out.println(to_file);
            out.close();
            driver.close();
            return;
        }
        // fields == 2
        System.setProperty("webdriver.firefox.marionette", "/export/geckodriver");
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("network.proxy.type", 4);
        driver = new FirefoxDriver();
        driver.get("http://devops.oraclecorp.com/user/anton.ivanov/detail/hosts/");
        // Enter Credentials
        WebElement element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys(ldap_user);
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys(ldap_password);
        element = driver.findElement(By.className("submit_btn"));
        element.submit();

        for (int i = 0; i < hosts.size(); i++) {
            System.out.print(String.format("%-30s%-60s", (i + 1) + "." + hosts.get(i).name, hosts.get(i).uses));
            to_file = to_file + String.format("%-30s%-60s", (i + 1) + "." + hosts.get(i).name, hosts.get(i).uses);
            em_status = "";
            use = "";
            driver.navigate().to("http://devops.oraclecorp.com/host/" + hosts.get(i) + "/detail/");
            driver.manage().window().maximize();

            element = (new WebDriverWait(driver, 30)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[@ng-click='host.showEM()']")));
            element = driver.findElement(By.xpath("//a[@ng-click='host.showEM()']"));
            element.click();
            WebDriverWait wait = new WebDriverWait(driver, 30);
            try {
                wait.until(new ExpectedCondition<Boolean>() {
                    public Boolean apply(WebDriver driver) {
                        WebElement button = driver.findElement(By.xpath("//*[contains(text(), 'EM Status')]"));
                        WebElement condition = driver.findElement(By.xpath("//*[contains(text(), 'There are no such host in EM')]"));
                        return (button.isDisplayed() || condition.isDisplayed());
                    }
                });
            } catch (Exception ex) {
                if (driver.findElement(By.xpath("//*[contains(text(), 'Error')]")).isDisplayed()) {
                    System.out.println("error0");
                    to_file = to_file + "error0\n";
                    continue;
                }
            }
            // Clear clipboard content
            StringSelection stringSelection = new StringSelection("");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            // Take screen shot
            action = new Actions(driver);
            action.keyDown(Keys.LEFT_CONTROL).sendKeys("a").sendKeys("c").perform();
            action.keyUp(Keys.LEFT_CONTROL).perform();
            // Copy screen shot to text
            try {
                toolkit = Toolkit.getDefaultToolkit();
                clipboard = toolkit.getSystemClipboard();
                em_status = (String) clipboard.getData(DataFlavor.stringFlavor);
            } catch (IOException ex) {
            }
            if (driver.findElement(By.xpath("//*[contains(text(), 'There are no such host in EM')]")).isDisplayed()) {
                System.out.println("There are no such host in EM");
                to_file = to_file + "There are no such host in EM\n";
                continue;
            }
            if (!em_status.contains("EM Status")) {
                System.out.println("No EM Status");
                to_file = to_file + "No EM Status\n";
                continue;
            }
            // If for some reasons status hadn't got
            em_status = em_status.substring(em_status.indexOf("EM Status") + 10).trim();
            em_status = em_status.substring(0, em_status.indexOf("\n")).trim();
            System.out.println(em_status);
            to_file = to_file + " " + em_status + "\n";
        }
        driver.close();
        file = new File("/tmp/devops").getAbsoluteFile();
        out = new PrintWriter(file);
        out.println(to_file);
        out.close();
    }

    static void getinfo(String user, String pass) throws InterruptedException, Exception {
        System.out.println(user + " " + pass);
        ldap_user = user;
        ldap_password = pass;
    }

    void EnableHosts_Threads() throws UnsupportedEncodingException, FileNotFoundException, IOException, SQLException, InterruptedException {
        File file = new File("/tmp/hosts").getAbsoluteFile();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String excl;
            int i = 0;
            while ((excl = br.readLine()) != null) {
                hosts.add(new host(excl));
            }
        }

        int size = hosts.size();
        System.out.println("VMSQE+Embedded hosts:" + size);
        for (int i = 0; i < hosts.size(); i++) {
            System.out.println(hosts.get(i).name);
        }

        selenium get_hosts = new selenium();
        String host;
        ExecutorService executor1 = Executors.newFixedThreadPool(5);
        ok = false;
        HtmlUnitDriver driver[] = new HtmlUnitDriver[size];
        for (int i = 0; i < size; i++) {
            if (hosts.get(i).name.contains("spbeg01")) {
                hosts.get(i).name = "spbeg1.ru.oracle.com";
            }
            //Runnable worker1 = new MyRunnable_vmsqe_attr(hosts.get(i), i, driver[i]);
            Runnable worker1 = new MyRunnable_vmsqe_attr(hosts.get(i), i);
            executor1.execute(worker1);
        }
        executor1.shutdown();
        // Wait until all threads are finish
        while (!executor1.isTerminated()) {
        }
        System.out.println("\nFinished all threads");

        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).state.contains("DISABLE")) {
                System.out.println(i + "." + hosts.get(i).name);
                get_hosts.EnableHost(hosts.get(i).name);
            }
        }
    }

    void DisableBadVMSQEHosts_Threads() throws UnsupportedEncodingException, FileNotFoundException, IOException, SQLException, InterruptedException {
        String select;
        try {
            //con = DriverManager.getConnection(url, user, password);
            connect();
            stm = con.createStatement();
            select = "SELECT name FROM vmsqe_hosts where state like '%ENABLED%' UNION SELECT name FROM hosts where state like '%ENABLED%'";
            rs = stm.executeQuery(select);
            while (rs.next()) {
                hosts.add(new host(rs.getString(1)));
            }
            rs.close();
        } catch (SQLException ex) {
        } finally {
            try {
                if (stm != null) {
                    stm.close();
                }
            } catch (SQLException e) {
                System.out.println("On close: " + e.toString());
            }
        }
        int size = hosts.size();
        System.out.println("VMSQE+Embedded hosts:" + size);
        selenium get_hosts = new selenium();
        String host, log = "";
        HtmlUnitDriver driver[] = new HtmlUnitDriver[size];
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
        System.out.println("MAX_THREADS=" + MAX_THREADS);
        for (int i = 0; i < hosts.size(); i++) {
            Runnable worker = new MyRunnable_vmsqe(hosts.get(i), driver[i], i, log);
            executor.execute(worker);
        }
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {
        }

        // Start to disable
        size = hosts_to_disable.size();

        if (size > 0) {
            System.out.println("\nHosts to verify:" + size);
            Long curTime = System.currentTimeMillis();
            Date curDate = new Date(curTime);
            Timestamp timestamp = new Timestamp(curDate.getTime());
            boolean exist = false;
            String body_line = "";
            ArrayList<String> body = new ArrayList<>();
            int disable_all = 0;
            for (int i = 0; i < size; i++) {
                //System.out.println(hosts_to_disable.get(i).name + "." + hosts_to_disable.get(i).failure + "." + hosts_to_disable.get(i).job_url);
                if (hosts_to_disable.get(i).failure == null) {
                    hosts_to_disable.get(i).failure = "";
                }
                if (hosts_to_disable.get(i).failure.contains("No space left on device") || hosts_to_disable.get(i).failure.contains("getting job environment is failed")) {
                    disable_all++;
                }
            }
            exist = false;
            if (disable_all < 10) {
                for (int i = 0; i < size; i++) {
                    host = hosts_to_disable.get(i).name;
                    System.out.print((i + 1) + "." + hosts_to_disable.get(i).name + ">" + hosts_to_disable.get(i).failure);
                    body_line = hosts_to_disable.get(i).name + " > " + hosts_to_disable.get(i).failure + ">" + hosts_to_disable.get(i).job_url;
                    exist = false;
                    if (hosts_to_disable.get(i).failure.contains("No space left on device") || hosts_to_disable.get(i).failure.contains("getting job environment is failed")) {
                        get_hosts.DisableHost(host, hosts.get(i).failure);
                        System.out.print(" -> Disabled");
                        body_line = body_line + " -> DISABLED";
                    } else {
                        System.out.print(" -> Need to check host");
                        body_line = body_line + " -> Need to check host";
                    }
                    body.add(body_line);

                    // TO DISABLE:
                    // 1. ERROR:AGENT-JOBENV && RUN ABORTED -> getting job environment is failed
                    // 2. No space left on device || There is not enough space on the disk
                    //if(failure.get(i).contains("No space left on device") || failure.get(i).contains("There is not enough space on the disk") || failure.get(i).contains("getAttributes")) {
                    try {
                        stm = con.createStatement();
                        select = "SELECT name,date FROM disabled where name like '%" + hosts_to_disable.get(i).name + "%' and date='" + curDate + "'";
                        rs = stm.executeQuery(select);
                        if (rs.next()) exist = true;
                        if(rs != null) rs.close();
                    } catch (SQLException ex) {
                    } finally {
                        if (stm != null) stm.close();
                    }
                    if (exist) p(" -> Exist in DB");
                    else {
                        p(" -> Insert into DB");
                        // Update DB
                        try {
                            pstm = con.prepareStatement("insert into disabled (name,failed_jobs,date,time,failure,job) values (?,?,?,?,?,?)");
                            pstm.setString(1, hosts_to_disable.get(i).name);
                            pstm.setInt(2, hosts_to_disable.get(i).failed_jobs);
                            pstm.setDate(3, curDate);
                            pstm.setTimestamp(4, timestamp);
                            pstm.setString(5, hosts_to_disable.get(i).failure);
                            pstm.setString(6, hosts_to_disable.get(i).job_url);
                            pstm.executeUpdate();
                        } catch (SQLException ex) {
                            System.out.println("On close: " + ex.toString());
                        } finally {
                            try {
                                if (pstm != null) pstm.close();
                            } catch (SQLException e) {
                                System.out.println("On close: " + e.toString());
                            }
                        }
                    }
                } // for
            } // if disable_all < 10
            close_connect();
            // Form body for Email
            body_line = "";
            SendEmail mailSender;
            int count = 0;
            for (int i = 0; i < body.size(); i++) {
                if ((body.get(i)).contains("DISABLED")) {
                    count++;
                    if (count == 1) {
                        body_line = "DISABLED:\n";
                    }
                    body_line = body_line + count + "." + body.get(i).substring(0, body.get(i).indexOf("->")) + "\n";
                }
            }
            if (count > 0) {
                if (count > 9) mailSender = new SendEmail("vitaly.missing@oracle.com", "Disabled hosts -> Too many hosts", body_line);
                else {
                      mailSender = new SendEmail("vitaly.missing@oracle.com", "Disabled hosts", body_line);
                    //mailSender = new SendEmail("leonid.mesnik@oracle.com", "Disabled hosts", body_line);
                    //mailSender = new SendEmail("sergei.kovalev@oracle.com", "Disabled hosts", body_line);
                }
            }
            count = 0;
            body_line = "";
            for (int i = 0; i < body.size(); i++) {
                if (!(body.get(i)).contains("DISABLED")) {
                    count++;
                    if (count == 1) {
                        body_line = body_line + "\nNEED TO CHECK:\n";
                    }
                    body_line = body_line + count + "." + body.get(i).substring(0, body.get(i).indexOf("->")) + "\n";
                }
            }
            // Send Email
            if (count > 0) {
                mailSender = new SendEmail("vitaly.missing@oracle.com", "Suspicious hosts", body_line);
                //mailSender = new SendEmail("leonid.mesnik@oracle.com","Suspicious hosts",body_line);
            }
        }
    }

    String FillLocationHost(String host) throws IOException {
        String loc;
        loc = RunCmd("traceroute " + host);
        if (loc.contains("santaclara")) {
            loc = "santaclara";
        } else if (loc.contains("burlington")) {
            loc = "burlington";
        } else {
            loc = "stpetersburg";
        }
        //System.out.println(host+">"+loc);
        return loc;
    }

    void UpdateDB() throws FileNotFoundException, IOException {
        Long curTime = System.currentTimeMillis();
        Date curDate = new Date(curTime);
        java.util.Date time = java.util.Calendar.getInstance().getTime();
        Timestamp timestamp = new Timestamp(curDate.getTime());
        String plocation;
        
        try {
            // put all hosts into table hosts from massiv hosts
            stm = con.createStatement();
            int size = hosts.size();
            System.out.println("size=" + size);
            String change;
            for (int i = 0; i < size; i++) {
                rs = stm.executeQuery("select * from hosts where name like '%" + hosts.get(i).name + "%'");
                // if found -> compare attributes
                if (rs.next()) {
                    // name, ping,ssh,state,swap,cpu_name,cpu_feature,failed_jobs
                    if (!rs.getString(2).trim().equals(hosts.get(i).ping.trim())
                            || !rs.getString(3).trim().equals(hosts.get(i).ssh.trim())
                            || !rs.getString(4).trim().equals(hosts.get(i).state.trim())
                            || !rs.getString(5).trim().equals(hosts.get(i).swap.trim())) {

                        change = "";
                        if (!rs.getString(2).trim().equals(hosts.get(i).ping.trim())) {
                            change = change + ">ping:" + rs.getString(2) + ":" + hosts.get(i).ping;
                        }
                        if (!rs.getString(3).trim().equals(hosts.get(i).ssh.trim())) {
                            change = change + ">ssh:" + rs.getString(3) + ":" + hosts.get(i).ssh;
                        }
                        if (!rs.getString(4).trim().equals(hosts.get(i).state.trim())) {
                            change = change + ">state:" + rs.getString(4) + ":" + hosts.get(i).state + ":";
                        }
                        if (!rs.getString(5).trim().equals(hosts.get(i).swap.trim())) {
                            change = change + ">swap:" + rs.getString(5) + ":" + hosts.get(i).swap;
                        }
                        if (rs.getInt(8) != hosts.get(i).failed_jobs) {
                            change = change + ">failed_jobs:" + rs.getInt(8) + ":" + hosts.get(i).failed_jobs;
                        }
                        change = change.replace("::", ":yellow:");
                        change = change.replace("ENABLED", "green");
                        change = change.replace("DISABLED", "disabled");
                        System.out.println(rs.getString(1) + "-> Changes:" + change + " -> updating DB");
                        pstm = con.prepareStatement("insert into changes (name,ping,date,time,ssh,state,swap,cpu_name,cpu_feature,failed_jobs) values (?,?,?,?,?,?,?,?,?,?)");
                        pstm.setString(1, rs.getString(1));
                        pstm.setString(2, rs.getString(2));
                        pstm.setDate(3, rs.getDate(9));
                        pstm.setTimestamp(4, rs.getTimestamp(10));
                        pstm.setString(5, rs.getString(3));
                        pstm.setString(6, rs.getString(4));
                        pstm.setString(7, rs.getString(5));
                        pstm.setString(8, rs.getString(6));
                        pstm.setString(9, rs.getString(7));
                        pstm.setInt(10, rs.getInt(8));
                        pstm.executeUpdate();

                        pstm = con.prepareStatement("update hosts set ping=?,date=?,time=?,ssh=?,state=?,swap=?,cpu_name=?,cpu_feature=?,failed_jobs=?,changed=?,mem_total=? where name=?");
                        pstm.setString(1, hosts.get(i).ping);
                        pstm.setDate(2, curDate);
                        pstm.setTimestamp(3, timestamp);
                        pstm.setString(4, hosts.get(i).ssh);
                        pstm.setString(5, hosts.get(i).state);
                        pstm.setString(6, hosts.get(i).swap);
                        pstm.setString(7, hosts.get(i).cpu_name);
                        pstm.setString(8, hosts.get(i).cpu_features);
                        pstm.setInt(9, hosts.get(i).failed_jobs);
                        pstm.setString(10, change);
                        pstm.setString(11, hosts.get(i).mem_tot);
                        pstm.setString(12, hosts.get(i).name);
                        pstm.executeUpdate();
                    }
                } else {
                    // add new record
                    System.out.println("Not found:" + hosts.get(i).name + " -> insert into table");
                    plocation = FillLocationHost(hosts.get(i).name);
                    pstm = con.prepareStatement("insert into hosts (name,ping,date,time,ssh,state,swap,cpu_name,cpu_feature,failed_jobs,location,changed,mem_total) values (?,?,?,?,?,?,?,?,?,?,?,?,?)");
                    pstm.setString(1, hosts.get(i).name);
                    pstm.setString(2, hosts.get(i).ping);
                    pstm.setDate(3, curDate);
                    pstm.setTimestamp(4, timestamp);
                    pstm.setString(5, hosts.get(i).ssh);
                    pstm.setString(6, hosts.get(i).state);
                    pstm.setString(7, hosts.get(i).swap);
                    pstm.setString(8, hosts.get(i).cpu_name);
                    pstm.setString(9, hosts.get(i).cpu_features);
                    pstm.setInt(10, hosts.get(i).failed_jobs);
                    pstm.setString(11, plocation);
                    pstm.setString(12, "");
                    pstm.setString(13, hosts.get(i).mem_tot);
                    pstm.executeUpdate();
                }
            }
        } catch (SQLException ex) {
        } finally {
            try {
                //if(con != null) con.close();
                if (stm != null) {
                    stm.close();
                }
                if (pstm != null) {
                    pstm.close();
                }
            } catch (SQLException e) {
                System.out.println("On close: " + e.toString());
            }
        }
    }

    void UpdateDB_vmsqe() throws FileNotFoundException, IOException {
        String plocation;
        Long curTime = System.currentTimeMillis();
        Date curDate = new Date(curTime);
        java.util.Date time = java.util.Calendar.getInstance().getTime();
        Timestamp timestamp = new Timestamp(curDate.getTime());
        java.sql.Date day_now = new java.sql.Date(new java.util.Date().getTime());
        //con = DriverManager.getConnection(url, user, password);
        int index = 0;
        try {
            // put all hosts into table hosts from massiv hosts
            stm = con.createStatement();
            int size = hosts.size();
            System.out.println("size=" + size);
            String change;

            for (int i = 0; i < size; i++) {
                index = i;
                rs = stm.executeQuery("select name,ping,failed_jobs,state,ssh,mem_total,os,cores,date,time,comment,requested,groups from vmsqe_hosts where name like '%" + hosts.get(i).name + "%'");
                // if found -> compare attributes
                if (rs.next()) {
                    if (hosts.get(i).ping == null) {
                        hosts.get(i).ping = "+";
                    }
                    if (hosts.get(i).oss == null) {
                        hosts.get(i).oss = "";
                    }
                    if (!rs.getString(2).trim().equals(hosts.get(i).ping.trim()) ||
                        !rs.getString(4).trim().equals(hosts.get(i).state.trim()) ||
                        ((rs.getString(12) == null) || (rs.getString(12) != null && !rs.getString(12).trim().equals(hosts.get(i).requested)))
                        || ((rs.getString(13) == null) || (rs.getString(13) != null && !rs.getString(13).trim().equals(hosts.get(i).group)))
                        || !rs.getString(7).trim().equals(hosts.get(i).oss.trim())) {

                        change = "";
                        if (!rs.getString(2).trim().equals(hosts.get(i).ping.trim())) {
                            change = change + ">ping:" + rs.getString(2) + ":" + hosts.get(i).ping;
                        }
                        if (!rs.getString(4).trim().equals(hosts.get(i).state.trim())) {
                            change = change + ">state:" + rs.getString(4) + ":" + hosts.get(i).state + ":";
                        }
                        if (!rs.getString(7).equals(hosts.get(i).oss.trim())) {
                            change = change + ">os:" + rs.getString(7) + ":" + hosts.get(i).oss + ":";
                        }
                        if (rs.getInt(3) != hosts.get(i).failed_jobs) {
                            change = change + ">failed_jobs:" + rs.getInt(3) + ":" + hosts.get(i).failed_jobs;
                        }

                        if ((rs.getString(12) == null)
                                || (rs.getString(12) != null && !rs.getString(12).trim().equals(hosts.get(i).requested.trim()))) {
                            change = change + ">requested" + hosts.get(i).requested;
                        }

                        if ((rs.getString(13) == null)
                                || (rs.getString(13) != null && !rs.getString(13).trim().equals(hosts.get(i).group))) {
                            change = change + ">group:" + hosts.get(i).group;
                        }

                        change = change.replace("::", ":yellow:");
                        change = change.replace("ENABLED", "green");
                        change = change.replace("DISABLED", "disabled");
                        System.out.println(rs.getString(1) + "-> Changes:" + change + " -> updating DB");

                        pstm = con.prepareStatement("update vmsqe_hosts set ping=?,date=?,time=?,state=?,failed_jobs=?,requested=?,os=?,groups=? where name=?");
                        pstm.setString(1, hosts.get(i).ping);
                        pstm.setDate(2, curDate);
                        pstm.setTimestamp(3, timestamp);
                        pstm.setString(4, hosts.get(i).state);
                        pstm.setInt(5, hosts.get(i).failed_jobs);
                        pstm.setString(6, hosts.get(i).requested);
                        pstm.setString(7, hosts.get(i).oss);
                        pstm.setString(8, hosts.get(i).group);
                        pstm.setString(9, hosts.get(i).name);
                        pstm.executeUpdate();

                        pstm = con.prepareStatement("insert into vmsqe_changes (name,ping,os,state,date,date_add) values (?,?,?,?,?,?)");
                        pstm.setString(1, rs.getString(1));
                        pstm.setString(2, rs.getString(2));
                        pstm.setString(3, rs.getString(7));
                        pstm.setString(4, rs.getString(4));
                        pstm.setDate(5, rs.getDate(9));
                        pstm.setDate(6, day_now);
                        pstm.executeUpdate();
                    }
                } else {
                    // add new record
                    System.out.println("Not found:" + hosts.get(i).name + " -> insert into table");
                    plocation = FillLocationHost(hosts.get(i).name);
                    pstm = con.prepareStatement("insert into vmsqe_hosts (name,ping,failed_jobs,state,mem_total,os,cores,date,time,requested,location,groups) values (?,?,?,?,?,?,?,?,?,?,?,?)");
                    pstm.setString(1, hosts.get(i).name);
                    pstm.setString(2, hosts.get(i).ping);
                    pstm.setInt(3, hosts.get(i).failed_jobs);
                    pstm.setString(4, hosts.get(i).state);
                    pstm.setString(5, hosts.get(i).rams);
                    pstm.setString(6, hosts.get(i).oss);
                    pstm.setInt(7, hosts.get(i).cores);
                    pstm.setDate(8, curDate);
                    pstm.setTimestamp(9, timestamp);
                    pstm.setString(10, hosts.get(i).requested);
                    pstm.setString(11, plocation);
                    pstm.setString(12, hosts.get(i).group);
                    pstm.executeUpdate();
                }
            }
        } catch (SQLException ex) {
        } catch (IndexOutOfBoundsException ex) {
            System.out.println("Size=" + hosts.size() + " Index=" + index + "->" + hosts.get(index).name);
        } finally {
            try {
                //if(con != null) con.close();
                if (stm != null) {
                    stm.close();
                }
                if (pstm != null) {
                    pstm.close();
                }
            } catch (SQLException e) {
                System.out.println("On close: " + e.toString());
            }
        }
    }

    void FillLocation_vmsqe() throws IOException, SQLException, InterruptedException {
        int j = 1;
        String locat;
        try {
            stm = con.createStatement();
            rs = stm.executeQuery("select name,location from vmsqe_hosts");
            while (rs.next()) {
                if (rs.getString(2) == null || rs.getString(2).length() == 0) {
                    locat = FillLocationHost(rs.getString(1));
                    System.out.println(j + "." + rs.getString(1) + ">" + locat);
                    pstm = con.prepareStatement("update vmsqe_hosts set location=? where name=?");
                    pstm.setString(1, locat);
                    pstm.setString(2, rs.getString(1));
                    pstm.executeUpdate();
                    j++;
                }
            }
            rs.close();
            stm.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    void connect() {
        if (con == null) {
            try {
                con = DriverManager.getConnection(url, user, password);
                System.out.println("connected to DB");
            } catch (SQLException ex) {
                System.out.println("error connecting to DB");
                Logger.getLogger(Hosting.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("already connected to DB");
        }
    }

    void close_connect() {
        if (con != null) {
            try {
                con.close();
                System.out.println("Connection to DB closed");
            } catch (SQLException ex) {
                Logger.getLogger(Hosting.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    void Delete_vmsqe() {
        System.setProperty("webdriver.firefox.marionette", "/export/geckodriver");
        driver = new FirefoxDriver();
        driver.get("http://devops.oraclecorp.com/user/anton.ivanov/detail/hosts/");
        int delete = 0;
        String host;
        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).name.contains("thunder")) {
                continue;
            }
            driver.get("http://devops.oraclecorp.com/host/api/" + hosts.get(i).name.substring(0, hosts.get(i).name.indexOf(".")) + "/data.txt");
            host = driver.getPageSource();
            if (host.contains("The Page you are trying to view does not")) {
                delete++;
                System.out.println(delete + "." + hosts.get(i) + " -> not found");
                try {
                    pstm = con.prepareStatement("delete from vmsqe_hosts where name=?");
                    pstm.setString(1, hosts.get(i).name);
                    pstm.executeUpdate();
                } catch (SQLException ex) {
                    Logger.getLogger(Hosting.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
        driver.close();
    }

    void Delete_vmsqe_not_GTEE() throws IOException, SQLException, InterruptedException {
        int i, j = 1;
        boolean bad_host_found;
        System.out.println("size=" + hosts.size());
        try {
            //con = DriverManager.getConnection(url, user, password);
            connect();
            stm = con.createStatement();
            rs = stm.executeQuery("select name from vmsqe_hosts");
            while (rs.next()) {
                bad_host_found = false;
                for (i = 0; i < hosts.size(); i++) {
                    if (rs.getString(1).equals(hosts.get(i)) && hosts.get(i).group.equals("OTHERS")) {
                        bad_host_found = true;
                        break;
                    }
                }
                if (bad_host_found) {
                    System.out.println(j + "." + rs.getString(1) + "> not GTEE groups -> delete");
                    pstm = con.prepareStatement("delete from vmsqe_hosts where name=?");
                    pstm.setString(1, rs.getString(1));
                    pstm.executeUpdate();
                    j++;
                }
            }
            rs.close();
            stm.close();
            pstm.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    void Delete_vmsqe_not_DEVOPS() throws IOException, SQLException, InterruptedException {
        try {
            //connect();
            System.setProperty("webdriver.firefox.marionette", "/export/geckodriver");
            stm = con.createStatement();
            rs = stm.executeQuery("SELECT count(*) FROM vmsqe_hosts");
            rs.next();
            System.out.println("MAX_THREADS=" + rs.getInt(1));
            ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
            
            rs = stm.executeQuery("select name from vmsqe_hosts");
            int i = 1;
            while (rs.next()) {
              Runnable worker = new MyRunnable_delete_not_reserved(rs.getString(1), i);
              executor.execute(worker);
              i++;
            }
            executor.shutdown();
            while (!executor.isTerminated()) {}
            rs.close();
            stm.close();
            //close_connect();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    void Init_Embedded() throws FileNotFoundException, IOException, InterruptedException, Exception {
        connect();
        //Getting All EMBEDDED hosts
        selenium get_hosts = new selenium();
        String AllHosts = get_hosts.Get_All_Hosts();
        // Fill hosts massiv
        hosts = new ArrayList<>();
        Fill_Hosts_Massiv_Init(hosts, AllHosts);
        // Delete hosts from not EMBEDDED group
        Delete_Trash();
        // Check ping,ssh and others attributes
        System.out.println("\nStarted: check ping,ssh");
        CheckPING_SSH_threads();

        System.out.println("\nStarted: check swap, cpu.name, cpu.features, status, Failed jobs");
        Check_Attributes_threads();
        System.out.println("Finished.");
        // Update DB
        System.out.println("\nStarted: Update DB");
        UpdateDB();
        System.out.println("Finished.");
        close_connect();
    }

    void Missed_VMSQE_group() throws IOException, SQLException, InterruptedException {
        int i;
        boolean host_found_in_aurora;
        System.out.println("size=" + hosts.size());
        try {
            //con = DriverManager.getConnection(url, user, password);
            connect();
            stm = con.createStatement();
            rs = stm.executeQuery("select name from vmsqe_hosts");
            while (rs.next()) {
                host_found_in_aurora = false;
                for (i = 0; i < hosts.size(); i++) {
                    if (rs.getString(1).equals(hosts.get(i))) {
                        host_found_in_aurora = true;
                        break;
                    }
                }
                if (!host_found_in_aurora) {
                    System.out.println(rs.getString(1) + "> missed VMSQE group");
                }
            }
            rs.close();
            stm.close();
            pstm.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        //if(con != null) {con.close();}  
    }

    // Hosts from VMSQE aurora group which missed in devops
    void Hosts_in_Aurora_and_Missed_in_devops() throws FileNotFoundException, IOException, InterruptedException, Exception {
        //Getting All Aurora hosts from group VMSQE
        selenium get_hosts = new selenium();
        String AllHosts = get_hosts.Getting_Aurora_VMSQE_Hosts();
        // Fill hosts massiv
        Fill_Hosts_Massiv_VMSQE(hosts, AllHosts);
        System.out.println("Size:" + hosts.size());
        /*
        for (int i = 0; i < hosts.size(); i++) {
            System.out.println(hosts.get(i));
        }
         */
        //Missed_VMSQE_group();
        //System.exit(0);
        //Trash();
        System.setProperty("webdriver.firefox.marionette", "/export/geckodriver");
        driver = new FirefoxDriver();
        driver.get("http://devops.oraclecorp.com/user/anton.ivanov/detail/hosts/");
        // Enter Credentials
        /*
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
         */
        int j = 0;
        for (int i = 0; i < hosts.size(); i++) {
            //driver.get("http://devops.oraclecorp.com/host/" + hosts.get(i).substring(0, hosts.get(i).indexOf(".")) + "/detail/");
            driver.get("http://devops.oraclecorp.com/host/api/" + hosts.get(i).name.substring(0, hosts.get(i).name.indexOf(".")) + "/data.txt");
            AllHosts = driver.getPageSource();
            if (AllHosts.contains("The Page you are trying to view does not")) {
                j++;
                System.out.println(j + "." + hosts.get(i) + " -> not found");
            }
        }
        driver.close();
    }

    void create_devops_table() throws IOException, SQLException {
        File file = new File("/tmp/devops2").getAbsoluteFile();
        //con = DriverManager.getConnection(url, user, password);
        connect();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line, name = "", usefield, emstatus;
            int i = 0;
            while ((line = br.readLine()) != null) {
                try {
                    i++;
                    usefield = line.substring(31, 80).trim().replaceAll("/", "_").replaceAll(" ", "_");
                    emstatus = line.substring(80).trim();
                    System.out.print(i + ">" + usefield);
                    System.out.print(">" + emstatus);
                    line = line.substring(line.indexOf(".") + 1);
                    //line.replaceAll(" Testing", "_Testing");
                    name = line.substring(0, line.indexOf(" "));
                    System.out.print(">" + name + "\n");
                    stm = con.createStatement();
                    rs = stm.executeQuery("select name from devops where name like '%" + name + "%'");
                    if (!rs.next()) {
                        pstm = con.prepareStatement("insert into devops (name,usefield,emstatus) values (?,?,?)");
                        pstm.setString(1, name);
                        pstm.setString(2, usefield);
                        pstm.setString(3, emstatus);
                        pstm.executeUpdate();
                    }
                } catch (Exception e) {
                    System.out.println("@" + name + "@");
                    e.printStackTrace();
                }
            }
        }
        //if(con != null) con.close();
        if (pstm != null) {
            pstm.close();
        }
        if (stm != null) {
            stm.close();
        }
    }

    void change_devops_table() throws IOException, SQLException {
        System.out.println("Find changes from /tmp/devops");
        File file = new File("/tmp/devops").getAbsoluteFile();
        //con = DriverManager.getConnection(url, user, password);
        connect();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line, name = "", usefield, emstatus;
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                if (line.trim().contains("error")) {
                    continue;
                }
                try {
                    usefield = line.substring(30, 90).trim().replaceAll("/", "_").replaceAll(" ", "_");
                    emstatus = line.substring(90).trim();
                    line = line.substring(line.indexOf(".") + 1);
                    name = line.substring(0, line.indexOf(" "));
                    stm = con.createStatement();
                    rs = stm.executeQuery("select name,usefield,emstatus from devops where name like '%" + name + "%'");
                    if (rs.next()) {
                        //if use changed -> update devops 
                        if (!usefield.equals(rs.getString(2)) || !emstatus.equals(rs.getString(3))) {
                            //if( !usefield.equals(rs.getString(2)) ){
                            pstm = con.prepareStatement("update devops set usefield=?,emstatus=?,changed=? where name=?");
                            pstm.setString(4, name);
                            pstm.setString(3, "y");
                            pstm.setString(2, emstatus);
                            pstm.setString(1, usefield);
                            pstm.executeUpdate();
                            i++;
                            System.out.println(i + "." + name + " -> update: " + rs.getString(2) + "->" + usefield + "->" + emstatus);
                        }
                        /*                   
                   if(!emstatus.equals(rs.getString(3))){
                     System.out.println(i+"."+name+">"+rs.getString(3)+"->"+emstatus);  
                   }
                         */
                    } else {
                        pstm = con.prepareStatement("insert into devops (name,usefield,emstatus,changed) values (?,?,?,?)");
                        pstm.setString(1, name);
                        pstm.setString(2, usefield);
                        pstm.setString(3, emstatus);
                        pstm.setString(4, "y");
                        pstm.executeUpdate();
                        i++;
                        System.out.println(i + "." + name + " -> inserted");
                    }
                    rs.close();
                } catch (Exception e) {
                    System.out.println("@" + name + "@");
                    e.printStackTrace();
                }
            }
        }
        //if(con != null) con.close();
        if (pstm != null) {
            pstm.close();
        }
    }

    void change_jira_labels() throws IOException, SQLException, InterruptedException {
        String filename = "/tmp/devops_changed";
        FileWriter fw = new FileWriter(filename, true);
        System.setProperty("webdriver.firefox.marionette", "/export/geckodriver");
        FirefoxProfile ff = new FirefoxProfile();
        ff.setPreference("network.proxy.type", ProxyType.AUTODETECT.ordinal());
        ff.setPreference("browser.startup.homepage", "about:blank");
        ff.setPreference("startup.homepage_welcome_url", "about:blank");
        ff.setPreference("startup.homepage_welcome_url.additional", "about:blank");
        FirefoxDriver driver = new FirefoxDriver(ff);
        String parentWindowId = driver.getWindowHandle();
        driver.get("https://jpgid.oracle.com/jpg/login?backUrl=https%3A%2F%2Fjava.se.oracle.com%2Finfrabugs%2Fdefault.jsp");
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("email")));
        WebElement element;
        element = driver.findElement(By.id("email"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("password"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("button"));
        element.submit();
        List<WebElement> list;
        WebElement text;
        Boolean start = true;
        driver.manage().window().maximize();
        //
        //con = DriverManager.getConnection(url, user, password);
        connect();
        stm = con.createStatement();
        rs = stm.executeQuery("select name,usefield,emstatus from devops where changed=\"y\"");
        String tag1 = "", tag2 = "", line, str, h;
        int j = 0;
        while (rs.next()) {
            j++;
            line = "";
            h = rs.getString(1);
            for (int i = 0; i < rs.getString(2).length(); i++) {
                line = line + (rs.getString(2).substring(i, i + 1)).toLowerCase();
            }
            if (line.contains("sqe;")) {
                try {
                    str = line.substring(line.indexOf("sqe") + 4);
                    tag1 = "Component:sqe:" + str.substring(0, str.indexOf(";"));
                    str = str.substring(str.indexOf(";") + 1);
                    str = str.substring(0, str.indexOf(";") + 1);
                    tag2 = str.substring(0, str.indexOf(";"));
                    hosts.add(new host(rs.getString(1)));
                    if (tag2.contains("bigapps")) {
                        tag2 = "bigapps_testing";
                    }
                    if (tag2.contains("techrefresh")) {
                        tag2 = "techrefresh-candidate";
                    }
                    if (rs.getString(3).contains("There are no")) {
                        tag2 = tag2 + " NO-EM-Agent";
                    }
                    fw.write(rs.getString(1) + " " + tag1 + " " + tag2 + "\n");
                    System.out.println(j + "." + rs.getString(1) + " " + tag1 + " " + tag2);
                } catch (Exception e) {
                    System.out.println("ERROR: " + j + "." + rs.getString(1) + " " + tag1 + " " + tag2);
                    fw.write(rs.getString(1) + " " + tag1 + " " + tag2 + "\n");
                }
                //project = INV AND text ~ "vmsqe-mac-14"
                if (j == 1) {
                    //driver.get("https://java.se.oracle.com/infrabugs/browse/INV-6613?jql=project%20%3D%20INV%20AND%20text%20~%20%22bur00bjp%22");
                    driver.get("https://java.se.oracle.com/infrabugs/secure/Dashboard.jspa");
                    start = false;
                }
                //myDynamicElement = (new WebDriverWait(driver, 20)).until(ExpectedConditions.presenceOfElementLocated(By.id("searcher-query")));
                if (j > 1) {
                    text = (new WebDriverWait(driver, 30)).until(ExpectedConditions.presenceOfElementLocated(By.id("advanced-search")));
                    text = driver.findElement(By.id("advanced-search"));
                    text.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                    text.sendKeys(Keys.DELETE);
                    text.sendKeys("project = INV AND text ~ \"" + h + "\"");
                    text.sendKeys(Keys.ENTER);
                }
                Thread.sleep(200);
                text = (new WebDriverWait(driver, 30)).until(ExpectedConditions.presenceOfElementLocated(By.className("trigger-label")));
                text = driver.findElement(By.className("trigger-label"));
                try {
                    if (text.isEnabled()) {
                        System.out.println(text.getText());
                        text.click();
                        text = (new WebDriverWait(driver, 20)).until(ExpectedConditions.presenceOfElementLocated(By.id("labels-textarea")));
                        text = driver.findElement(By.id("labels-textarea"));
                        System.out.println(text.getText());
                        WebElement textArea = driver.findElement(By.id("labels-textarea"));
                        System.out.println(textArea.getText());
                        textArea.sendKeys(Keys.BACK_SPACE);
                        textArea.sendKeys(Keys.BACK_SPACE);
                        textArea.sendKeys(Keys.BACK_SPACE);
                        textArea.sendKeys(Keys.BACK_SPACE);
                        textArea.sendKeys(tag1 + "  " + tag2 + "  ");
                        element = driver.findElement(By.className("button"));
                        element.submit();
                    } else {
                        System.out.println("not enable");
                    }
                } catch (Exception e) {
                    System.out.println("No INFRABUG for:" + h);
                    e.printStackTrace();
                    continue;
                }
            }
        }
        fw.close();
        //if(con != null) con.close();
        if (stm != null) {
            stm.close();
        }
    }

    void Jira_add_label() throws IOException, SQLException, InterruptedException {
        File file = new File("/tmp/devops");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String excl;
            int i = 0;
            while ((excl = br.readLine()) != null) {
                hosts.add(new host(excl));
                i++;
            }
        }
        System.out.println(hosts.size());
        String h, tag1, tag2, tag3, parentWindowId;

        System.setProperty("webdriver.firefox.marionette", "/export/geckodriver");
        //WebDriver driver = new FirefoxDriver();
        FirefoxProfile ff = new FirefoxProfile();
        ff.setPreference("network.proxy.type", ProxyType.AUTODETECT.ordinal());
        ff.setPreference("browser.startup.homepage", "about:blank");
        ff.setPreference("startup.homepage_welcome_url", "about:blank");
        ff.setPreference("startup.homepage_welcome_url.additional", "about:blank");
        FirefoxDriver driver = new FirefoxDriver(ff);
        parentWindowId = driver.getWindowHandle();
        driver.get("https://jpgid.oracle.com/jpg/login?backUrl=https%3A%2F%2Fjava.se.oracle.com%2Finfrabugs%2Fdefault.jsp");
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("email")));
        WebElement element;
        element = driver.findElement(By.id("email"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("password"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("button"));
        element.submit();
        List<WebElement> list;
        WebElement text;
        Boolean start = true;
        for (int i = 0; i < hosts.size(); i++) {
            h = (hosts.get(i).name).substring(0, (hosts.get(i).name).indexOf(" "));
            tag1 = (hosts.get(i).name).substring((hosts.get(i).name).indexOf(" ") + 1);
            tag1 = tag1.substring(0, tag1.indexOf(" "));
            tag2 = (hosts.get(i).name).substring(tag1.length() + h.length() + 2);
            System.out.print(i + "." + h + ">" + tag1 + " " + tag2);
            if (start) {
                driver.get("https://java.se.oracle.com/infrabugs/browse/INV-6613?jql=project%20%3D%20INV%20AND%20text%20~%20%22bur00bjp%22");
                start = false;
            }
            myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("searcher-query")));
            text = driver.findElement(By.id("searcher-query"));
            text.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            text.sendKeys(Keys.DELETE);
            text.sendKeys(h);
            text.sendKeys(Keys.ENTER);
            Thread.sleep(1500);
            try {
                myDynamicElement = driver.findElement(By.className("trigger-label"));
                myDynamicElement.click();
            } catch (Exception e) {
                System.out.println("No INFRABUG for:" + h);
                e.printStackTrace();
                continue;
            }
            Thread.sleep(1000);
            Set<String> allOpenedWindows = driver.getWindowHandles();
            if (!allOpenedWindows.isEmpty()) {
                for (String windowId : allOpenedWindows) {
                    driver.switchTo().window(windowId);
                    myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("labels-textarea")));
                    try {
                        WebElement textArea = driver.findElement(By.id("labels-textarea"));
                        System.out.println(textArea.getText());
                        textArea.sendKeys(Keys.BACK_SPACE);
                        textArea.sendKeys(Keys.BACK_SPACE);
                        textArea.sendKeys(Keys.BACK_SPACE);
                        textArea.sendKeys(Keys.BACK_SPACE);
                        textArea.sendKeys(tag1 + " " + tag2 + " ");
                        element = driver.findElement(By.className("button"));
                        element.submit();
                        break;
                    } catch (NoSuchWindowException e) {
                        e.printStackTrace();
                    }
                }
            }
            Thread.sleep(1500);
        } //for
    }

    void Jira_change_label() throws IOException, SQLException, InterruptedException {
        String h, tag1 = null, tag2 = null;
        System.setProperty("webdriver.firefox.marionette", "/export/geckodriver");
        FirefoxProfile ff = new FirefoxProfile();
        ff.setPreference("network.proxy.type", ProxyType.AUTODETECT.ordinal());
        ff.setPreference("browser.startup.homepage", "about:blank");
        ff.setPreference("startup.homepage_welcome_url", "about:blank");
        ff.setPreference("startup.homepage_welcome_url.additional", "about:blank");
        FirefoxDriver driver = new FirefoxDriver(ff);
        driver.get("https://jpgid.oracle.com/jpg/login?backUrl=https%3A%2F%2Fjava.se.oracle.com%2Finfrabugs%2Fdefault.jsp");
        WebElement myDynamicElement = (new WebDriverWait(driver, 20)).until(ExpectedConditions.presenceOfElementLocated(By.id("email")));
        WebElement element;
        element = driver.findElement(By.id("email"));
        element.sendKeys("vitaly.missing@oracle.com");
        element = driver.findElement(By.id("password"));
        element.sendKeys("Vm----1974");
        element = driver.findElement(By.className("button"));
        element.submit();
        WebElement text;
        Boolean start = true;

        //con = DriverManager.getConnection(url, user, password);
        connect();
        stm = con.createStatement();
        rs = stm.executeQuery("select name,usefield,emstatus from devops where changed=\"y\"");
        //rs = stm.executeQuery("select name,usefield,emstatus from devops");
        String line, str;
        int jj = 0;
        while (rs.next()) {
            jj++;
            if (jj < 25) {
                continue;
            }
            line = "";
            h = rs.getString(1);
            for (int i = 0; i < rs.getString(2).length(); i++) {
                line = line + (rs.getString(2).substring(i, i + 1)).toLowerCase();
            }
            try {
                str = line.substring(line.indexOf("sqe") + 4);
                tag1 = "Component:sqe:" + str.substring(0, str.indexOf(";"));
                str = str.substring(str.indexOf(";") + 1);
                str = str.substring(0, str.indexOf(";") + 1);
                tag2 = str.substring(0, str.indexOf(";"));
                hosts.add(new host(rs.getString(1)));
                if (tag2.contains("bigapps")) {
                    tag2 = "bigapps_testing";
                }
                if (tag2.contains("efresh")) {
                    tag2 = "techrefresh-candidate";
                }
                if (rs.getString(3).contains("There are no")) {
                    tag2 = tag2 + " NO-EM-Agent";
                }
                //fw.write(rs.getString(1)+" "+tag1+" "+tag2+"\n"); 
                System.out.println(jj + "." + rs.getString(1) + " " + tag1 + " " + tag2);
            } catch (Exception e) {
                System.out.println("ERROR: " + jj + "." + rs.getString(1) + " " + tag1 + " " + tag2);
                //fw.write(rs.getString(1)+" "+tag1+" "+tag2+"\n"); 
            }
            if (start) {
                driver.get("https://java.se.oracle.com/infrabugs/issues/?jql=");
                start = false;
            }
            text = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("advanced-search")));
            text = driver.findElement(By.id("advanced-search"));
            text.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            text.sendKeys(Keys.DELETE);
            text.sendKeys("project = INV AND text ~ " + h);
            text.sendKeys(Keys.TAB);
            text.sendKeys(Keys.ENTER);
            try {
                myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("summary-val")));
                while (!myDynamicElement.getText().contains(h)) {
                    Thread.sleep(500);
                    myDynamicElement = driver.findElement(By.id("summary-val"));
                }
            } catch (TimeoutException e) {
                System.out.print("->  no issue were found");
                continue;
            } catch (Exception e) {
                System.out.print("->  error");
                //No issues were found
                continue;
            }
            myDynamicElement = (new WebDriverWait(driver, 20)).until(ExpectedConditions.presenceOfElementLocated(By.className("trigger-label")));
            myDynamicElement = driver.findElement(By.className("trigger-label"));
            try {
                System.out.println(myDynamicElement.getLocation());
                myDynamicElement.click();
            } catch (Exception e) {
                System.out.println("No INFRABUG for:" + h);
                e.printStackTrace();
                continue;
            }
            myDynamicElement = (new WebDriverWait(driver, 15)).until(ExpectedConditions.presenceOfElementLocated(By.id("labels-textarea")));
            element = driver.findElement(By.id("labels-textarea"));
            Thread.sleep(300);
            try {
                WebElement textArea = driver.findElement(By.id("labels-textarea"));
                textArea = driver.findElement(By.id("labels-textarea"));
                textArea.sendKeys(Keys.BACK_SPACE);
                textArea.sendKeys(Keys.BACK_SPACE);
                textArea.sendKeys(Keys.BACK_SPACE);
                textArea.sendKeys(Keys.BACK_SPACE);
                textArea.sendKeys(tag1 + " " + tag2 + " ");
                element = driver.findElement(By.className("button"));
                element.submit();
            } catch (NoSuchWindowException e) {
                e.printStackTrace();
            }
            Thread.sleep(300);
            //element.sendKeys(Keys.ESCAPE);
            //System.out.println("Cancelled");
        } //while
    }

    void Reboot() throws IOException, SQLException, InterruptedException {
        //con = DriverManager.getConnection(url, user, password);
        connect();
        stm = con.createStatement();
        int i = 0;
        String host = "";
        System.setProperty("webdriver.firefox.marionette", "/export/geckodriver");
        FirefoxProfile ff = new FirefoxProfile();
        ff.setPreference("network.proxy.type", ProxyType.AUTODETECT.ordinal());
        ff.setPreference("browser.startup.homepage", "about:blank");
        ff.setPreference("startup.homepage_welcome_url", "about:blank");
        ff.setPreference("startup.homepage_welcome_url.additional", "about:blank");
        FirefoxDriver driver = new FirefoxDriver(ff);
        driver.get("http://devops.oraclecorp.com/host/vmsqe/detail/");
        WebElement myDynamicElement = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("sso_username")));
        WebElement element;
        element = driver.findElement(By.id("sso_username"));
        element.sendKeys(ldap_user);
        element = driver.findElement(By.id("ssopassword"));
        element.sendKeys(ldap_password);
        element = driver.findElement(By.className("submit_btn"));
        element.submit();
        //String sele = "SELECT * FROM vmsqe_hosts where (os like '%Windows%') and state=\"YELLOW\" and requested like '%" + ldap_user.substring(0,ldap_user.indexOf("@")) + "%'";
        String sele = "SELECT * FROM vmsqe_hosts where state=\"YELLOW\" and requested like '%" + ldap_user.substring(0, ldap_user.indexOf("@")) + "%'";
        System.out.println(sele);
        rs = stm.executeQuery(sele);
        /*
       while(rs.next()){
          i++;
          host = rs.getString(1).substring(0, rs.getString(1).indexOf("."));
          System.out.println(i+"."+host);
       }
         */

        while (rs.next()) {
            i++;
            host = rs.getString(1).substring(0, rs.getString(1).indexOf("."));
            System.out.print(i + "." + host);
            try {
                driver.get("http://devops.oraclecorp.com/host/" + host + "/detail/");
                element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href,'/host/" + host + "/actions/')]")));
                element.findElement(By.xpath("//a[contains(@href,'/host/" + host + "/actions/')]"));
                element.click();
                element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href,'/host/" + host + "/reboot/modal/')]")));
                element.findElement(By.xpath("//a[contains(@href,'/host/" + host + "/reboot/modal/')]"));
                element.click();
                element = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(text(),'Reboot')]")));
                element.findElement(By.xpath("//a[contains(text(),'Reboot')]"));
                element.click();
            } catch (Exception e) {
                System.out.println(" -> not rebooted. continue");
            }
            System.out.println(" -> rebooted");
        }

        rs.close();
        stm.close();
        //con.close();
        driver.close();
    }

    public static void main(String[] args) throws InterruptedException, Exception {
        Hostings instance = new Hostings();
        // For embedded group
        //instance.Init_Embedded();
        
        // For VMSQE group
        instance.Init_VMSQE();
        
        //instance.Delete_vmsqe_not_DEVOPS();
        //System.exit(0);
        //new Hostings().Hosts_in_Aurora_and_Missed_in_devops();
        //System.exit(0);
        //new Hostings().FillLocation_vmsqe();
        //new Hostings().Error2();
        //instance.DisableBadVMSQEHosts_Threads();
        //new Hostings().EnableHosts_Threads();
        //ldap_user = "";
        //new Hostings().create_devops_table();
        //new Hostings().change_jira_labels();
        //new Hostings().test3();
        //instance.UNO();
        System.exit(0);

        NewJDialog dialog = new NewJDialog(new javax.swing.JFrame(), true);
        dialog.setVisible(true);

        //new Hosting().Check_Status_VMSQE();
        new Hostings().Reboot();
        System.exit(0);

        new Hostings().Check_Status_VMSQE();
        // 1) devops hosts with field 'use' and EM status -> /tmp/devops
        // param: 2 - use && EM status; 
        //        1 - use
        //new Hosting().Get_DEVOPS_hosts_with_fields(2);
        // 2) Find changes 
        //new Hosting().change_devops_table();
        // 3) Port changes to JIRA       
        //new Hosting().Jira_change_label();
        //System.exit(0);
    }

    class MyRunnable implements Runnable {

        private final host host;
        private final int i;

        MyRunnable(host host, int i) {
            this.host = host;
            this.i = i;
        }

        @Override
        public void run() {
            host.ping = "-";
            host.ssh = "-";
            try {
                if (ping(host.name)) {
                    host.ping = "+";
                    if (ssh_connect(host.name)) {
                        host.ssh = "+";
                    }
                }
            } catch (IOException e) {
                System.out.println("exception happened");
                System.exit(-1);
            } catch (InterruptedException ex) {
                Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println(Thread.currentThread().getName() + ":" + hosts.size() + ":" + (i + 1) + " " + host.name + " ping:" + host.ping + " ssh:" + host.ssh);
        }
    }

    class MyRunnable_vmsqe implements Runnable {

        private final host host;
        int disable = 0;
        private final HtmlUnitDriver driver;
        private final int i;
        private String log;

        MyRunnable_vmsqe(host host, HtmlUnitDriver driver, int i, String log) {
            this.host = host;
            this.driver = driver;
            this.i = i;
            this.log = log;
        }

        @Override
        public void run() {
            String host_page = "";
            selenium get_hosts = new selenium();
            try {
                host_page = get_hosts.GetHost_silent(host.name, driver);
            } catch (InterruptedException ex) {
                Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (!host_page.contains("class=\"disabled-working\"") && !host_page.contains("class=\"disabled-free\"")) {
                int fail = 0;
                String look = host_page;
                String s1, job_url, url, job_page, failures;
                url = host_page;
                log = hosts.size() + ":" + i + ":" + host.name + '\n';
                if (host_page.contains("FAILED\n")) {
                    while (look.indexOf("FAILED\n") > 0) {
                        try {
                            //get url   
                            url = look.substring(0, look.indexOf("FAILED\n"));
                            if (url.substring(url.length() - 6).contains("status")) {
                                System.out.println("status:FAILED");
                                look = look.substring(look.indexOf("FAILED\n") + 6);
                                continue;
                            }
                            if (url.substring(url.length() - 12).contains("macx_swapon")) {
                                System.out.println("macx_swapon");
                                look = look.substring(look.indexOf("FAILED\n") + 6);
                                continue;
                            }
                            if (url.contains("FAILED CKSUM")) {
                                System.out.println("status:FAILED");
                                look = look.substring(look.indexOf("FAILED\n") + 6);
                                continue;
                            }
                            if (!url.contains("job_id")) {
                                System.out.println("no job_id");
                                look = look.substring(look.indexOf("FAILED\n") + 6);
                                continue;
                            }
                            s1 = url.substring(url.lastIndexOf("job_id="));
                            s1 = s1.substring(0, s1.indexOf("\""));
                            job_url = "http://aurora.ru.oracle.com/faces/Job.xhtml?" + s1;
                            look = look.substring(look.indexOf("FAILED\n") + 6);
                            fail++;
                            log = log + "-> Scan jobs url:" + job_url + "\n";
                            job_page = get_hosts.GetURL_silent(job_url, driver);
                            failures = "";
                            if (!job_page.contains("archive is not a ZIP archive") && (job_page.contains("No space left on device") || job_page.contains("There is not enough space on the disk"))) {
                                failures = "No space left on device";
                            } else if (job_page.contains("ConnectorException: Requested file")) {
                                failures = "Requested file/build is not found";
                            } else if (job_page.contains("Get job environment error") && (job_page.contains("java.nio.file.AccessDeniedException") || job_page.contains("java.io.IOException: Unable to delete file"))) {
                                failures = "getting job environment is failed";
                            } else if (job_page.contains("unexpected host/agent restart")) {
                                failures = "unexpected host/agent restart";
                            } else if (job_page.contains("com.sun.aurora.agent.AgentException: Checksum failed") && job_page.contains("is not found")) {
                                failures = "AgentException: Checksum failed";
                            } else if (job_page.contains("unexpected host/agent restart") && job_url.contains("NIGHTLY")) {
                                failures = "unexpected host/agent restart";
                            } else if (job_page.contains("pid=PID is not number; failing check left-process") && job_url.contains("NIGHTLY")) {
                                failures = "failing check left-process";
                            } else if (job_url.contains("getAttributes.get")) 
                                failures = "getAttributes failed";
                            
                            if (job_page.contains("Job failed because it stayed in scheduled state for more than 300000")) 
                                failures = failures + ">job is not started";
                            if (job_page.contains("Job failed because of idle timeout hit")) 
                                failures = failures + " timeout";
                            if (fail > 1) 
                                failures = failures + ">jobs failed:" + fail;
                            if ((failures.length() > 0 || fail > 1) && !host_page.contains("class=\"disabled-working\"")
                                    && !host_page.contains("class=\"disabled-free\"") && (!failures.contains("job is not started"))) {
                                failures = failures + (job_url.contains("NIGHTLY") ? ">NIGHTLY" : "");
                                hosts_to_disable.add(new host(host.name, fail, failures, job_url));
                                disable++;
                                look = "";
                            }
                            if (failures.length() > 0) {
                                log = log + "-> Failure:" + failures + (job_url.contains("NIGHTLY") ? ">NIGHTLY" : "") + "\n";
                            }
                        } catch (StringIndexOutOfBoundsException ex) {
                            ex.printStackTrace();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    log = log + "Failed:" + fail;
                    System.out.println(log);
                    
                } // if( failed > 0 )
            }
        }
    }

    class MyRunnableVMSQE implements Runnable {

        private final host host;
        private final int i;

        MyRunnableVMSQE(host host, int i) {
            this.host = host;
            this.i = i;
        }

        @Override
        public void run() {
            host.ping = "-";
            try {
                if (ping(host.name)) {
                    host.ping = "+";
                }
            } catch (IOException e) {
                System.out.println("exception happened");
                System.exit(-1);
            } catch (InterruptedException ex) {
                Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println(Thread.currentThread().getName() + ":" + hosts.size() + ":" + (i + 1) + " " + host.name + " ping:" + host.ping);
        }
    }

    class MyRunnable_delete_not_reserved implements Runnable {
        private final String host;
        private final int i;
        
        MyRunnable_delete_not_reserved(String host, int i) {
            this.host = host;
            this.i = i;
        }

        @Override
        public void run() {
            HtmlUnitDriver driver = new HtmlUnitDriver();
            if (host.contains("thunder")) return;
            //System.out.println(Thread.currentThread().getName() + ":" + i + ".\t" + host);
            driver.get("http://devops.oraclecorp.com/host/api/" + host.substring(0, host.indexOf(".")) + "/data.txt");
            String page = driver.getPageSource();
            if (page.contains("The Page you are trying to view does not") ||  
                page.contains("is_reserved=False") || page.contains("404 Not Found") ) {
                //System.out.println(Thread.currentThread().getName() + ":" + i + ":" + host + " -> not found");
                System.out.println(i + ":" + host + " -> not found");
                try {
                  pstm = con.prepareStatement("delete from vmsqe_hosts where name=?");
                  pstm.setString(1, host);
                  pstm.executeUpdate();
                } catch (SQLException ex) {
                  Logger.getLogger(Hostings.class.getName()).log(Level.SEVERE, null, ex);
                }
            }   
            driver.close();
            //System.out.println(Thread.currentThread().getName() + ":" + (i + 1) + " " + host);
        }
    }

    class MyRunnable_attr implements Runnable {

        private final host host;
        private final int i;
        //private final HtmlUnitDriver driver;

        MyRunnable_attr(host host, int i) {
            this.host = host;
            this.i = i;
            //this.driver = driver;
        }

        @Override
        public void run() {
            HtmlUnitDriver driver = new HtmlUnitDriver();
            selenium get_hosts = new selenium();
            String hostpage = "";
            int count;
            String look, sw, mem, free;
            try {
                hostpage = get_hosts.GetHost_silent(host.name, driver);
            } catch (InterruptedException ex) {
                Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
            }
            host.cpu_name = get_attribute("cpu.name:", hostpage);
            host.cpu_features = get_attribute("cpu.features:", hostpage);
            host.swap = get_attribute("swap.total:", hostpage);
            host.mem_tot = get_attribute("mem.total:", hostpage);
            host.disk_ag_root_free = get_attribute("disk.ag_root.free:", hostpage);
            // verify that only 1 host has a swap else force to exit job
            if (host.swap.trim().length() > 2) {
                ok = true;
            }
            if (hostpage.contains("class=\"enabled-free\"") || hostpage.contains("class=\"enabled-working\"") || hostpage.contains("class=\"enabled-free-reserved\"")
                    || ((hostpage.contains("class=\"enabled-free-not-seen\"")) && (hostpage.contains("<span class=\"enabled-free-not-seen\">false</span>")) || (hostpage.contains("<td><span class=\"enabled-free-not-seen\">false</span></td>")))) {
                host.state = "ENABLED";
            } else if (hostpage.contains("class=\"disabled-free\"")) {
                host.state = "DISABLED";
            } else if (hostpage.contains("class=\"enabled-free-not-seen\"") && !(hostpage.contains("<span class=\"enabled-free-not-seen\">false</span>"))) {
                host.state = "";
            } else {
                host.state = "ENABLED";
            }

            count = 0;
            look = hostpage;
            while (look.indexOf("FAILED") > 0) {
                look = look.substring(look.indexOf("FAILED") + 6);
                count++;
            }
            host.failed_jobs = count;
            try {
                Double.parseDouble(host.swap);
            } catch (NumberFormatException e) {
                host.swap = "0";
            }
            try {
                Double.parseDouble(host.mem_tot);
            } catch (NumberFormatException e) {
                host.mem_tot = "0";
            }
            System.out.println((i + 1) + ". " + host.name + " > " + host.cpu_name + " > " + host.cpu_features + " > swap:"
                    + String.format("%8.1f", Double.parseDouble(host.swap) / (1024 * 1024 * 1024)).trim() + " > mem:"
                    + String.format("%8.1f", Double.parseDouble(host.mem_tot) / (1024 * 1024 * 1024)).trim() + " >" + host.state
                    + ">Failed jobs:" + host.failed_jobs);
        }
    }

    class MyRunnable_Trash implements Runnable {

        private final host host;
        private final int i;
        //private final HtmlUnitDriver driver;

        MyRunnable_Trash(host host, int i) {
            this.host = host;
            this.i = i;
            //this.driver = driver;
        }

        @Override
        public void run() {
            selenium get_hosts = new selenium();
            HtmlUnitDriver driver = new HtmlUnitDriver();
            String hostpage = "", req = "";
            boolean delete = false;
            String s = "delete from vmsqe_hosts where name=?";
            try {
                hostpage = get_hosts.GetHostDEVOPS_silent(host.name.substring(0, host.name.indexOf(".")), driver);
            } catch (InterruptedException ex) {
                Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (hostpage.contains("The Page you are trying to view does not")) {
                delete = true;
                System.out.println((i + 1) + ". " + host.name + " > delete");
                host.requested = "delete";
            } else if (hostpage.contains("is_reserved=False") || !hostpage.contains("user_short")) {
                delete = true;
                System.out.println((i + 1) + ". " + host.name + " > not reserved");
                host.requested = "not requested";
            } else {
                req = hostpage.substring(hostpage.indexOf("user_short") + 11);
                req = req.substring(0, req.indexOf("\n"));
                host.requested = req;
                System.out.println((i + 1) + ". " + host.name + " > " + host.requested);
            }
            if (delete) {
                try {
                    pstm = con.prepareStatement(s);
                    pstm.setString(1, host.name);
                    pstm.executeUpdate();
                } catch (SQLException ex) {
                    Logger.getLogger(Hosting.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            driver.close();
        }
    }

    class MyRunnable_vmsqe_attr implements Runnable {

        private final host host;
        private final int i;
        private HtmlUnitDriver driver = new HtmlUnitDriver();

        //MyRunnable_vmsqe_attr(host host, int i, HtmlUnitDriver driver) {
        MyRunnable_vmsqe_attr(host host, int i) {
            this.host = host;
            this.i = i;
            this.driver = driver;
        }

        @Override
        public void run() {
            selenium get_hosts = new selenium();
            String hostpage = "", os, sw, cygw_vers_p, free;
            int count, core;
            String look;
            try {
                hostpage = get_hosts.GetHost_silent(host.name, driver);
            } catch (InterruptedException ex) {
                Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (hostpage.contains("GTEE unstable group")) {
                host.group = "GTEE.unstable";
            } else {
                if (hostpage.contains("               gtee\n")) {
                    host.group = "GTEE";
                } else {
                    if (hostpage.contains("Aurora developers, have access to internal queries and reports")) {
                        host.group = "Aurora-dev";
                    } else {
                        if (hostpage.contains("Group of machine for testing init_vmsqes fix") || hostpage.contains("Group of machine for testing LargePages fix")) {
                            host.group = "LargePage";
                        } else {
                            if (hostpage.contains("        GTEE.bigapps\n") || hostpage.contains("Large hosts for bigapps")) {
                                host.group = "GTEE.bigapps";
                            } else {
                                host.group = "OTHERS";
                            }
                        }
                    }
                }
            }

            host.state = "ENABLED";
            if (hostpage.contains("class=\"disabled-free\"") || hostpage.contains("class=\"disabled-working\"")) {
                host.state = "DISABLED";
            } else {
                if (hostpage.contains("Cannot find VHost with name")) {
                    host.state = "MISSED";
                } else {
                    if (hostpage.contains("class=\"enabled-free-not-seen\"") && hostpage.contains("hung?") && !hostpage.contains("<span class=\"enabled-free-not-seen\">false")) {
                        host.state = "YELLOW";
                    }
                }
            }
            count = 0;
            host.mem_tot = get_attribute("mem.total:", hostpage);
            if (!host.state.equals("MISSED")) {
                if (host.name.contains("mac")) {
                    os = "MacOSX" + get_attribute("os.version:", hostpage).trim();
                    host.oss = os;
                } else {
                    os = get_attribute("os.full.name:", hostpage).trim();
                    host.oss = os;
                }
            }
            host.swap = get_attribute("swap.total:", hostpage);
            host.disk_ag_root_free = get_attribute("disk.ag_root.free:", hostpage);
            // cygwin.version
            host.cygw_vers = get_attribute("kernel-version:", hostpage);
            try {
                Double.parseDouble(host.swap);
            } catch (NumberFormatException e) {
                host.swap = "0";
            }
            try {
                Double.parseDouble(host.disk_ag_root_free);
            } catch (NumberFormatException e) {
                host.disk_ag_root_free = "0";
            }
            core = 0;
            try {
                core = Integer.parseInt(get_attribute("cpu.totalCores:", hostpage));
            } catch (NumberFormatException ex) {
            }
            host.cores = core;
            look = hostpage;
            while (look.indexOf("FAILED") > 0) {
                look = look.substring(look.indexOf("FAILED") + 6);
                count++;
            }
            host.failed_jobs = count;
            System.out.println((i + 1) + ". " + host.name + ">" + host.group + ">" + host.state + ">Failed jobs:" + host.failed_jobs + "OS>" + host.oss);
        }
    }

}
