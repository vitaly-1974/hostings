/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hosting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
/**
 *
 * @author vmissing
 */
public class create_html {
  
  void Table_Init(BufferedWriter bw, String Heating) throws IOException {
    bw.write("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">"+
          "<title>Hosts Status</title></head><body>"+
          "<h2>" + Heating + "</h2>"+ 
          "<table cellpadding=\"1\" border=\"1\" width=\"650\"><tr>"+
          "<th style=\"width:15px;\">N</th>"+
          "<th style=\"width:550px;\">Host</th>"+
          "<th style=\"width:10px;\">Ping</th>"+
          "<th style=\"width:20px;\">ssh</th>"+
          "<th style=\"width:20px;\">Status</th>"+
          "<th style=\"width:20px;\">Swap</th></tr>");          
  }  

  void Table_End(BufferedWriter bw) throws IOException {
    bw.write("</table>");
  }  
  
  public void new_html() throws UnsupportedEncodingException, IOException{
    File file = new File("/export/embedded/Embedded/host_monitoring/hosts_status");
    BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"));
    String line = null, 
           host = "",ping = "",ssh = "",sts = "",sswap="0",
           ping_style = "",ssh_style = "",sts_style = "";
    BufferedWriter bw = new BufferedWriter(new FileWriter("/export/embedded/Embedded/host_monitoring/hosts_status.html",false));
    
    int i = 0;    
    boolean first_disabled = false,
              first_no_ssh = false,
              first_no_aurora = false;
    
    Table_Init(bw,"no ping");
    
    while ( (line = br.readLine()) != null && line.length() > 0 ) {
       i++; 
       sswap = "0";host = "";ping = "";ssh = "";sts = "";ping_style = "";ssh_style = "";
       sswap=line.substring(line.lastIndexOf("|")+1).trim();
       line=line.substring(0,line.lastIndexOf("|"));
       host=line.substring(0, line.indexOf("|"));
       line=line.substring(line.indexOf("|")+1);
       ping=line.substring(0, line.indexOf("|"));
       line=line.substring(line.indexOf("|")+1);
       ssh=line.substring(0, line.indexOf("|"));
       line=line.substring(2);
       sts=line.trim();
       sts=sts.equals("DISABLED") ? sts : "ENABLED";
       ping_style=ping.equals("-") ? "style=\"background-color:red\"" : "style=\"background-color:green\"";
       ssh_style=ssh.equals("-") ? "style=\"background-color:red\"" : "style=\"background-color:green\"";
       sts_style="style=\"background-color:yellow\"";
       if(ping.equals(" ")) {
         ping_style="style=\"background-color:white\"";
         ssh_style="style=\"background-color:white\"";
         System.out.println(host);
       } else
       if(ping.contains("-")){
           ping="";
           ping_style="style=\"background-color:red\"";
           ssh_style="";
           sts_style="";
           ssh="";
           sts=sts.equals("DISABLED") ? sts : "";
       } else 
       if(ping.equals("+") && ssh.equals("-")){
           ssh_style="style=\"background-color:red\"";
           ssh="";
           sts_style=sts.equals("DISABLED") ? "style=\"background-color:gray\"": sts.equals("ENABLED") ? "style=\"background-color:yellow\"": "";
           if (!first_no_ssh) {
             first_no_ssh = true; 
             i=1;
             Table_End(bw);
             Table_Init(bw,"no ssh connect");
           }  
       }
       sts_style=sts.equals("DISABLED") ? "style=\"background-color:gray\"": sts_style;
       
       if (sts.equals("DISABLED") && !first_disabled) {
           first_disabled = true; 
           i=1;
           Table_End(bw);
           Table_Init(bw,"Disabled");
       } else
       if (ping.equals("+") && ssh.equals("+") && !first_no_aurora) {
           first_no_aurora = true; 
           i=1;
           Table_End(bw);
           Table_Init(bw,"check Aurora agent");
       }    

       bw.write("<tr><td>"+ i + "</td><td><a href=\"http://aurora.ru.oracle.com/faces/Host.xhtml?host="+host+"\">"+host+"</a></td>" +
          "<td "+ ping_style + "></td>"+        
          "<td "+ ssh_style + "></td>"+
          "<td "+ sts_style + ">"+sts+"</td>"+
          "<td>"+sswap+"</td></tr>");
    }
      bw.write("</table></body></html>");
      br.close();
      bw.close();
    }
} 