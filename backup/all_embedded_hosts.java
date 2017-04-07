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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class all_embedded_hosts {

    public static void main( String[] args )
    throws FileNotFoundException, IOException, InterruptedException {
        
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
}