package hosting;

import javax.swing.JFormattedTextField.AbstractFormatter;
import com.jcraft.jsch.JSch;
import static hosting.selenium.driver;
import java.io.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import java.util.Scanner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


public class Host {

    PreparedStatement pstm = null;
    Statement stm  = null;
    Connection con = null;
    ResultSet rs = null;
    //final String host = "sette0.ru.oracle.com";
    final String host = "localhost";
    final String url = "jdbc:mysql://"+host+":3306/mysql";
    final String user = "root";
    final String password = "atari";
    ArrayList<String> hosts,ping,ssh,swap,mem_tot,cpu_name,cpu_features,location,state, oss,rams,hosts1,comment,requested,group;
    ArrayList<Integer> failed_jobs,cores; 
    // for auto disabled hosts 
    ArrayList<String> hosts_to_disable = new ArrayList<>(),
                      failure = new ArrayList<>(),
                      jobs = new ArrayList<>();
      
    int processors = Runtime.getRuntime().availableProcessors();
    final int MAX_THREADS = processors;

    boolean ok;  
    
    public Host() {
        this.ssh = new ArrayList<>();
        this.state = new ArrayList<>();
        this.location = new ArrayList<>();
        this.cpu_name = new ArrayList<>();
        this.mem_tot = new ArrayList<>();
        this.swap = new ArrayList<>();
        this.ping = new ArrayList<>();
        this.hosts = new ArrayList<>();
        this.hosts1 = new ArrayList<>();
        this.cpu_features = new ArrayList<>();
        this.oss = new ArrayList<>();
        this.rams = new ArrayList<>();
        this.cores = new ArrayList<>();
        this.comment = new ArrayList<>();
        this.requested = new ArrayList<>();
        this.group = new ArrayList<>();
        this.failed_jobs = new ArrayList<>(); 
    }
    
    // Check ssh connection 
    public boolean ssh_connect(String host) {
      String user = "gtee";
      String password = "Gt33acct";
      int port = 22;
      com.jcraft.jsch.Session session = null;
      JSch jsch = new JSch();
      try {
        session = jsch.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(20000);
        return true;
      } catch(Exception e) {
        System.err.print(e);
        if(e.toString().contains("Auth fail")){
           try {
             session = jsch.getSession("root", host, port);
             session.setPassword("atari");
             session.setConfig("StrictHostKeyChecking", "no");
             session.connect(20000);
             return true;
           } catch(Exception e1) {
             System.err.print(e1);  
           }
        }
        return false;
       }  
       finally {
          session.disconnect();
       }
    }
                           
    String RunCmd(String cmd) throws IOException {
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String sin,serr,sout = "";
        while ((sin = stdInput.readLine()) != null) sout = sout + sin;
        while ((serr = stdError.readLine()) != null) sout = sout + "-err:"+serr;
        return sout;
    }    
   
    ArrayList<String> Fill_Hosts_Massiv_Init(ArrayList<String> hosts,String AllHosts) throws IOException, SQLException {
        File file = new File( "/home/hudson/hosts.exclude");
        try (BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"))) {
            String excl, excl_list = "";
            while ((excl = br.readLine()) != null) {
                   if(excl.contains("ru") || excl.contains("us") || excl.indexOf(".")>0 ){
                     excl_list = excl_list + " "+ excl; 
                     Delete_Excluded(excl);
                   }
            }   
            String line, host;
            line = AllHosts;
            line = line.substring(line.lastIndexOf("multiple=\"multiple\"") + 1);
            line = line.substring(line.indexOf(">")+1);
            line = line.substring(0,line.indexOf("selected") - 8).trim();
            while ( !line.isEmpty() ) {
                host = line.substring( line.indexOf("<option value")+15, line.indexOf(">")-1 );
                line = line.substring(line.indexOf("option>")+7).trim();
                //check if it is not in exclude list
                if (!excl_list.contains(host.trim())) {
                    hosts.add( host );
                } else {
                    System.out.println(" exclude:"+host);
                }
            }
        }
        return hosts;
    }    
    
    ArrayList<String> Fill_Hosts_Massiv_Init_VMSQE() throws IOException, SQLException {
        File file = new File( "/tmp/vmsqepage");
        try (BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"))) {
            String line = "", host = "" , os = "", ram;
            int i = 0, td, ours = 0, all = 0;
            boolean add;
            String log,request="?";
            while ((line = br.readLine()) != null) {
                //anton.ivanov leonid.mesnik evgeny.yavits vitaly.missing
                if(line.contains("evgeny.yavits(") || 
                   line.contains("anton.ivanov(")  ||
                   line.contains("vitaly.missing(")  ||
                   line.contains("leonid.mesnik(")) {
                   
                   if(line.contains("evgeny.yavits(")) request = "evgeny.yavits";else
                   if(line.contains("anton.ivanov("))  request = "anton.ivanov";else
                   if(line.contains("vitaly.missing("))request = "vitaly.missing";else
                   if(line.contains("leonid.mesnik(")) request = "leonid.mesnik";

                   add = true;
                   ours++;
                   host = line.substring(line.indexOf('!')+4, line.indexOf(':'));
                   if(!host.startsWith("emb")) {
                      td = 0;
                      ram = "";
                      // scan OS
                      while ((line = br.readLine()) != null) {
                        // RAM  
                        if(line.contains("(Physical)")) {
                           ram = (line.substring(line.indexOf("-")+1,line.indexOf("(Physical)")-1)).trim();                 
                        }
                        if(line.contains("<td>")) td++;
                        if(td == 2){
                           os =  (line.trim().substring(4));
                           break;
                        }
                      }
                      // scan Filter QE HS Nightly Testing
                      while ((line = br.readLine()) != null) {
                        if(line.contains("/host/")) {
                            line = br.readLine().trim();
                            if( (line.contains("QE") && line.contains("HS")) || (line.contains("Emvbedded")) || host.startsWith("vmsqe-mac")){
                              for(i=0;i < hosts.size();i++){
                                 if(hosts.get(i).contains(host)) {
                                    add = false; 
                                    break;
                                 } 
                              }
                              if(add) {
                                all++;  
                                hosts.add(host);
                                oss.add(os.trim());
                                rams.add(ram);
                                requested.add(request);
                              }
                            } 
                            break;
                        }
                      }
                   }
                   
                }
            }       
            System.out.println("size="+hosts.size());
            System.out.println("Started: Definition of domain");
            for(i = 0; i < hosts.size(); i++) {
                if(hosts.get(i).contains("spbef13")) hosts.set(i,hosts.get(i).replace("spbef13","vmsqe-x4170-18"));
                if(hosts.get(i).contains("spbef12")) hosts.set(i,hosts.get(i).replace("spbef12","vmsqe-x4170-17"));
                if(hosts.get(i).contains("spbef10")) hosts.set(i,hosts.get(i).replace("spbef10","vmsqe-x4170-15"));
                log = RunCmd("nslookup "+hosts.get(i) + ".us.oracle.com");        
                if(log.contains("t find")) {
                   hosts.set(i,hosts.get(i) + ".ru.oracle.com");
                } else 
                   hosts.set(i,hosts.get(i) + ".us.oracle.com");
                System.out.println((i+1)+"."+hosts.get(i)+">"+oss.get(i)+">"+rams.get(i));  
            }
            System.out.println("Finished: Definition of domain");
            return hosts;
        }
    }    
    
    void Delete_Excluded(String host) throws SQLException {
          try {
            con = DriverManager.getConnection(url, user, password);
            pstm = con.prepareStatement("delete from hosts where name=?");
            pstm.setString(1, host);
            pstm.executeUpdate();
          } catch (SQLException ex) {
            ex.printStackTrace();      
      } 
      finally{
               try {
                    if(con != null) con.close();
                    if(pstm != null) pstm.close();
               } catch (SQLException e) {
                   System.out.println("On close: " + e.toString());
               }
      }
    }
    
    void Delete_Trash() throws SQLException {
          try {
            System.out.println("\nStarted: Delete_Trash");
            con = DriverManager.getConnection(url, user, password);
            stm = con.createStatement();
            rs = stm.executeQuery("select name from hosts");
            int size = hosts.size();
            //size = 10;
            boolean delete = true;
            while(rs.next()) {
             for (int i = 0; i < size; i++) { 
                  if(hosts.get(i).equals(rs.getString(1))) delete = false;
                  i=size;
              }
              if(delete) {
                 System.out.println("Need to delete:"+rs.getString(1)); 
                 Delete_Excluded(rs.getString(1));
              } 
            }         
          } catch (SQLException ex) {
          } 
          finally{
               try {
                    if(con != null) con.close();
                    if(stm != null) stm.close();
               } catch (SQLException e) {
                   System.out.println("On close: " + e.toString());
               }
          }
    }
    
    public boolean ping(String host) throws IOException, InterruptedException {
      boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
      ProcessBuilder processBuilder = new ProcessBuilder("ping", isWindows? "-n" : "-c", "1", host);
      Process proc = processBuilder.start();
      int returnVal = proc.waitFor();
      return returnVal == 0;
    }
    
    public void CheckPING_SSH(ArrayList<String> hosts) throws UnsupportedEncodingException, IOException, Exception {
      int size = hosts.size();
      String s;
      for (int i = 0; i < size; i++) { 
          System.out.print(size + ":" + (i+1) + " " + hosts.get(i));
          ping.add("-");
          ssh.add("-");
          try {
            if(ping(hosts.get(i))) {
               ping.set(i,"+");
            } else ping.set(i,"-"); 
            if (ping.get(i).equals("+")) {
               if(ssh_connect(hosts.get(i))) {
                  ssh.set(i,"+"); 
               } else  ssh.set(i,"-"); 
            }
          } catch (IOException e) {
            System.out.println("exception happened");
            System.exit(-1);
          }
          System.out.println(" ping:"+ping.get(i)+" ssh:"+ssh.get(i));          
      } //End For hosts
    }
    
    public void CheckPING_SSH_threads(){
      int size = hosts.size();
      String s;
      ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
      System.out.println("MAX_THREADS="+MAX_THREADS);
      for (int i = 0; i < size; i++) { 
          Runnable worker = new MyRunnable(hosts.get(i),i);
          executor.execute(worker);
      }
      executor.shutdown();
      // Wait until all threads are finish
      while (!executor.isTerminated()) {}
      System.out.println("\nFinished all threads");
    }        
    
    public void Check2() throws IOException{
      String list="";
      int i = 0; 
      try {
       con = DriverManager.getConnection(url, user, password);
       stm = con.createStatement();
       //rs = stm.executeQuery("SELECT name FROM vmsqe_hosts where ping='+' and (os like '%Solaris%' or os like '%SunOS%' or os like '%Oracle.1%')");
       rs = stm.executeQuery("SELECT name FROM vmsqe_hosts where ping='+' and (os like '%Solaris%') and state='YELLOW'");
       while(rs.next()){
           //if(list.contains(rs.getString(1))) {
            System.out.println(i+"."+rs.getString(1)); 
            scp_host(rs.getString(1));
            i++;
           //}
       }    
      } catch (SQLException ex) {} 
      finally{
               try {
                    if(con != null) con.close();
                    if(stm != null) stm.close();
                    if(pstm != null) pstm.close();
               } catch (SQLException e) {
                   System.out.println("On close: " + e.toString());
               }
      }
    }
    
    public void CheckSSH(){
      ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
      System.out.println("MAX_THREADS="+MAX_THREADS);
      int i = 0; 
      try {
       con = DriverManager.getConnection(url, user, password);
       stm = con.createStatement();
       rs = stm.executeQuery("select name from vmsqe_hosts where state like '%MISSED%'");
       while(rs.next()){
           ssh.add("-");
           if(ssh_connect(rs.getString(1))) {
              ssh.set(i,"+"); 
           } else  ssh.set(i,"-"); 
           System.out.println(rs.getString(1)+" ssh:"+ssh.get(i));  
           pstm = con.prepareStatement("update vmsqe_hosts set ssh=? where name=?");
           pstm.setString(1,ssh.get(i));
           pstm.setString(2,rs.getString(1));
           pstm.executeUpdate();
           i++;
       }
      }
      catch (SQLException ex) {
      } 
      finally{
               try {
                    if(con != null) con.close();
                    if(stm != null) stm.close();
                    if(pstm != null) pstm.close();
               } catch (SQLException e) {
                   System.out.println("On close: " + e.toString());
               }
      }
      executor.shutdown();
      // Wait until all threads are finish
      while (!executor.isTerminated()) {}
    }        
        
    public void CheckPING_threads(){
      int size = hosts.size();
      String s;
      ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
      System.out.println("MAX_THREADS="+MAX_THREADS);
      for (int i = 0; i < size; i++) { 
          Runnable worker = new MyRunnableVMSQE(hosts.get(i),i);
          executor.execute(worker);
      }
      executor.shutdown();
      // Wait until all threads are finish
      while (!executor.isTerminated()) {}
    }        
    
    public String get_attribute(String attr, String page){
        String look = page;
        if(look.indexOf(attr)>0){ 
           look = look.substring(look.indexOf(attr)+attr.length());
           look = look.substring(look.indexOf('>')+1).trim();
           look = look.substring(look.indexOf('>')+1).trim();
           look = look.substring(0,look.indexOf(" ")).trim();
        } else {
           look = "-";    
        }  
       return look;
    }
    
    
    void Check_Attributes_threads() throws InterruptedException, IOException {
        ExecutorService executor1 = Executors.newFixedThreadPool(5);
        int size = hosts.size();
        System.out.println("size="+size);
        ok = false;
        HtmlUnitDriver driver[] = new HtmlUnitDriver[size];
        for (int i = 0; i < size; i++) {
            state.add(i,"");
            cpu_name.add(i,"");
            cpu_features.add(i,"");
            swap.add(i,"");
            failed_jobs.add(i,0);
            mem_tot.add(i,"");
            Runnable worker1 = new MyRunnable_attr(hosts.get(i),i,driver[i]);
            executor1.execute(worker1);
        }
        executor1.shutdown();
        // Wait until all threads are finish
        while (!executor1.isTerminated()) {
        }
        System.out.println("\nFinished all threads"); 
        if(!ok){
            System.out.println("Get attributes method failed, no host with swap is found -> force to exit");
            System.exit(-1);
        } 
    }
    
    void Check_vmsqe_threads() throws InterruptedException, IOException {
        ExecutorService executor1 = Executors.newFixedThreadPool(5);
        int size = hosts.size();
        System.out.println("size="+size);
        ok = false;
        HtmlUnitDriver driver[] = new HtmlUnitDriver[size];
        for (int i = 0; i < size; i++) {
            state.add(i,"");
            mem_tot.add(i,"");
            cores.add(i,0);
            failed_jobs.add(i,0);
            group.add(i,"");
            Runnable worker1 = new MyRunnable_vmsqe_attr( hosts.get(i).contains("spbeg01") ? "spbeg1.ru.oracle.com" : hosts.get(i),i,driver[i]);
            executor1.execute(worker1);
        }
        executor1.shutdown();
        // Wait until all threads are finish
        while (!executor1.isTerminated()) {}
        System.out.println("\nFinished all threads"); 
    }
    
    void Init_Embedded() throws FileNotFoundException, IOException, InterruptedException, Exception {
      //Getting All EMBEDDED hosts
      selenium get_hosts = new selenium();
      String AllHosts =  get_hosts.Get_All_Hosts();
       
      // Fill hosts massiv
      hosts = Fill_Hosts_Massiv_Init(hosts,AllHosts);
      
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
    }

    void Init_VMSQE() throws FileNotFoundException, IOException, InterruptedException, Exception {
      //Getting All VMSQE hosts
      selenium get_hosts = new selenium();
      String AllHosts =  get_hosts.Get_All_VMSQE_Hosts();
      PrintWriter out = new PrintWriter("/tmp/vmsqepage");
      out.println(AllHosts);
      out.close();
      
      // Fill hosts massiv
      hosts = Fill_Hosts_Massiv_Init_VMSQE();
      
      // Delete from vmsqe_hosts which are not in devops list
      Delete_vmsqe();
      
      // Check ping and others attributes
      System.out.println("\nStarted: check ping");
      CheckPING_threads();
      System.out.println("\nStarted: check status, Failed jobs");
      Check_vmsqe_threads();
      System.out.println("Finished.");
      
      // Update DB
      System.out.println("\nStarted: Update DB");
      UpdateDB_vmsqe();
      System.out.println("Finished.");
      
      out = new PrintWriter("/tmp/last_update");
      java.util.Date date = new java.util.Date();
      java.sql.Date sqlDate = new java.sql.Date(date.getTime());
      System.out.println(sqlDate);
      out.println(sqlDate);
      out.close();
    }

    void DisableBadHosts() throws UnsupportedEncodingException, FileNotFoundException, IOException, SQLException, InterruptedException{    
      // Getting all EMBEDDED enabled hosts 
      selenium get_hosts = new selenium();
      String EnabledHosts =  get_hosts.Getting_Enabled_Hosts();
      String line = EnabledHosts;
      line = line.substring(line.lastIndexOf("multiple=\"multiple\"") + 1);
      line = line.substring(line.indexOf(">")+1);
      line = line.substring(0,line.indexOf("selected") - 8).trim();
      String host;
      while ( !line.isEmpty() ) {
            host = line.substring( line.indexOf("<option value")+15, line.indexOf(">")-1 );
            line = line.substring(line.indexOf("option>")+7).trim();
            hosts.add( host );           
      }
      int size = hosts.size();
      System.out.println("Embedded Enabled hosts:"+size);
      ArrayList<String> hosts_to_disable = new ArrayList<>();
      String host_page;
      int fail;
      String look;
      int nightly_failed,promoted_failed;
      for (int i = 0; i < size; i++) { 
        System.out.print(i+1+". "+hosts.get(i));
        host_page =  get_hosts.GetHost_silent(hosts.get(i));
        fail = 0;
        look = host_page;

        while (look.indexOf("FAILED")>0){
           look = look.substring(look.indexOf("FAILED")+6);
           fail++;
        }
        System.out.print(" -> Failed jobs:" + (fail) );
       
       if(fail > 0) {
         String s1,
                //s = "<td style=\"background-color: \"><a href=\"/faces/Job.xhtml?job_id=";
         s= "<a href=\"/faces/Job.xhtml;";
         nightly_failed = 0;
         promoted_failed = 0;
         // count failed nightly jobs
         while(host_page.indexOf(s) > 0){
           s1 = host_page.substring(host_page.indexOf(s)+s.length()+1);
           // if nightly and FAILED
           if(s1.indexOf("EMBEDDED_PROMOTION")<100 && s1.indexOf("EMBEDDED_PROMOTION")>0 && s1.indexOf("FAILED")<700 && s1.indexOf("FAILED")>0){
             promoted_failed++;  
           } 
           if(s1.indexOf("NIGHTLY")<100 && s1.indexOf("NIGHTLY")>0 && s1.indexOf("FAILED")<700 && s1.indexOf("FAILED")>0){
             nightly_failed++;  
           } 
           host_page = s1;
         } 
         System.out.print(" -> Failed nightly:" + nightly_failed);
         System.out.print(" -> Failed promotion:" + promoted_failed);
         if( nightly_failed > 0) {
           if(host_page.contains("class=\"disabled-free\"")) {
             System.out.println("already disabled");  
           } else { 
             hosts_to_disable.add(hosts.get(i));
             failed_jobs.add(nightly_failed);
             System.out.println(" -> need to verify or disable");
           }
         }  else System.out.println(""); 
        } else System.out.println("");  
      
      } //end for
        
      // disable all bad hosts 
      size = hosts_to_disable.size();
      System.out.println("Hosts to verify/disable:"+size);
      Long curTime = System.currentTimeMillis(); 
      Date curDate = new Date(curTime);
      Timestamp timestamp = new Timestamp(curDate.getTime());
      for (int i = 0; i < size; i++) { 
           host = hosts_to_disable.get(i);
           if(failed_jobs.get(i) > 2) {
              //get_hosts.DisableHost(host);
           }
           System.out.print(host+" failed jobs:"+failed_jobs.get(i));
           if(failed_jobs.get(i) > 2) System.out.println(" -> Disabled");else 
           System.out.println(" -> need to check host");
      }

      System.out.println("Update DB (table 'disabled')");
      try {
          con = DriverManager.getConnection(url, user, password);
          for (int i = 0; i < size; i++) { 
            if(failed_jobs.get(i) > 2) {  
              pstm = con.prepareStatement("insert into disabled (name,failed_jobs,date,time) values (?,?,?,?)");
              pstm.setString(1,hosts_to_disable.get(i));
              pstm.setInt(2,failed_jobs.get(i));
              pstm.setDate(3,curDate);
              pstm.setTimestamp(4, timestamp);
              pstm.executeUpdate();
            }  
          }     
      } catch (SQLException ex) {
         //ex.printStackTrace();      
      } 
      finally{
               try {
                    if(con != null) con.close();
                    if(pstm != null) pstm.close();
               } catch (SQLException e) {
                   System.out.println("On close: " + e.toString());
               }
      }
    }
    
    void EnableHosts_Threads() throws UnsupportedEncodingException, FileNotFoundException, IOException, SQLException, InterruptedException{    
    File file = new File( "/tmp/hosts");
        try (BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"))) {
            String excl;
            int i = 0;
            while ((excl = br.readLine()) != null) hosts.add(i,excl);
        }
      
      int size = hosts.size();
      System.out.println("VMSQE+Embedded hosts:"+size);
      for(int i = 0;i < hosts.size();i++){
         System.out.println(hosts.get(i));
      }
     
      
      selenium get_hosts = new selenium();
      String host;
      HtmlUnitDriver driver[] = new HtmlUnitDriver[size];
      ExecutorService executor = Executors.newFixedThreadPool(3);
      //System.out.println("MAX_THREADS="+MAX_THREADS);
      for(int i = 0;i < hosts.size();i++){
          Runnable worker = new MyRunnable_vmsqe_attr(hosts.get(i),i,driver[i]);
          executor.execute(worker);
      }
      executor.shutdown();
            // Wait until all threads are finish
	    while (!executor.isTerminated()) {}
	    System.out.println("\nFinished all threads");        
        for(int i = 0;i < hosts.size();i++){
           if( state.get(i).contains("DISABLE")){
             System.out.println(hosts.get(i));  
           }
        }
    }
    
    void DisableBadVMSQEHosts_Threads() throws UnsupportedEncodingException, FileNotFoundException, IOException, SQLException, InterruptedException{    
      String select = "";
      try {
          con = DriverManager.getConnection(url, user, password);
          stm = con.createStatement();
          //select = "SELECT name FROM vmsqe_hosts,hosts where state like '%ENABLED%'";
          select="SELECT name FROM vmsqe_hosts where state like '%ENABLED%' and groups='GTEE' UNION SELECT name FROM hosts where state like '%ENABLED%'";
          rs = stm.executeQuery(select);
          while(rs.next()){
             hosts.add(rs.getString(1));           
          }
          rs.close();
      } catch (SQLException ex) {
         //ex.printStackTrace();      
      } 
      finally{
               try {
                    if(con != null) con.close();
                    if(stm != null) stm.close();
               } catch (SQLException e) {
                   System.out.println("On close: " + e.toString());
               }
      }
      int size = hosts.size();
      System.out.println("VMSQE+Embedded hosts:"+size);
      /*
      selenium get_hosts = new selenium();
      String EnabledHosts =  get_hosts.Getting_Enabled_Hosts();
      String line = EnabledHosts;
      line = line.substring(line.lastIndexOf("multiple=\"multiple\"") + 1);
      line = line.substring(line.indexOf(">")+1);
      line = line.substring(0,line.indexOf("selected") - 8).trim();
      String host;
      while ( !line.isEmpty() ) {
            host = line.substring( line.indexOf("<option value")+15, line.indexOf(">")-1 );
            line = line.substring(line.indexOf("option>")+7).trim();
            hosts.add( host );           
      }
      size = hosts.size();
      System.out.println("VMSQE+Embedded hosts:"+size);
      */
      selenium get_hosts = new selenium();
      String host;
      HtmlUnitDriver driver[] = new HtmlUnitDriver[size];
      ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
      System.out.println("MAX_THREADS="+MAX_THREADS);
      for(int i = 0;i < hosts.size();i++){
          Runnable worker = new MyRunnable_vmsqe(hosts.get(i),driver[i],i);
          executor.execute(worker);
      }
      executor.shutdown();
      // Wait until all threads are finish
      while (!executor.isTerminated()) {}
   
      // Start to disable
      size = hosts_to_disable.size();
      
      if(size > 0){
        System.out.println("Hosts to verify:"+size);
        Long curTime = System.currentTimeMillis(); 
        Date curDate = new Date(curTime);
        Timestamp timestamp = new Timestamp(curDate.getTime());
        boolean exist = false;
        String body_line = "";
        ArrayList<String> body = new ArrayList<>();
        int disable_all = 0;
        for (int i = 0; i < size; i++) { 
           if(failure.get(i).contains("No space left on device") || failure.get(i).contains("getting job environment is failed")) {
              disable_all++; 
           }
        }
        if(disable_all < 10) {
        for (int i = 0; i < size; i++) { 
           host = hosts_to_disable.get(i);
           System.out.print((i+1)+"."+hosts_to_disable.get(i)+">"+failure.get(i));
           body_line = hosts_to_disable.get(i) + " > " + failure.get(i) + ">"+jobs.get(i);
           // TO DISABLE:
           // 1. ERROR:AGENT-JOBENV && RUN ABORTED -> getting job environment is failed
           // 2. No space left on device || There is not enough space on the disk
           //if(failure.get(i).contains("No space left on device") || failure.get(i).contains("There is not enough space on the disk") || failure.get(i).contains("getAttributes")) {
           if(failure.get(i).contains("No space left on device") || failure.get(i).contains("getting job environment is failed")) {
              get_hosts.DisableHost(host,failure.get(i));
              System.out.print(" -> Disabled");
              body_line = body_line + " -> DISABLED";
           } else {
              System.out.print(" -> Need to check host");
              body_line = body_line + " -> Need to check host";
           } 
           body.add(body_line);
           exist = false;
           try {
             con = DriverManager.getConnection(url, user, password);
             stm = con.createStatement();
             select = "SELECT name,date FROM disabled where name like '%"+hosts_to_disable.get(i)+"%' and date=curDate";
             rs = stm.executeQuery(select);
             if(rs.next()){
                exist = true;
             } 
             rs.close();
           } catch (SQLException ex) {
             //ex.printStackTrace();      
           } 
           if(exist) {
              System.out.println(" -> Exist in DB");
           } else {
              System.out.println(" -> Insert into DB"); 
              // Update DB
              try {
                con = DriverManager.getConnection(url, user, password);
                pstm = con.prepareStatement("insert into disabled (name,failed_jobs,date,time,failure,job) values (?,?,?,?,?,?)");
                pstm.setString(1,hosts_to_disable.get(i));
                pstm.setInt(2,failed_jobs.get(i));
                pstm.setDate(3,curDate);
                pstm.setTimestamp(4, timestamp);
                pstm.setString(5, failure.get(i));
                pstm.setString(6, jobs.get(i));
                pstm.executeUpdate();
                //System.out.println("Insert into DB");
              } catch (SQLException ex) {} 
              finally{
               try{
                    if(con != null) con.close();
                    if(pstm != null) pstm.close();
               } catch (SQLException e) {System.out.println("On close: " + e.toString());}
              }
           }
        } // for
        } // if disable_all < 10
        // Form body for Email
        body_line = "";
        SendEmail mailSender;
        int count = 0;
        for(int i = 0; i < body.size(); i++) {
           if( (body.get(i)).contains("DISABLED")) {
               count++; 
               if(count == 1) body_line = "DISABLED:\n";
               body_line = body_line + count + "." + body.get(i).substring(0,body.get(i).indexOf("->")) + "\n";
           }        
        }
        if(count > 0) {
           if(count > 9) {
             mailSender = new SendEmail("vitaly.missing@oracle.com","Disabled hosts -> Too many hosts",body_line);
           } else {
             mailSender = new SendEmail("vitaly.missing@oracle.com","Disabled hosts",body_line);
             mailSender = new SendEmail("leonid.mesnik@oracle.com","Disabled hosts",body_line);
             mailSender = new SendEmail("sergei.kovalev@oracle.com","Disabled hosts",body_line);
           }
        }  
        count = 0;
        body_line = "";
        for(int i = 0; i < body.size(); i++) {
           if(!(body.get(i)).contains("DISABLED")) {
               count++;
               if(count == 1) body_line = body_line + "\nNEED TO CHECK:\n";
               body_line = body_line + count + "." + body.get(i).substring(0,body.get(i).indexOf("->")) + "\n";
           }        
        }
        // Send Email
        if(count > 0) {
           mailSender = new SendEmail("vitaly.missing@oracle.com","Suspicious hosts",body_line);
           mailSender = new SendEmail("leonid.mesnik@oracle.com","Suspicious hosts",body_line);
        }  
      }
    }        

    void FillLocation() throws IOException {
      int size = hosts.size();
      String loc;
      //size =10;
      for (int i = 0; i < size; i++) { 
           location.add(i, "");
           loc = RunCmd("traceroute "+hosts.get(i));        
           if(loc.contains("santaclara")) {
               location.set(i, "santaclara");
           } else               
           if(loc.contains("burlington")) {
               location.set(i, "burlington");
           } else 
               location.set(i, "stpetersburg");
            System.out.println(hosts.get(i)+">"+location.get(i));
      }
    }
    
    String FillLocationHost(String host) throws IOException {
      String loc;
      loc = RunCmd("traceroute "+host);        
      if(loc.contains("santaclara")) {
         loc = "santaclara";
      } else               
      if(loc.contains("burlington")) {
         loc = "burlington";
      } else 
         loc = "stpetersburg";
         System.out.println(host+">"+loc);
      return loc;
    }
    
    void CheckDNS(String host) throws IOException {
      String dns;
      int size = hosts.size();
      for (int i = 0; i < 3; i++) { 
         dns = RunCmd("/home/gtee/dns_check.sh "+hosts.get(i));
         if(!dns.contains("10.209.76.198") || !dns.contains("10.209.76.197") || !dns.contains("192.135.82.132")) {
           System.out.println(host+">"+dns);
         } 
      }   
    }
    
    void CheckOwner(String host) throws IOException {
      String dns;
         dns = RunCmd("/work/owner "+host);
         if(dns.contains("aurora   auroragrp")) {
            System.out.println(host+"> aurora");
         } else 
         if(dns.contains("root     root")) {    
            System.out.println(host+"> root"); 
         } else
            System.out.println(host+">"+dns);      
    }
    
    void scp_host(String host) throws IOException {
      String dns;
         dns = RunCmd("/work/scp_host.sh "+host);
         System.out.println(host+">"+dns); 
    }
       
    void UpdateDB() throws FileNotFoundException, IOException {
      Long curTime = System.currentTimeMillis(); 
      Date curDate = new Date(curTime);
      java.util.Date time = java.util.Calendar.getInstance ().getTime();
      Timestamp timestamp = new Timestamp(curDate.getTime());
      String plocation;
      try {
       con = DriverManager.getConnection(url, user, password);
       // put all hosts into table hosts from massiv hosts
       stm = con.createStatement();
       int size = hosts.size();
       System.out.println("size="+size);
       String change;
       for (int i = 0; i < size; i++) { 
         rs = stm.executeQuery("select * from hosts where name like '%"+hosts.get(i)+"%'");
         // if found -> compare attributes
         if(rs.next()) {
            // name, ping,ssh,state,swap,cpu_name,cpu_feature,failed_jobs
            if(!rs.getString(2).trim().equals((ping.get(i)).trim()) || 
                !rs.getString(3).trim().equals((ssh.get(i)).trim()) ||
                !rs.getString(4).trim().equals((state.get(i)).trim()) ||
                !rs.getString(5).trim().equals((swap.get(i)).trim())) {
             
               change = "";
               if(!rs.getString(2).trim().equals((ping.get(i)).trim())) change = change + ">ping:"+rs.getString(2)+":"+ping.get(i); 
               if(!rs.getString(3).trim().equals((ssh.get(i)).trim())) change = change + ">ssh:"+rs.getString(3)+":"+ssh.get(i); 
               if(!rs.getString(4).trim().equals((state.get(i)).trim())) change = change + ">state:"+rs.getString(4)+":"+state.get(i)+":";  
               if(!rs.getString(5).trim().equals((swap.get(i)).trim())) change = change + ">swap:"+rs.getString(5)+":"+swap.get(i);  
               if(rs.getInt(8)!=failed_jobs.get(i)) change = change + ">failed_jobs:"+rs.getInt(8)+":"+failed_jobs.get(i);  
               change = change.replace("::", ":yellow:");
               change = change.replace("ENABLED", "green");
               change = change.replace("DISABLED", "disabled");
               System.out.println(rs.getString(1)+"-> Changes:"+change+" -> updating DB");
               pstm = con.prepareStatement("insert into changes (name,ping,date,time,ssh,state,swap,cpu_name,cpu_feature,failed_jobs) values (?,?,?,?,?,?,?,?,?,?)");
               pstm.setString(1,rs.getString(1));
               pstm.setString(2,rs.getString(2));
               pstm.setDate(3,rs.getDate(9));
               pstm.setTimestamp(4,rs.getTimestamp(10));
               pstm.setString(5,rs.getString(3));
               pstm.setString(6,rs.getString(4));
               pstm.setString(7,rs.getString(5));
               pstm.setString(8,rs.getString(6));
               pstm.setString(9,rs.getString(7));
               pstm.setInt(10,rs.getInt(8));
               pstm.executeUpdate();
               
               pstm = con.prepareStatement("update hosts set ping=?,date=?,time=?,ssh=?,state=?,swap=?,cpu_name=?,cpu_feature=?,failed_jobs=?,changed=?,mem_total=? where name=?");
               pstm.setString(1,ping.get(i));
               pstm.setDate(2,curDate);
               pstm.setTimestamp(3, timestamp);
               pstm.setString(4,ssh.get(i));
               pstm.setString(5,state.get(i));
               pstm.setString(6,swap.get(i));
               pstm.setString(7,cpu_name.get(i));
               pstm.setString(8,cpu_features.get(i));
               pstm.setInt(9,failed_jobs.get(i));
               pstm.setString(10,change);
               pstm.setString(11,mem_tot.get(i));
               pstm.setString(12,hosts.get(i));
               pstm.executeUpdate();
               //System.out.println("update result:"+pstm.getUpdateCount());
            }  
          } else {
             // add new record
             System.out.println("Not found:"+hosts.get(i)+" -> insert into table");
             plocation = FillLocationHost(hosts.get(i));
               pstm = con.prepareStatement("insert into hosts (name,ping,date,time,ssh,state,swap,cpu_name,cpu_feature,failed_jobs,location,changed,mem_total) values (?,?,?,?,?,?,?,?,?,?,?,?,?)");
               pstm.setString(1,hosts.get(i));
               pstm.setString(2,ping.get(i));
               pstm.setDate(3,curDate);
               pstm.setTimestamp(4, timestamp);
               pstm.setString(5,ssh.get(i));
               pstm.setString(6,state.get(i));
               pstm.setString(7,swap.get(i));
               pstm.setString(8,cpu_name.get(i));
               pstm.setString(9,cpu_features.get(i));
               pstm.setInt(10,failed_jobs.get(i));
               pstm.setString(11,plocation);
               pstm.setString(12,"");
               pstm.setString(13,mem_tot.get(i));
               pstm.executeUpdate();
         }
       }  
      }
      catch (SQLException ex) {
         ex.printStackTrace();      
      } 
      finally{
               try {
                    if(con != null) con.close();
                    if(stm != null) stm.close();
                    if(pstm != null) pstm.close();
               } catch (SQLException e) {
                   System.out.println("On close: " + e.toString());
               }
      }
    }
    
    void UpdateDB_vmsqe() throws FileNotFoundException, IOException {
      Long curTime = System.currentTimeMillis(); 
      Date curDate = new Date(curTime);
      java.util.Date time = java.util.Calendar.getInstance ().getTime();
      Timestamp timestamp = new Timestamp(curDate.getTime());
      java.sql.Date day_now = new java.sql.Date( new java.util.Date().getTime() );
      try {
       con = DriverManager.getConnection(url, user, password);
       // put all hosts into table hosts from massiv hosts
       stm = con.createStatement();
       int size = hosts.size();
       System.out.println("size="+size);
       String change;
       for (int i = 0; i < size; i++) { 
         rs = stm.executeQuery("select name,ping,failed_jobs,state,ssh,mem_total,os,cores,date,time,comment,requested,groups from vmsqe_hosts where name like '%"+hosts.get(i)+"%'");
         // if found -> compare attributes
         if(rs.next()) {
            if(!rs.getString(2).trim().equals((ping.get(i)).trim()) || 
               !rs.getString(4).trim().equals((state.get(i)).trim()) ||
               (rs.getString(12) == null) ||                    
               (rs.getString(12) != null && !rs.getString(12).trim().equals((requested.get(i)))) ||                    
               (rs.getString(13) == null) ||                    
               (rs.getString(13) != null && !rs.getString(13).trim().equals((group.get(i)))) ||                         
               !rs.getString(7).equals((oss.get(i)).trim()) ) {
             
               change = "";
               if(!rs.getString(2).trim().equals((ping.get(i)).trim())) change = change + ">ping:"+rs.getString(2)+":"+ping.get(i); 
               if(!rs.getString(4).trim().equals((state.get(i)).trim())) change = change + ">state:"+rs.getString(4)+":"+state.get(i)+":";  
               if(!rs.getString(7).equals((oss.get(i)).trim())) change = change + ">os:"+rs.getString(7)+":"+oss.get(i)+":";
               if(rs.getInt(3)!=failed_jobs.get(i)) change = change + ">failed_jobs:"+rs.getInt(3)+":"+failed_jobs.get(i);  
               if((rs.getString(12) == null) ||                    
                  (rs.getString(12) != null && !rs.getString(12).trim().equals((requested.get(i))))) 
                  change = change + requested.get(i);      
               if((rs.getString(13) == null) ||                    
                  (rs.getString(13) != null && !rs.getString(13).trim().equals((group.get(i))))) 
                  change = change + group.get(i);      
                  
               change = change.replace("::", ":yellow:");
               change = change.replace("ENABLED", "green");
               change = change.replace("DISABLED", "disabled");
               System.out.println(rs.getString(1)+"-> Changes:"+change+" -> updating DB");
               
               pstm = con.prepareStatement("update vmsqe_hosts set ping=?,date=?,time=?,state=?,failed_jobs=?,os=?,requested=?,groups=? where name=?");
               pstm.setString(1,ping.get(i));
               pstm.setDate(2,curDate);
               pstm.setTimestamp(3, timestamp);
               pstm.setString(4,state.get(i));
               pstm.setInt(5,failed_jobs.get(i));
               pstm.setString(6,oss.get(i).trim());
               pstm.setString(7,requested.get(i));
               pstm.setString(8,group.get(i));
               pstm.setString(9,hosts.get(i));
               pstm.executeUpdate();
               
               pstm = con.prepareStatement("insert into vmsqe_changes (name,ping,os,state,date,date_add) values (?,?,?,?,?,?)");
               pstm.setString(1,rs.getString(1));
               pstm.setString(2,rs.getString(2));
               pstm.setString(3,rs.getString(7));
               pstm.setString(4,rs.getString(4));
               pstm.setDate(5,rs.getDate(9));
               pstm.setDate(6,day_now);
               pstm.executeUpdate();
            } 
          } else {
             // add new record
             System.out.println("Not found:"+hosts.get(i)+" -> insert into table");
               pstm = con.prepareStatement("insert into vmsqe_hosts (name,ping,failed_jobs,state,mem_total,os,cores,date,time,requested,groups) values (?,?,?,?,?,?,?,?,?,?,?)");
               pstm.setString(1,hosts.get(i));
               pstm.setString(2,ping.get(i));
               pstm.setInt(3,failed_jobs.get(i));
               pstm.setString(4,state.get(i));
               pstm.setString(5,rams.get(i));
               pstm.setString(6,oss.get(i));
               pstm.setInt(7,cores.get(i));
               pstm.setDate(8,curDate);
               pstm.setTimestamp(9, timestamp);
               pstm.setString(10,requested.get(i));
               pstm.setString(11,group.get(i));
               pstm.executeUpdate();
         }
       }  
      }
      catch (SQLException ex) {
         ex.printStackTrace();      
      } 
      finally{
               try {
                    if(con != null) con.close();
                    if(stm != null) stm.close();
                    if(pstm != null) pstm.close();
               } catch (SQLException e) {
                   System.out.println("On close: " + e.toString());
               }
      }
    }
    
    void CheckPtrace() throws FileNotFoundException, IOException {
      try {
       con = DriverManager.getConnection(url, user, password);
       stm = con.createStatement();
       rs = stm.executeQuery("select name from hosts where name like '%sca-apm-xgene%' and ping=\"+\" and ssh=\"+\"");
       String output;
       while(rs.next()){
           System.out.print(rs.getString(1));
           //output = RunCommandOnHost(rs.getString(1).trim(),"ptrace");
           //System.out.println(" -> "+output);
       }     
      
      } catch (SQLException ex) {
         ex.printStackTrace();      
      } 
      finally{
               try {
                    if(con != null) con.close();
                    if(stm != null) stm.close();
               } catch (SQLException e) {
                   System.out.println("On close: " + e.toString());
               }
      }
    }
    
    void RunCommandOnHost(String host, String command) throws IOException {
       String cmd;
       int size = hosts.size();
       System.out.println(size);
       for (int i = 0; i < size; i++) { 
        if(location.contains("santaclara") || location.contains("burlington")) {
         cmd = "/export/dns.sh "+hosts.get(i);
         Process p = Runtime.getRuntime().exec(cmd);
         BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
         BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
         String s,output = "";
         while ((s = stdInput.readLine()) != null) {
            //System.out.println(s);
            output = output + s;
         }
         while ((s = stdError.readLine()) != null) {
            //System.out.println(s);
            output = output + s;
         }
         if(location.contains("santaclara")) {
           if(!output.contains("10.209.76.198") || !output.contains("10.209.76.197") || !output.contains("192.135.82.132")) {
               System.out.println("santa-clara:"+hosts.get(i)); 
           }    
         } else               
           if(!output.contains("192.135.82.124") || !output.contains("144.20.190.70") || !output.contains("192.135.82.132")) {
               System.out.println("burlington:"+hosts.get(i)); 
           }      
        } 
       }
    }
    
    void Error1() throws SQLException, ClassNotFoundException, IOException{
    String url = "jdbc:mysql://sette0.ru.oracle.com:3306/mysql";
    String uid = "root";
    String psw = "atari";
    Connection con = null;
    try {
     Class.forName("com.mysql.jdbc.Driver");
     con = DriverManager.getConnection(url, uid, psw);
     Statement statement = con.createStatement();
     //String s = "SELECT name FROM vmsqe_hosts where ping like '%+%' and (os like '%Linux%' or os like '%Ubuntu%' or os like '%openSUSE%')";
     String s = "SELECT name,ping,ssh FROM hosts where name like '%sca-apm-xgene%'";
     ResultSet rs = statement.executeQuery(s);
     int i = 1, j =1;
     String host = "", cmd ="", loc="";
     Boolean outp = false;
     while(rs.next()){
        host = rs.getString(1);
        System.out.print(i+">"+host);
        i++;
        if(!rs.getString(2).equals("-") || !rs.getString(3).equals("-") ) {
        cmd = "/work/dns.sh "+host;
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String ss,output = "";
         while ((ss = stdInput.readLine()) != null) {output = output + ss;}
         while ((ss = stdError.readLine()) != null) {output = output + ss;}
           //if(output.contains("169.254.182.77")) {
           if(output.contains("error while loading")) {
              System.out.println("> check");
           }   else  System.out.println("");
        } else  System.out.println("-> cant ssh");
     }
     } catch(Exception e){System.out.println(e);}
       if(con != null) {con.close();}
    }
    
    void Error2() throws SQLException, ClassNotFoundException, IOException{
    String url = "jdbc:mysql://localhost:3306/mysql";
    String uid = "root";
    String psw = "atari";
    Connection con = null;
    try {
     Class.forName("com.mysql.jdbc.Driver");
     con = DriverManager.getConnection(url, uid, psw);
     Statement statement = con.createStatement();
     String s = "SELECT name FROM vmsqe_hosts where ping like '%+%' and (os like '%Linux%' or os like '%Ubuntu%' or os like '%openSUSE%')";
     ResultSet rs = statement.executeQuery(s);
     int i = 1, j =1;
     String host = "", cmd ="", loc="";
     while(rs.next()){
        host = rs.getString(1);
        System.out.print(i+">"+host);
         
     }
     } catch(Exception e){System.out.println(e);}
       if(con != null) {con.close();}
    }
    
    void DNS() throws SQLException, ClassNotFoundException, IOException{
    String url = "jdbc:mysql://localhost:3306/mysql";
    String uid = "root";
    String psw = "atari";
    Connection con = null;
    try {
     Class.forName("com.mysql.jdbc.Driver");
     con = DriverManager.getConnection(url, uid, psw);
     Statement statement = con.createStatement();
     String s = "SELECT name,location FROM hosts where (location like 'santa%' or location like 'burlin%') and ping='+' and ssh='+'";
     ResultSet rs = statement.executeQuery(s);
     int i = 1, j =1;
     String host = "", cmd ="", loc="";
     Boolean outp = false;
     while(rs.next()){
        host = rs.getString(1);
        loc = rs.getString(2);
        System.out.println(host);
        if(host == null || host.contains("bus") || host.contains("ne-16") || host.contains("-7a") || host.contains("-8a") || host.contains("emb-apm-xgene") || !loc.contains("santaclara")) {continue;}
        outp = false;
        cmd = "/export/dns.sh "+host;
        //System.out.print(">"+cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String ss,output = "";
         while ((ss = stdInput.readLine()) != null) {output = output + ss;}
         while ((ss = stdError.readLine()) != null) {output = output + ss;}
           if(!output.contains("10.209.76.198") || !output.contains("10.209.76.197") || !output.contains("192.135.82.132")) {
              cmd = "/export/setup_dns_santa.sh "+host;
              p = Runtime.getRuntime().exec(cmd);
              System.out.print(">santa:"+host+">"+cmd);
              stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
              stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
              ss = "";
              output = "";
              while ((ss = stdInput.readLine()) != null) {output = output + ss;}
              while ((ss = stdError.readLine()) != null) {output = output + ss;}
              System.out.println(" "+i+">"+output);
              i++;
           }    
         cmd = "/export/dns1.sh "+host;
         p = Runtime.getRuntime().exec(cmd);
         //System.out.print(">"+cmd);
         stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
         stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
         ss = "";
         output = "";
         while ((ss = stdInput.readLine()) != null) {output = output + ss;}
         while ((ss = stdError.readLine()) != null) {output = output + ss;}
           if(!output.contains("10.209.76.198") || !output.contains("10.209.76.197") || !output.contains("192.135.82.132")) {
              cmd = "/export/setup_dns_santa1.sh "+host;
              System.out.print(">santa:"+host+">"+cmd);
              p = Runtime.getRuntime().exec(cmd);
              stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
              stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
              ss = "";
              output = "";
              while ((ss = stdInput.readLine()) != null) {output = output + ss;}
              while ((ss = stdError.readLine()) != null) {output = output + ss;}
              System.out.println(" "+i+">"+output);
              i++;
           }    
     }
     } catch(Exception e){System.out.println(e);}
       if(con != null) {con.close();}
    }

    public String GetHost(String host) throws Exception{
       hosts.add(0, host);
       hosts.set(0, host);
       CheckPING_SSH(hosts);
       return (host+ping.get(0)+ssh.get(0));
    }
    
   void DNS1() throws SQLException, ClassNotFoundException, IOException{
   String url = "jdbc:mysql://localhost:3306/mysql";
   String uid = "root";
   String psw = "atari";
   Connection con = null;
   try {
     Class.forName("com.mysql.jdbc.Driver");
     con = DriverManager.getConnection(url, uid, psw);
     Statement statement = con.createStatement();
     String s = "SELECT name,location FROM hosts where ping='+' and ssh='+'";
     ResultSet rs = statement.executeQuery(s);
     int i = 1, j =1;
     String host = "", cmd ="", loc="";
     Boolean outp = false;
     while(rs.next()){
        host = rs.getString(1);
        loc = rs.getString(2);
        //System.out.println(host);
        if(host == null || host.contains("bus") || host.contains("ne-16") || host.contains("-7a") || host.contains("-8a") || host.contains("emb-apm-xgene")) {continue;}
        outp = false;
        cmd = "/export/rc_local.sh "+host;
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String ss,output = "";
         while ((ss = stdInput.readLine()) != null) {output = output + ss;}
         while ((ss = stdError.readLine()) != null) {output = output + ss;}
           if(output.contains("backpack")) {
              System.out.println(i+"."+host);
              i++;
           }    
    }
    } catch(Exception e){System.out.println(e);}
      if(con != null) {con.close();}
    }
   
   void List() throws SQLException, ClassNotFoundException, IOException{
   String url = "jdbc:mysql://localhost:3306/mysql";
   String uid = "root";
   String psw = "atari";
   Connection con = null;
   try {
     Class.forName("com.mysql.jdbc.Driver");
     con = DriverManager.getConnection(url, uid, psw);
     Statement statement = con.createStatement();
     String s = "SELECT name,location FROM hosts where location like '%burlin%'";
     ResultSet rs = statement.executeQuery(s);
     int i = 1, j =1;
     String host = "", cmd ="", loc="";
     while(rs.next()){
        host = rs.getString(1);
        System.out.println(i+"."+host);
        i++;
     }
    } catch(Exception e){System.out.println(e);}
      if(con != null) {con.close();}
    }
   
    void Test() throws IOException, SQLException, InterruptedException {
        File file = new File( "/home/hudson/hosts.exclude");
        try (BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"))) {
            String excl;
            int i = 0;
            while ((excl = br.readLine()) != null) {
                   if(excl.contains("ru") || excl.contains("us") || excl.indexOf(".")>0 ){
                     hosts.add(i,excl);
                     i++;
                   }
            }   
            ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
            System.out.println("MAX_THREADS="+MAX_THREADS);
            for(i=0;i < 10;i++){
                Runnable worker = new MyRunnable(hosts.get(i),i);
                executor.execute(worker);
            }
            executor.shutdown();
            // Wait until all threads are finish
	    while (!executor.isTerminated()) {
    	    }
	    System.out.println("\nFinished all threads");        
        }
    }    
    
    void Test_vmsqe() throws IOException, SQLException, InterruptedException {
        File file = new File( "/home/vitaly/vmsqe_aurora_hosts");
        try (BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"))) {
            String excl,host;
            int i = 0;
            while ((excl = br.readLine()) != null) {
                  host = excl.substring(excl.indexOf(">")+1).trim();
                  hosts.add(i,host);
                  i++;
            }   
        } catch(Exception e){System.out.println(e);}    
        int i,j=1,k=0,no_in_devops = 0,in_devops = 0;
        String s, host;  
        String url = "jdbc:mysql://localhost:3306/mysql";
        String uid = "root";
        String psw = "atari";
        Connection con = null;
        System.out.println("size="+hosts.size());
        try {
          con = DriverManager.getConnection(url, user, password);
          stm = con.createStatement();
          selenium get_hosts;
          for(i=0;i < hosts.size();i++){
                //s = "SELECT name FROM vmsqe_hosts where name like '%"+hosts.get(i)+"%'";
                rs = stm.executeQuery("select name from vmsqe_hosts where name like '%"+hosts.get(i)+"%'");
                try {
                if(rs.next()){
                   //System.out.println(j+"."+rs.getString(1)+">found");
                   j++;
                } 
                else {
                  k++;  
                  System.out.println(k+"."+hosts.get(i));
                  hosts1.add(hosts.get(i));
                  //System.out.println(k);
                }
                } catch(Exception e){
                  //System.out.println(e);
                }
                rs.close();
            }
            System.out.println("found="+j);
            System.out.println("not found="+k);
            //statement.close();
         } catch(Exception e){System.out.println(e);}
         if(con != null) {con.close();}  
        
        driver = new FirefoxDriver();
        driver.get("http://devops.oraclecorp.com/user/evgeny.yavits/detail/hosts/");
        // Enter Credentials
        //driver.findElement(By.cssSelector("a[title*='Click to']")).click();
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
        String AllHosts = driver.getPageSource();
        String Info;
        for(i=0;i < hosts1.size();i++){
            host = hosts1.get(i).substring(0,hosts1.get(i).indexOf("."));
            driver.get("http://devops.oraclecorp.com/host/"+host+"/detail/");
            Info = driver.getPageSource();
            if(Info.contains("404 Not Found")){
               no_in_devops++;
               System.out.println(no_in_devops+"."+hosts1.get(i));
            } else {
               hosts1.set(i, ">"+hosts1.get(i));
            }
        }
        driver.close();
        for(i=0;i < hosts1.size();i++){
            if(hosts1.get(i).contains(">")) {
               in_devops++; 
               System.out.println(in_devops+"."+hosts1.get(i).substring(1));
            }
        }
    }    
    
    void Test_vmsqe1() throws IOException, SQLException, InterruptedException {
        int i,j=1,k=0,l=0;
        Connection con = null;
        System.out.println("size="+hosts.size());
        try {
          con = DriverManager.getConnection(url, user, password);
          stm = con.createStatement();
          for(i=0;i < hosts.size();i++){
                rs = stm.executeQuery("select name from vmsqe_hosts where name like '%"+hosts.get(i)+"%'");
                try {
                if(rs.next()){
                   //System.out.println(j+"."+rs.getString(1)+">found");
                   j++;
                } 
                else {
                  k++;  
                  System.out.println(k+"."+hosts.get(i));
                  hosts1.add(hosts.get(i));
                  System.out.println(k+"."+rs.getString(1)+">not found");
                }
                } catch(Exception e){
                  l++;  
                  //System.out.println(e);
                  
                }
                rs.close();
            }
            System.out.println("found="+j);
            System.out.println("not found="+k);
            System.out.println("exception="+l);
            stm.close();
            int iden,kol=0;
            for(i=0;i < hosts.size();i++){
                iden=0;
                for(j=0;j < hosts.size();j++){
                    if(hosts.get(i).equals(hosts.get(j))) iden++;
                }
                if(iden>1) {
                    kol++;
                    System.out.println(kol+"."+hosts.get(i));
                }
            }
            
         } catch(Exception e){System.out.println(e);}
         if(con != null) {con.close();}  
    }   
    
    void Delete_vmsqe() throws IOException, SQLException, InterruptedException {
        int i,j=1;
        boolean host_found_in_massiv;
        Connection con = null;
        System.out.println("size="+hosts.size());
        try {
          con = DriverManager.getConnection(url, user, password);
          stm = con.createStatement();
          rs = stm.executeQuery("select name from vmsqe_hosts");
          while(rs.next()){
              host_found_in_massiv = false;  
              for(i = 0; i < hosts.size(); i++){  
                  if(rs.getString(1).equals(hosts.get(i))){
                    host_found_in_massiv = true;    
                    break;
                  }
              }
              if(!host_found_in_massiv) {
                 System.out.println(j+"."+rs.getString(1)+"> not found in devops -> delete");
                 pstm = con.prepareStatement("delete from vmsqe_hosts where name=?");
                 pstm.setString(1, rs.getString(1));
                 pstm.executeUpdate();
                 j++;
              }
          }
          rs.close();
          stm.close();
        } catch(Exception e){System.out.println(e);}
        if(con != null) {con.close();}  
    }   
    
    boolean ssh_connect(String host,String user,String password) {
      int port = 22;
      com.jcraft.jsch.Session session = null;
      JSch jsch = new JSch();
      try {
        session = jsch.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(20000);
        return true;
      } catch(Exception e) {
        return false;
      }
       finally {
          session.disconnect();
      }
    }

    void check_overcomm(String host,String user,String pass) throws IOException {
        String log, path = "/export/home/" + user + "/agadmin.sh";
        log = RunCmd("/home/vitaly/check_overcommit.sh " + host + " " + user + " " + pass);
        if(log.contains("vm.overcommit_memory = 2")) {
           System.out.println("> no need to fix"); 
        } else {
           System.out.print("> try to fix"); 
           log = RunCmd("/home/vitaly/fix_overcommit.sh " + host + " " + user + " " + pass);
           if(log.contains("vm.overcommit_memory = 2")) {
              System.out.println("> fixed");  
           } else {
              System.out.print("> try user vmissing");  
              log = RunCmd("/home/vitaly/fix_overcommit.sh " + host + " vmissing Vm---1974");
              if(log.contains("vm.overcommit_memory = 2")) {
                System.out.println("> fixed");          
              } else
                System.out.println("> !!! not fixed");      
           }
        }
    }

    void check_hosts(String host,String user,String pass) throws IOException {
        String log = RunCmd("/home/vitaly/check_hosts.sh " + host + " " + user + " " + pass);
        if(log.contains("169.254.")) {
           System.out.println("> need to fix"); 
        } else System.out.println(">"); 
    }
    
    void check_mac_version(String host,String user,String pass) throws IOException {
        String cmd = "/home/vitaly/check_mac_version.sh " + host + " " + user + " " + pass;
        //System.out.println(">"+cmd); 
        String log = RunCmd(cmd);
        System.out.println("\t"+log); 
    }
    
    void scp_host(String host,String user) throws IOException {
        String log, path = "/export/home/" + user + "/agadmin.sh";
        log = RunCmd("/home/vitaly/check_agadmin.sh " + host + " " + user + " " + path);
        if(log.contains("No such file")) {
          System.out.print("-> no " + path);
          if(user.equals("aurora")) {
            System.out.print("-> try /export/local/aurora");
            path = "/export/local/" + user + "/agadmin.sh";
            log = RunCmd("/home/vitaly/check_agadmin.sh " + host + " " + user + " " + path);
            if(!log.contains("No such file")) {
              System.out.println("-> copy to "+ path.substring(0, path.length() - 10));
              log = RunCmd("/home/vitaly/scp_host.sh " + host + " " + user + " " + path.substring(0, path.indexOf("agadmin.sh")));
            } else System.out.println();
          } else System.out.println();
        } else {
           System.out.println("-> copy to "+ path.substring(0, path.length() - 10));
           log = RunCmd("/home/vitaly/scp_host.sh " + host + " " + user + " " + path.substring(0, path.indexOf("agadmin.sh")));
           //System.out.println(log);
        }
    }

    
    void update_scripts() throws IOException{
      String list = "sca00csg.us.oracle.com";
      int count = 0;
      String local_user;
      try {
       con = DriverManager.getConnection(url, user, password);
       stm = con.createStatement();
       //rs = stm.executeQuery("SELECT name FROM vmsqe_hosts where ping='+' and (os like '%Solaris%' or os like '%SunOS%' or os like '%Oracle.1%')");
       //rs = stm.executeQuery("SELECT name FROM vmsqe_hosts where ping='+' and (os like '%Linux%') and state='YELLOW' and name like '%.us.%'");
       rs = stm.executeQuery("SELECT name FROM vmsqe_hosts where ping='+' and (os like '%Linux%')");
       while(rs.next()){
            //if(list.contains(rs.getString(1))) {
            count++;
            System.out.print(count + "." + rs.getString(1) + ">");
            if(ssh_connect(rs.getString(1),"aurora","aurora")) {
              local_user = "aurora";
            } else
            if(ssh_connect(rs.getString(1),"aginfra","aurora")) {
              local_user = "aginfra";
            } else {
              System.out.println(" can't ssh with aurora/aginfra");
              local_user = "";
              continue;
            }
            System.out.print("user:" + local_user);
            scp_host(rs.getString(1),local_user);
           //}
       }
      } catch (SQLException ex) {}
      finally{
               try {
                    if(con != null) con.close();
                    if(stm != null) stm.close();
                    if(pstm != null) pstm.close();
               } catch (SQLException e) {
                    System.out.println("On close: " + e.toString());
               }
      }
    }

    void check_overcommit() throws IOException{
      int count = 0;
      String local_user,local_pass = "aurora";
      try {
       con = DriverManager.getConnection(url, user, password);
       stm = con.createStatement();
       //rs = stm.executeQuery("SELECT name FROM vmsqe_hosts where ping='+' and (os like '%Linux%') and (groups not like '%Tech%') and (groups not like '%Build%')");
       rs = stm.executeQuery("SELECT name FROM vmsqe_hosts where ping='+' and (os like '%Mac%')");
       while(rs.next()){
            count++;
            System.out.print(count + "." + rs.getString(1) + "\t");
            if(ssh_connect(rs.getString(1),"aurora","aurora")) {
              local_user = "aurora";
              local_pass = "aurora";
            } else
            if(ssh_connect(rs.getString(1),"aginfra","aurora")) {
              local_user = "aginfra";
              local_pass = "aurora";
            } else  
            if(ssh_connect(rs.getString(1),"gtee","Gt33acct")) {
              local_user = "gtee";
              local_pass = "Gt33acct";
            } else      
            if(ssh_connect(rs.getString(1),"vmissing","Vm---1974")) {    
              local_user = "vmissing";
              local_pass = "Vm---1974";
            } else {
              System.out.println(" can't ssh with aurora/aginfra/gtee/vmissing");
              local_user = "";
              continue; 
            }
            System.out.print("user:" + local_user);
            //check_overcomm(rs.getString(1),local_user,local_pass);
            //check_hosts(rs.getString(1),local_user,local_pass);
            check_mac_version(rs.getString(1),local_user,local_pass);
       }
      } catch (SQLException ex) {System.out.print(ex);}
      finally{
               try {
                    if(con != null) con.close();
                    if(stm != null) stm.close();
                    if(pstm != null) pstm.close();
               } catch (SQLException e) {
                    System.out.println("On close: " + e.toString());
               }
      }
    }
    
    public static void main(String[] args) throws InterruptedException, Exception {
       Host host = new Host();
       //host.Init_Embedded();
       //host.Init_VMSQE();
       //host.Test_vmsqe1();
       //host.DisableBadHosts();
       //host.DisableBadVMSQEHosts_Threads();
       //ost.EnableHosts_Threads();
       //host.Error1();
       //host.Test();
       //host.Check2();
       host.check_overcommit();
       //host.update_scripts();
       
       //System.exit(0);
    }

    public class MyRunnable implements Runnable {
      private final String host;
      private final int i;
      
      MyRunnable(String host,int i) {
	 this.host = host;
         this.i = i;
      }
      @Override
      public void run() {
          ping.add("-");
          ssh.add("-");
          try {
            if(ping(hosts.get(i))) {
               ping.set(i,"+");
            } else ping.set(i,"-"); 
            if (ping.get(i).equals("+")) {
               if(ssh_connect(hosts.get(i))) {
                  ssh.set(i,"+"); 
               } else  ssh.set(i,"-"); 
            }
          } catch (IOException e) {
            System.out.println("exception happened");
            e.printStackTrace();
            System.exit(-1);
          } catch (InterruptedException ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
          }
          System.out.println(Thread.currentThread().getName()+":"+hosts.size() + ":" + (i+1) + " " + hosts.get(i)+" ping:"+ping.get(i)+" ssh:"+ssh.get(i));          
      }
    }
    
    public class MyRunnable_vmsqe implements Runnable {
      private final String host;
      int disable = 0;
      private final HtmlUnitDriver driver;
      private final int i;
      
      MyRunnable_vmsqe(String host,HtmlUnitDriver driver,int i) {
	 this.host = host;
         this.driver = driver;
         this.i = i;
      }
      @Override
      public void run() {
        String host_page = "";
        selenium get_hosts = new selenium();
        try {
            host_page = get_hosts.GetHost_silent(host,driver);
        } catch (InterruptedException ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
        }
        int fail = 0;
        String look = host_page;
        String s1,job_url,url,job_page,failures="";        
        url = host_page;
        System.out.println(hosts.size() + ":" +i+":"+ host);
        if(host_page.contains("FAILED\n")){
        while (look.indexOf("FAILED\n") > 0){
          //System.out.println("FAILED");  
          try { 
           //get url   
           url = look.substring(0,look.indexOf("FAILED\n"));
           if(url.substring(url.length() - 6).contains("status")) {
              System.out.println("status:FAILED");
              look = look.substring(look.indexOf("FAILED\n")+6);
              continue;
           }
           if(url.substring(url.length() - 12).contains("macx_swapon")) {
              System.out.println("macx_swapon");
              look = look.substring(look.indexOf("FAILED\n")+6);
              continue;
           }
           if(url.contains("FAILED CKSUM")) {
              System.out.println("status:FAILED");
              look = look.substring(look.indexOf("FAILED\n")+6);
              continue;
           }
           if(!url.contains("job_id")) {
              System.out.println("no job_id");
              look = look.substring(look.indexOf("FAILED\n")+6);
              continue;
           }
           s1 = url.substring(url.lastIndexOf("job_id="));
           s1 = s1.substring(0,s1.indexOf("\""));
           job_url = "http://aurora.ru.oracle.com/faces/Job.xhtml?"+s1;
           look = look.substring(look.indexOf("FAILED\n")+6);
           fail++;
           System.out.println(" -> Scan jobs url:" + job_url );
           job_page =  get_hosts.GetURL_silent(job_url,driver);
           failures = "";
           // No space left on device
           if(job_page.contains("No space left on device") || job_page.contains("There is not enough space on the disk")) failures = "No space left on device"; else
           if(job_page.contains("ConnectorException: Requested file")) failures = "Requested file/build is not found"; else
           // http://aurora.ru.oracle.com/faces/Job.xhtml?job_id=1325527.JAVASE.NIGHTLY.TEST.Test_Baseline.test.test.windows-amd64_jck_jck-runtime-headless_server_mixed_vm_instr_invokedynamic.runTests
           if( (job_page.contains("ERROR:AGENT-JOBENV") && job_page.contains("RUN ABORTED") && !job_page.contains("InvalidARLException")) || (job_page.contains("Get job environment error") && (job_page.contains("java.io.FileNotFoundException") || job_page.contains("java.nio.file.AccessDeniedException"))) ) failures = "getting job environment is failed"; else
           if(job_page.contains("unexpected host/agent restart")) failures = "unexpected host/agent restart";else
           if(job_page.contains("com.sun.aurora.agent.AgentException: Checksum failed") && job_page.contains("is not found")) failures = "AgentException: Checksum failed";else               
           if(job_page.contains("unexpected host/agent restart") && job_url.contains("NIGHTLY")) failures = "unexpected host/agent restart";else
           if(job_page.contains("pid=PID is not number; failing check left-process") && job_url.contains("NIGHTLY")) failures="failing check left-process"; else
           if(job_url.contains("getAttributes.get")) failures="getAttributes failed";
           if(fail > 1) failures="jobs failed:"+fail;
           //if((failures.length() > 0 || fail > 2) && !host_page.contains("class=\"disabled-free\"") && job_url.contains("NIGHTLY")){
           if((failures.length() > 0 || fail > 1) && !host_page.contains("class=\"disabled-free\"")){
              failure.add(failures + (job_url.contains("NIGHTLY") ? ">NIGHTLY":""));
              hosts_to_disable.add(hosts.get(i));
              jobs.add(job_url);
              disable++;
              failed_jobs.add(fail);
              look="";
           } 
          } catch (StringIndexOutOfBoundsException ex) {
            ex.printStackTrace();      
           } catch (InterruptedException ex) { 
              Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
          } 
        }
        } // if( failed > 0 )
      }
    }
    
    public class MyRunnableVMSQE implements Runnable {
      private final String host;
      private final int i;
      
      MyRunnableVMSQE(String host,int i) {
	 this.host = host;
         this.i = i;
      }
      @Override
      public void run() {
          ping.add("-");
          try {
            if(ping(hosts.get(i))) {
               ping.set(i,"+");
            } else ping.set(i,"-"); 
          } catch (IOException e) {
            System.out.println("exception happened");
            e.printStackTrace();
            System.exit(-1);
          } catch (InterruptedException ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
          }
          System.out.println(Thread.currentThread().getName()+":"+hosts.size() + ":" + (i+1) + " " + hosts.get(i)+" ping:"+ping.get(i));          
      }
    }
    
    public class MyRunnable_attr implements Runnable {
      private final String host;
      private final int i;
      private final HtmlUnitDriver driver;
      
      MyRunnable_attr(String host,int i,HtmlUnitDriver driver) {
	 this.host = host;
         this.i = i;
         this.driver = driver;
      }
 
      @Override
      public void run() {
        selenium get_hosts = new selenium();
        String hostpage = "";
        int count;
        String look, sw, mem;
        try {
           hostpage = get_hosts.GetHost_silent(host,driver);
        } catch (InterruptedException ex) {
           Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
        }
                cpu_name.set(i,get_attribute("cpu.name:",hostpage));
                cpu_features.set(i,get_attribute("cpu.features:",hostpage));
                sw = get_attribute("swap.total:",hostpage);
                swap.set(i,sw);
                mem = get_attribute("mem.total:",hostpage);
                mem_tot.set(i,mem);
                // verify that only 1 host has a swap else force to exit job
                if(sw.trim().length()>2) ok=true;
                if(hostpage.contains("class=\"enabled-free\"") || hostpage.contains("class=\"enabled-working\"") || hostpage.contains("class=\"enabled-free-reserved\"") 
                  || ( (hostpage.contains("class=\"enabled-free-not-seen\"")) && (hostpage.contains("<span class=\"enabled-free-not-seen\">false</span>"))) ) {
                    state.set(i, "ENABLED");
                } else
                if(hostpage.contains("class=\"disabled-free\"") || hostpage.contains("class=\"disabled-working\"")){
                    state.set(i, "DISABLED");
                } else 
                if(hostpage.contains("class=\"enabled-free-not-seen\"") && !(hostpage.contains("<span class=\"enabled-free-not-seen\">false</span>"))){
                    state.set(i, "");
                } else 
                    state.set(i, "ENABLED");    
                
                count = 0;
                look = hostpage;
                while (look.indexOf("FAILED")>0){
                 look = look.substring(look.indexOf("FAILED")+6);
                 count++;
                }
                failed_jobs.set(i, count);
                try {
                  Double.parseDouble(swap.get(i));
                } catch (NumberFormatException e) {
                  swap.set(i,"0");
                }
                try {
                  Double.parseDouble(mem_tot.get(i));
                } catch (NumberFormatException e) {
                  mem_tot.set(i,"0");
                }
                System.out.println((i+1)+". "+ hosts.get(i) + " > " + cpu_name.get(i) + " > " + cpu_features.get(i) + " > swap:" + String.format("%8.1f",Double.parseDouble(swap.get(i))/(1024*1024*1024)).trim() + " > mem:" + String.format("%8.1f",Double.parseDouble(mem_tot.get(i))/(1024*1024*1024)).trim() + " >"+state.get(i) +">Failed jobs:" +failed_jobs.get(i));
      }
    }
    
    public class MyRunnable_vmsqe_attr implements Runnable {
      private final String host;
      private final int i;
      private final HtmlUnitDriver driver;
      
      MyRunnable_vmsqe_attr(String host,int i,HtmlUnitDriver driver) {
	 this.host = host;
         this.i = i;
         this.driver = driver;
      }
 
      @Override
      public void run() {
        selenium get_hosts = new selenium();
        String hostpage = "", os , os1;
        int count,core;
        String look;
        try {
           hostpage = get_hosts.GetHost_silent(host,driver);
        } catch (InterruptedException ex) {
           Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
        }
        // GTEE.unstable
        if(hostpage.contains("GTEE unstable group")){ 
           group.set(i,"GTEE.unstable");
        } else {  
           group.set(i,"GTEE"); 
        }   
        // STATE
        //if(hostpage.contains("class=\"enabled-free\"") || hostpage.contains("class=\"enabled-working\"") || hostpage.contains("class=\"enabled-free-reserved\"") 
        //   || ( (hostpage.contains("class=\"enabled-free-not-seen\"")) && (hostpage.contains("<span class=\"enabled-free-not-seen\">false</span>"))) ) {
        //   state.set(i, "ENABLED");
        //} else
        state.set(i, "ENABLED"); 
        if(hostpage.contains("class=\"disabled-free\"") || hostpage.contains("class=\"disabled-working\"")){
           state.set(i, "DISABLED");
        } else 
        if(hostpage.contains("Cannot find VHost with name")){    
           state.set(i, "MISSED"); 
        } else
        if(hostpage.contains("class=\"enabled-free-not-seen\"") && hostpage.contains("hung?") && !hostpage.contains("<span class=\"enabled-free-not-seen\">false")){    
           state.set(i, "YELLOW");
        }
        //else    
        //   state.set(i, "ENABLED"); 
        count = 0;
        mem_tot.set(i,get_attribute("mem.total:",hostpage));
        if(!state.get(i).equals("MISSED")) {
          os1 = oss.get(i).trim(); 
          if(!(os1.contains("Linux") || os1.contains("Windows") || os1.contains("Solaris") || os1.contains("SunOS") || os1.contains("Ubuntu") || os1.contains("OS X") || os1.contains("Mac"))) {
             os = get_attribute("os.full.name:",hostpage).trim();
             oss.set(i,os);
             System.out.println(hosts.get(i)+">"+oss.get(i));
          } 
        }  
        core = 0;
        try {
            core = Integer.parseInt(get_attribute("cpu.totalCores:",hostpage));
        } catch (NumberFormatException ex) {
        }
        cores.set(i,core);
        look = hostpage;
        while (look.indexOf("FAILED")>0){
            look = look.substring(look.indexOf("FAILED")+6);
            count++;
        }
        failed_jobs.set(i, count);
        System.out.println((i+1)+". "+ hosts.get(i) + " > " + state.get(i) + "> Cores:" +cores.get(i)+"> RAM:" +mem_tot.get(i)+"> Failed jobs:" +failed_jobs.get(i) +">"+group.get(i));
      }
    }

    public class DateLabelFormatter extends AbstractFormatter {
 
    private String datePattern = "yyyy-MM-dd";
    private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);
     
    @Override
    public Object stringToValue(String text) throws ParseException {
        return dateFormatter.parseObject(text);
    }
 
    @Override
    public String valueToString(Object value) throws ParseException {
        if (value != null) {
            Calendar cal = (Calendar) value;
            return dateFormatter.format(cal.getTime());
        }
         
        return "";
    }
 
}
}