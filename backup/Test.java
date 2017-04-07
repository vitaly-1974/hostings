package hosting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Test {
    public String RunCmd(String host, String cmd) throws Exception {
        //scmd = cmd;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Task(host,cmd));
        String status = "";
        try {
            //System.out.println(future.get(8, TimeUnit.SECONDS));
            future.get(15, TimeUnit.SECONDS);
            status = future.get();
        } catch (TimeoutException e) {
            future.cancel(true);
            status = "Terminated"; 
            System.out.println("Terminated!");
        }
        executor.shutdownNow();
        return status;
    }
}

class Task implements Callable<String> {
    String host_name  = "", host_cmd = "";
    Task(String host,String cmd) {
        host_name = host;
        host_cmd = cmd;
    }

    @Override
    public String call() throws Exception {
      String host = "" , cmd = "";
      String s = null, output = null;
      Process p = null;
      //Boolean exit = false;
       //cmd="/export/embedded/Embedded/host_monitoring/swap_view.sh "+host_name; 
      cmd = host_cmd+" "+host_name;
       try {   
          p = Runtime.getRuntime().exec(cmd);  
          BufferedReader stdInput = null, stdError = null;
          stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
          stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));  
          output = "";
          while ((s = stdInput.readLine()) != null) {
            output = output.concat(s);
          }
          while ((s = stdError.readLine()) != null) {
            output = output.concat("error:"+s);
            //exit = true;
          }
          p.waitFor();        
       } catch (InterruptedException e) {
         output = "Terminated";  
         Thread.currentThread().interrupt();
       } catch (Exception e) {  
          e.printStackTrace();  
       } finally {
          if(p != null)
             p.destroy();
       }
       return output;
    }
}