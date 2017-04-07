package hosting;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public class mysql {
    
static void InsertAllHosts(Connection con) throws UnsupportedEncodingException, FileNotFoundException, IOException, SQLException{    
PreparedStatement pstm = null;
     //File file = new File( "/export/embedded/Embedded/host_monitoring/all_embedded_hosts_no_ppc" );
     //BufferedReader br = new BufferedReader (new InputStreamReader(new FileInputStream( file ), "UTF-8"));
     String line = null, host = "";
     //while ((line = br.readLine()) != null) {
        //pstm = con.prepareStatement("insert into hosts (name) values (?)");
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery("select * from hosts");
        while(rs.next()){
           System.out.println(rs.getString(1));
         }   
        //pstm.setString(1,line);
        //pstm.executeUpdate();
        //System.out.println("inserted " + line);
     //}
     //br.close();
}
        
public static void main(String[] args) throws MalformedURLException, IOException, SQLException, InterruptedException, Exception {
PreparedStatement pstm = null;
Statement stm  = null;
Connection con = null;
ResultSet rs = null;
String url = "jdbc:mysql://localhost:3306/mysql";
String user = "root";
String password = "atari";

Host host = new Host();

/*
SSH sh = new SSH();

if(sh.ssh_connect("emb-sca-fs-p1025twr-7.us.oracle.com")) {
  System.out.println("+");  
} else System.out.println("-");  
*/

//host.DisableBadHosts(con);
//Hosting host1 = new Hosting();
host.Init();
//host.CheckPtrace();
//host1.Update();
System.exit(0);
}

}