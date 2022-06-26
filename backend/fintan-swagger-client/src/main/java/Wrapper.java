import java.io.*;
import java.util.*;

import org.acoli.fintan.swagger.client.*;
import org.acoli.fintan.swagger.client.api.CorpusApi;
import org.acoli.fintan.swagger.client.auth.*;
import org.acoli.fintan.swagger.client.model.*;

/** equivalent to Client, except that this one starts its own Docker containers
    Note: at the moment, this fails with a PERMANENT REDIRECT (308) response from Client 
*/
public class Wrapper {

    public static boolean dockerStarted(String name) throws IOException {
      Process p = Runtime.getRuntime().exec("docker ps");
      BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
      for(String line = in.readLine(); line!=null; line=in.readLine()) {
        if (line.contains(name)) {
          return true;
        }
      }
      return false;
    }

    public static void main(String[] args) throws Exception {

        if(args.length<3) {
          System.err.println("synopsis: Wrapper ID PepperImporter FILE[1..n]\n"+
            "Wrapper around Client, see there for arguments. This extends Client in starting a Docker container if not already running.");
          System.exit(1);
        }

        Process toBeKilled = null;
        if (!dockerStarted("acoli/tordf")) {
          toBeKilled = Runtime.getRuntime().exec("docker run -p 8080:8080 acoli/tordf");
          // wait for one sec
          Thread.sleep(1000);
        }

        if(dockerStarted("acoli/tordf")) {
          ArrayList<String> argv=new ArrayList<String>();
          argv.add("-base");
          argv.add("http://localhost:8080/data/");
          argv.addAll(Arrays.asList(args));

          Client.main(argv.toArray(new String[args.length+2]));

          if (toBeKilled!=null) {
            toBeKilled.destroy();
          }
        } else {
          BufferedReader in = new BufferedReader(new InputStreamReader(toBeKilled.getErrorStream()));
          for(String line = in.readLine(); line!=null; line=in.readLine()) {
            System.err.println(line);
          }
        }

    }
}
