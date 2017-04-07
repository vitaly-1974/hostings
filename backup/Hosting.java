package hosting;

/**
 *
 * @author vmissing
 */
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
 
class Hosting {
 
    ArrayList<String> hosts = new ArrayList<String>(),
                              ping = new ArrayList<String>(),
                               ssh = new ArrayList<String>(),
                              swap = new ArrayList<String>(), 
                             state = new ArrayList<String>();

    String[] Enabled_Disabled_All = new String[2];
    
    void CreateFileAllHosts() throws UnsupportedEncodingException, FileNotFoundException, IOException {
        File file = new File( "/export/embedded/Embedded/host_monitoring/all_embedded_hosts" );
        BufferedReader br = new BufferedReader (
            new InputStreamReader(
                new FileInputStream( file ), "UTF-8"
            )
        );
        String line = null, host = "";
        while ((line = br.readLine()) != null) {
            if (line.indexOf('"')>0){
            host = line.substring(line.indexOf('"')+1,line.lastIndexOf('"'));
            System.out.println( host );
            }
        }
        br.close();   
    }
    
            
    void LoadAuroraPage() throws IOException {
        String cmd  = null;
        System.out.println("Starting loading aurora page ...");
        cmd = "wget -O /home/gtee/enabled_hosts http://aurora.ru.oracle.com/faces/Submit.xhtml?templateName=EMB_generic";
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String s;
        System.out.println("Finished loading aurora page ...");
    }

    void RunCmd(String cmd) throws IOException {
        System.out.println("Run command:"+cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String s;
        while ((s = stdInput.readLine()) != null) {
              System.out.println("output:"+s);
        }
        while ((s = stdError.readLine()) != null) {
              System.out.println("error:"+s);
        }
    }    
    
    void UploadHtmlSette0(File file) throws IOException {
        String cmd  = null;
        cmd = "sh /home/gtee/scp_sette0.sh";
        cmd = "/home/gtee/test.sh emb-sca-apm-xgene-47.us.oracle.com";
        // cmd = "/home/gtee/sshpass -p 'qwerty' scp -o StrictHostKeyChecking=no /export/embedded/Embedded/host_monitoring/hosts_status.html hudson@sette0:/var/www/";
        System.out.println(cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String s;
        while ((s = stdInput.readLine()) != null) {
              System.out.println(s);
        }
        while ((s = stdError.readLine()) != null) {
              System.out.println(s);
        }
    }

    //sshpass -p 'qwerty' scp /export/embedded/Embedded/host_monitoring/hosts_status.html hudson@sette0:/var/www/
        
    ArrayList<String> Fill_Hosts_Massiv(ArrayList<String> hosts) throws IOException {
        File file = new File( "/export/embedded/Embedded/host_monitoring/all_hosts" );
        //BufferedWriter bw = new BufferedWriter(new FileWriter(file,false));
        //file = new File( "/export/embedded/Embedded/host_monitoring/hosts.exclude");
        //BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"));
        
        String line = null, exclude = null, host = null;
        
        line = Enabled_Disabled_All[2];     
        line = line.substring(line.lastIndexOf("form:j_idt159:loop:1:m_pv\">") + 27);
        line = line.substring(0,line.indexOf("selected") - 8).trim();
        while ( !line.isEmpty() ) {
            host = line.substring( line.indexOf("<option value")+15, line.indexOf(">")-1 );
            System.out.println(host);
            line = line.substring(line.indexOf("option>")+7).trim();
            //check if it is not in exclude list
            //while ((exclude = br.readLine()) != null) {
                          
            //}
        }
        //br.close();
        
        //bw.close();
        /*         
        file = new File( "/export/embedded/Embedded/host_monitoring/all_embedded_hosts_no_ppc" );
        BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"));
        while ((line = br.readLine()) != null) {
            if(!line.contains("exclude")) hosts.add( line );
        }
        br.close();
        */
        return hosts;
    }    
    
    void CreateHostsToReboot(File file) throws UnsupportedEncodingException, IOException {
      BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"));
      BufferedWriter bw = new BufferedWriter(new FileWriter("/export/embedded/Embedded/host_monitoring/hosts_to_reboot",false));
      String line = null, host = "";
      String ping = "",ssh = "",sts = "";
      while ( (line = br.readLine()) != null && line.length() > 0 ) {
         line=line.substring(0,line.lastIndexOf("|"));
         host=line.substring(0, line.indexOf("|"));
         line=line.substring(line.indexOf("|")+1);
         ping=line.substring(0, line.indexOf("|"));
         line=line.substring(line.indexOf("|")+1);
         ssh=line.substring(0, line.indexOf("|"));
         line=line.substring(2);
         sts=line.trim();
         if (ping.equals("+") && ssh.equals("+") && sts.equals("-")) {  
            System.out.println(host);
            bw.write(host+'\n');
         } 
      }
      br.close();
      bw.close();      
    }
    
    void RestartAurora(File file) throws UnsupportedEncodingException, IOException, Exception {
      BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"));
      String host = "" , output = null;
      Test run = null;
      while ( (host = br.readLine()) != null && host.length() > 0 ) {
           run = new Test();
           output = run.RunCmd(host,"/export/embedded/Embedded/host_monitoring/restart_aurora.sh");
           System.out.println(host+ ":"+output);
      }
      br.close();
    }
    
    void RebootHost(File file) throws UnsupportedEncodingException, IOException, Exception {
      BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"));
      String host = "" , output = null;
      Test run = null;
      while ( (host = br.readLine()) != null && host.length() > 0 ) {
           run = new Test();
           output = run.RunCmd(host,"/export/embedded/Embedded/host_monitoring/reboot_host.sh");
           System.out.println(host+ ":"+output);
      }
      br.close();
    }
    
    void CheckPING_SSH(ArrayList<String> hosts) throws UnsupportedEncodingException, IOException, Exception {
    BufferedWriter bw = new BufferedWriter(new FileWriter("/export/embedded/Embedded/host_monitoring/hosts_status",false));
    int size = hosts.size();
    String command,s;
    //size = 5;
    for (int i = 0; i < size; i++) { 
          System.out.println(size + ":" + (i+1) + " " + hosts.get(i));
          ping.add("-");
          ssh.add("-");
          state.add("-");
          swap.add("");
          
          // Fill hosts status   
          if ( Enabled_Disabled_All[0].contains("option value=\""+hosts.get(i)+"\"") ) {state.set(i, "ENABLED");}
          if ( Enabled_Disabled_All[1].contains("option value=\""+hosts.get(i)+"\"") ) {state.set(i, "DISABLED");}

          // Skip DISABLED/ENABLED hosts
          if (state.get(i).equals("DISABLED") || state.get(i).equals("ENABLED")) {          
             ping.set(i," ");
             ssh.set(i," "); 
          } else {
          try {
            command = "nc -w 2 -zv "+ hosts.get(i) +" 22";
            //1) Ping, ssh
            // run the Unix command using the Runtime exec method:
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((s = stdError.readLine()) != null) {
                if(s.indexOf("failed")>0) ping.set(i,"+");
                else
                if(s.indexOf("succeeded")>0) {ping.set(i,"+");ssh.set(i,"+");}
            }
          }
          catch (IOException e) {
            System.out.println("exception happened");
            e.printStackTrace();
            System.exit(-1);
          }
          
          // Getting swap for hosts with ping+ssh
          if(ping.get(i).equals("+") && ssh.get(i).equals("+")){
            String host_swap = "";
            Test getswap = new Test();
            host_swap = getswap.RunCmd(hosts.get(i),"/export/embedded/Embedded/host_monitoring/swap_view.sh");
            if (host_swap.contains("Swap")){
              host_swap = (host_swap.substring(host_swap.indexOf("Swap")+5).trim());
              host_swap = host_swap.substring(0,host_swap.indexOf(" "));
              swap.set(i,host_swap);
            } else 
            if (host_swap.contains("Terminated")){
              ssh.set(i, "-");
              swap.set(i,"?");
            } else 
              swap.set(i,"0");      
          }
          }
    } //End For hosts
        
    // Order and Output to file not pingable hosts
    for (int i = 0; i < size; i++) {
        if(ping.get(i).equals("-")){
            System.out.printf("%-45s%-5s%-5s%-10s%n","-p:"+hosts.get(i),"|"+ping.get(i),"|"+ssh.get(i),"|"+state.get(i));          
            bw.write(hosts.get(i)+"|"+ping.get(i)+"|"+ssh.get(i)+"|"+state.get(i)+"|"+swap.get(i)+'\n');
        }
    }
    
    // Output not ssh hosts
    for (int i = 0; i < size; i++) {
        if(!ping.get(i).equals("-") && ssh.get(i).equals("-")){
            System.out.printf("%-45s%-5s%-5s%-10s%n","-ssh:"+hosts.get(i),"|"+ping.get(i),"|"+ssh.get(i),"|"+state.get(i));          
            bw.write(hosts.get(i)+"|"+ping.get(i)+"|"+ssh.get(i)+"|"+state.get(i)+"|"+swap.get(i)+'\n');
        }
    }
    
    // Output yellow hosts
    for (int i = 0; i < size; i++) {
        if(ping.get(i).equals("+") && ssh.get(i).equals("+") && state.get(i).equals("-")) {
            System.out.printf("%-45s%-5s%-5s%-10s%n","-ssh:"+hosts.get(i),"|"+ping.get(i),"|"+ssh.get(i),"|"+state.get(i));          
            bw.write(hosts.get(i)+"|"+ping.get(i)+"|"+ssh.get(i)+"|"+state.get(i)+"|"+swap.get(i)+'\n');
        }
    }
    
    // output DISABLED hosts
    for (int i = 0; i < size; i++) {
        if(state.get(i).equals("DISABLED")){
            //System.out.printf("%-45s%-5s%-5s%-10s%n",hosts.get(i),"|"+ping.get(i),"|"+ssh.get(i),"|"+state.get(i));          
            bw.write(hosts.get(i)+"|"+ping.get(i)+"|"+ssh.get(i)+"|"+state.get(i)+"|"+swap.get(i)+'\n');
        }
    }
    bw.close();
    }
    
    //public static void main(String args[]) throws FileNotFoundException, IOException, InterruptedException, Exception {
    //public void Update() throws FileNotFoundException, IOException, InterruptedException, Exception {
    void Update() throws FileNotFoundException, IOException, InterruptedException, Exception {
        String s = null, command = null;
 
        System.out.println("\nStarted: upload html into sette0");
        File file = new File( "/export/embedded/Embedded/host_monitoring/hosts_status.html" );
        UploadHtmlSette0(file);
        System.out.println("Finished.");
        System.exit(0);
 
        //Getting Enabled,Disabled hosts and put to var -> Enabled_Hosts,Disabled_Hosts
        selenium get_hosts = new selenium();
        Enabled_Disabled_All =  get_hosts.Getting_Enabled_Disabled_Hosts();
        
        // Fill massiv of all hosts from file all_embedded_hosts_no_ppc
        hosts = Fill_Hosts_Massiv(hosts);
        System.exit(0);
        
        // Scan status ping,ssh,swap for enabled hosts
        System.out.println("\nStarted: check ping,ssh,swap");
        CheckPING_SSH(hosts);
        System.out.println("Finished.");
        
        // Create hosts_status.html
        System.out.println("\nStarted: create html page");
        create_html new_html = new create_html(); 
        new_html.new_html();
        System.out.println("Finished.");
       
        // scp hosts_status.html -> sette:/var/www/
        System.out.println("\nStarted: upload html into sette0");
        //File file = new File( "/export/embedded/Embedded/host_monitoring/hosts_status.html" );
        UploadHtmlSette0(file);
        System.out.println("Finished.");
      
        // Create a List of Hosts to be reboot via php script -> new file hosts_to_reboot
        file = new File("/export/embedded/Embedded/host_monitoring/hosts_status");
        System.out.println("\nStarted: Create a list of hosts to restart aurora agent");
        CreateHostsToReboot(file);
        System.out.println("Finished.");

        System.exit(0);
        
        // Restart aurora agent
        System.out.println("\nStarted: Reboot");
        file = new File("/export/embedded/Embedded/host_monitoring/hosts_to_reboot");
        RebootHost(file);
        System.out.println("Finished: Reboot");
        
        // wait for 60sec
        System.out.println("\nStarted: sleep");
        Thread.sleep(60000);
        
        // Re-read state of hosts in aurora
        System.out.print("\nSubmit aurora job for all ENABLED/DISABLED hosts");
        get_hosts = new selenium();
        Enabled_Disabled_All = get_hosts.Getting_Enabled_Disabled_Hosts();
        System.out.println(" -> Finished");
    
        // Report after restart agent
        System.out.println("\n Report:");
        file = new File("/export/embedded/Embedded/host_monitoring/hosts_to_reboot");
        BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"));
        String host = "";
        while ( (host = br.readLine()) != null && host.length() > 0 ) {
          System.out.print(host);   
          if ( Enabled_Disabled_All[0].contains("option value=\""+host+"\"") ) 
             System.out.println(" GREEN"); 
          else 
             System.out.println(" YELLOW");             
        }
        br.close();
        System.exit(0);

        // Restart aurora
        //file = new File("/export/embedded/Embedded/host_monitoring/hosts_to_reboot");
        //RestartAurora(file);
 
        //Reboot hosts
        //RebootHosts(file);
}
}