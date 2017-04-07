package hosting;

import java.io.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
 
public class Host {
    
    PreparedStatement pstm = null;
    Statement stm  = null;
    Connection con = null;
    ResultSet rs = null;
    final String url = "jdbc:mysql://localhost:3306/mysql";
    final String user = "root";
    final String password = "atari";

    ArrayList<String> hosts,ping,ssh,swap,mem_tot,cpu_name,cpu_features,location,state=new ArrayList<>();
    ArrayList<Integer> failed_jobs=new ArrayList<>(); 

    public Host() {
        this.ssh = new ArrayList<>();
        this.state = new ArrayList<>();
        this.location = new ArrayList<>();
        this.cpu_name = new ArrayList<>();
        this.mem_tot = new ArrayList<>();
        this.swap = new ArrayList<>();
        this.ping = new ArrayList<>();
        this.hosts = new ArrayList<>();
        this.cpu_features = new ArrayList<>();
        this.failed_jobs = new ArrayList<>(); 
    }
                       
    String RunCmd(String cmd) throws IOException {
        System.out.println("Run command:"+cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String sin,serr,sout = "";
        while ((sin = stdInput.readLine()) != null) {
            sout = sout + sin;
        }
        while ((serr = stdError.readLine()) != null) {
            sout = sout + serr;
        }
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
    
    void Delete_Excluded(String host) throws SQLException {
          try {
            con = DriverManager.getConnection(url, user, password);
            pstm = con.prepareStatement("delete from hosts where name=?");
            pstm.setString(1, host);
            pstm.executeUpdate();
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
    }
    
    boolean ping(String host) throws IOException, InterruptedException {
      boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
      ProcessBuilder processBuilder = new ProcessBuilder("ping", isWindows? "-n" : "-c", "1", host);
      Process proc = processBuilder.start();
      int returnVal = proc.waitFor();
      return returnVal == 0;
    }
    
    void CheckPING_SSH(ArrayList<String> hosts) throws UnsupportedEncodingException, IOException, Exception {
      int size = hosts.size();
      String command,s;
      //int isssh;
      SSH ssh_ = new SSH();
      //size = 10;
      for (int i = 0; i < size; i++) { 
          //isssh = 1;
          System.out.print(size + ":" + (i+1) + " " + hosts.get(i));
          ping.add("-");
          ssh.add("-");
          try {
            if(ping(hosts.get(i))) {
               ping.set(i,"+");
            } else ping.set(i,"-"); 
            /*
              command = "nc -w 2 -zv "+ hosts.get(i) +" 22";
            // Ping, ssh
            // run the Unix command using the Runtime exec method:
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((s = stdError.readLine()) != null) {
                if(s.indexOf("failed")>0) {
                    ping.set(i,"+");
                    isssh = 0;
                    continue;
                }
                else 
                //if(s.indexOf("succeeded")>0) {ping.set(i,"+");ssh.set(i,"+");}
                if(s.indexOf("succeeded")>0) {
                    ping.set(i,"+");
                    continue;
                }
            }
            */
            // ssh
            //if (ping.get(i).equals("+") && isssh == 1) {
            if (ping.get(i).equals("+")) {
               if(ssh_.ssh_connect(hosts.get(i))) {
                  ssh.set(i,"+"); 
               } else  ssh.set(i,"-"); 
            }
            
            
            /*
            if (ping.get(i).equals("+") && isssh == 1) {
               command = "timeout 1 ssh "+ hosts.get(i);
               //String output = RunCommandOnHost(hosts.get(i),"");
               //System.out.println(output);
               
               p = Runtime.getRuntime().exec(command);
               //p.waitFor(1, TimeUnit.SECONDS);
               stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
               stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
               String log = "";
               while ((s = stdError.readLine()) != null) {
                 log = log +s;
                 //System.out.println("err:"+s);
               }
               while ((s = stdInput.readLine()) != null) {
                 log = log +s;
                 //System.out.println("inp:"+s);
               }
               //if (log.length() > 0) p.destroyForcibly();
               if(log.trim().length() > 0 && !log.contains("failed")) {
                 ssh.set(i,"+");
               } else {
                 ssh.set(i,"-");
               }   
            } 
            */
          }
          catch (IOException e) {
            System.out.println("exception happened");
            //e.printStackTrace();
            System.exit(-1);
          }
          System.out.println(" ping:"+ping.get(i)+" ssh:"+ssh.get(i));          
      } //End For hosts
        
      //for (int i = 0; i < size; i++) {
      //      System.out.printf("%-45s%-5s%-5s%n",hosts.get(i),"|"+ping.get(i),"|"+ssh.get(i));          
      //}
    }
    
    String get_attribute(String attr, String page){
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
    
    void Check_Attributes(ArrayList<String> hosts) throws InterruptedException, IOException {
        boolean ok;  
        //try (BufferedWriter bw = new BufferedWriter(new FileWriter("/home/hudson/hosts_status",false))) {
            selenium get_hosts = new selenium();
            int size = hosts.size();
            System.out.println("size="+size);
            String host_page;
            //size=10;
            int count;
            String look, sw, mem;
            ok = false;
            for (int i = 0; i < size; i++) {
                host_page =  get_hosts.GetHost_silent(hosts.get(i));
                // Scan attributes
                state.add(i,"");
                cpu_name.add(i,"");
                cpu_features.add(i,"");
                swap.add(i,"");
                failed_jobs.add(i,0);
                mem_tot.add(i,"");
                
                cpu_name.set(i,get_attribute("cpu.name:",host_page));
                cpu_features.set(i,get_attribute("cpu.features:",host_page));
                sw = get_attribute("swap.total:",host_page);
                swap.set(i,sw);
                mem = get_attribute("mem.total:",host_page);
                mem_tot.set(i,mem);
                // verify that only 1 host has a swap else force to exit job
                if(sw.trim().length()>2) ok=true;
                
                if(host_page.contains("class=\"enabled-free\"") || host_page.contains("class=\"enabled-working\"")) {
                    state.set(i, "ENABLED");
                } else
                    if(host_page.contains("class=\"disabled-free\"")){
                        state.set(i, "DISABLED");
                    } else
                        state.set(i, "");
                
                count = 0;
                look = host_page;
                while (look.indexOf("FAILED")>0){
                 look = look.substring(look.indexOf("FAILED")+6);
                 count++;
                }
                /*
                count = 0;
                String p = host_page, s1;
                String s = "<td style=\"background-color: \"><a href=\"/faces/Job.xhtml?job_id=";
                while(p.contains(s)){
                    s1 = p.substring(p.indexOf(s)+s.length()+1);
                    // if nightly and FAILED
                    System.out.println(s1.indexOf("NIGHTLY")+":"+s1.indexOf("FAILED"));
                    if(s1.indexOf("NIGHTLY")<100 && s1.indexOf("NIGHTLY")>0 && s1.indexOf("FAILED")<400 && s1.indexOf("FAILED")>0){
                        count++;
                    }
                    if(s1.indexOf("EMBEDDED_PROMOTION")<100 && s1.indexOf("EMBEDDED_PROMOTION")>0 && s1.indexOf("FAILED")<400 && s1.indexOf("FAILED")>0){
                        count++;
                    } 
                    p = s1;
                }
                */
                failed_jobs.set(i, count);
                // OUTPUT results to file
                //bw.write(hosts.get(i)+" "+ping.get(i)+" "+ssh.get(i)+" "+cpu_name.get(i)+" "+cpu_features.get(i)+" "+swap.get(i)+" "+state.get(i)+" "+failed_jobs.get(i)+" "+location.get(i)+'\n');
                //System.out.println(i+". "+ hosts.get(i) + " > " + cpu_name.get(i) + " > " + cpu_features.get(i) + " > " + swap.get(i) + " > " + state.get(i) +" Failed jobs:" +failed_jobs.get(i)+" > "+location.get(i));
                //bw.write(hosts.get(i)+" "+ping.get(i)+" "+ssh.get(i)+" "+cpu_name.get(i)+" "+cpu_features.get(i)+" "+swap.get(i)+" "+state.get(i)+" "+failed_jobs.get(i)+'\n');
                  
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
                System.out.println(i+". "+ hosts.get(i) + " > " + cpu_name.get(i) + " > " + cpu_features.get(i) + " > swap:" + String.format("%8.1f",Double.parseDouble(swap.get(i))/(1024*1024*1024)).trim() + " > mem:" + String.format("%8.1f",Double.parseDouble(mem_tot.get(i))/(1024*1024*1024)).trim() + " >"+state.get(i) +">Failed jobs:" +failed_jobs.get(i));
            }
        //}
        if(!ok){
            System.out.println("Get attributes method failed, no host with swap is found -> force to exit");
            System.exit(-1);
        }
    }
    
    void Init() throws FileNotFoundException, IOException, InterruptedException, Exception {
      //Getting All EMBEDDED hosts
      selenium get_hosts = new selenium();
      String AllHosts =  get_hosts.Get_All_Hosts();
       
      // Fill hosts massiv
      hosts = Fill_Hosts_Massiv_Init(hosts,AllHosts);

      // Delete hosts from not EMBEDDED group
      Delete_Trash();
      
      // Check ping,ssh and others attributes
      System.out.println("\nStarted: check ping,ssh");
      CheckPING_SSH(hosts);
      System.out.println("Finished.");

      System.out.println("\nStarted: check swap, cpu.name, cpu.features, status, Failed jobs");
      Check_Attributes(hosts);
      System.out.println("Finished.");

      System.out.println("\nStarted: Update DB");
      UpdateDB();
      System.out.println("Finished.");
    }

    void DisableBadHosts(Connection con) throws UnsupportedEncodingException, FileNotFoundException, IOException, SQLException, InterruptedException{    
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
      //size = 10;
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
         //System.out.println("s="+s+ " "+s.indexOf(s)); 
         while(host_page.indexOf(s) > 0){
           //System.out.println("index =" + host_page.indexOf(s)); 
           s1 = host_page.substring(host_page.indexOf(s)+s.length()+1);
           // if nightly and FAILED
           //System.out.print("-> nightly:"+s1.indexOf("NIGHTLY")+":"+s1.indexOf("FAILED"));
           //System.out.print("-> promotion:"+s1.indexOf("EMBEDDED_PROMOTION")+":"+s1.indexOf("FAILED"));
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
         if( (nightly_failed + promoted_failed) > 0) {
           if(host_page.contains("class=\"disabled-free\"")) {
             System.out.println("already disabled");  
           } else { 
             hosts_to_disable.add(hosts.get(i));
             failed_jobs.add(nightly_failed + promoted_failed);
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
           if(failed_jobs.get(i)>1) {
              //get_hosts.DisableHost(host);
           }
           System.out.print(host+" failed jobs:"+failed_jobs.get(i));
           if(failed_jobs.get(i) > 1) System.out.println(" -> Disabled");else 
           System.out.println(" -> need to check host (1 failed job)");
      }

      System.out.println("Update DB (table 'disabled')");
      try {
          con = DriverManager.getConnection(url, user, password);
          for (int i = 0; i < size; i++) { 
            if(failed_jobs.get(i)>1) {  
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
    
    void UpdateDB() throws FileNotFoundException, IOException {
       PreparedStatement pstm1 = null;
       PreparedStatement pstm2 = null;

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
       //size=10;
       for (int i = 0; i < size; i++) { 
         rs = stm.executeQuery("select * from hosts where name like '%"+hosts.get(i)+"%'");
         // if found -> compare attributes
         if(rs.next()) {
            System.out.println("Found:"+rs.getString(1));
            // name, ping,ssh,state,swap,cpu_name,cpu_feature,failed_jobs
            if(!rs.getString(2).trim().equals((ping.get(i)).trim()) || 
                !rs.getString(3).trim().equals((ssh.get(i)).trim()) ||
                !rs.getString(4).trim().equals((state.get(i)).trim()) ||
                !rs.getString(5).trim().equals((swap.get(i)).trim()) || 
                (rs.getInt(8)!=failed_jobs.get(i)) ) {
             
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
               //System.out.println("insert result:"+pstm.getUpdateCount());
         }
         //System.out.println("end for");
       }  
      }
      catch (SQLException ex) {
         //ex.printStackTrace();      
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
           output = RunCommandOnHost(rs.getString(1).trim(),"ptrace");
           System.out.println(" -> "+output);
       }     
      
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
    }
    
    String RunCommandOnHost(String host, String command) throws IOException {
        String cmd;
        // host = "emb-sca-apm-xgene-47.us.oracle.com";
        cmd = "/home/gtee/test.sh "+host;
        System.out.println(cmd);
        // cmd = "/home/gtee/sshpass -p 'qwerty' scp -o StrictHostKeyChecking=no /export/embedded/Embedded/host_monitoring/hosts_status.html hudson@sette0:/var/www/";
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
        return output;
    }
}