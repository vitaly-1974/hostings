/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hosting;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import org.eclipse.jetty.websocket.api.Session;

public class SSH {
    
    public boolean ssh_connect(String host) {
    String user = "gtee";
    String password = "Gt33acct";
    int port=22;
    com.jcraft.jsch.Session session = null;
    JSch jsch = new JSch();
    try {
        session = jsch.getSession(user, host, port);
        //.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        //System.out.println("Establishing Connection...");
        session.connect(20000);
        //System.out.println("Connection established.");
        return true;
    } catch(Exception e) {
        //System.out.println("Connection not established.");
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
    }  finally {session.disconnect();}
    }
    
}    
 