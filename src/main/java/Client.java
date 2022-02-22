import java.io.File;
import java.util.*;

import org.acoli.fintan.swagger.client.*;
import org.acoli.fintan.swagger.client.api.CorpusApi;
import org.acoli.fintan.swagger.client.auth.*;
import org.acoli.fintan.swagger.client.model.*;

public class Client {

    public static void main(String[] args) {

      CorpusApi apiInstance = new CorpusApi();

      ApiClient ac = apiInstance.getApiClient();
      ac.setConnectTimeout(Integer.MAX_VALUE);
      ac.setReadTimeout(Integer.MAX_VALUE);
      ac.setWriteTimeout(Integer.MAX_VALUE);
      apiInstance.setApiClient(ac);

      String basePath = apiInstance.getApiClient().getBasePath();

      System.err.println("synopsis: Client [-base BasePath] [-o format] ID PepperImporter FILE[1..n]\n"+
          "\tBasePath       base path, e.g., http://localhost:8080/data/, defaults to "+basePath+"\n"+
          "\tformat         output format, e.g., POWLA, POWLA-RDF, CoNLL-RDF, or CoNLL, defaults to POWLA\n"+
          "\tID             string, used for internal resource identification\n"+
          "\tPepperImporter one Pepper importer, e.g., ExmaraldaImporter, PaulaImporter, etc.\n"+
          "\tFILEi          argument file(s), should conform to requirements of the selected PepperImporter");

        String id=null;
        String importer = null;
        String format="POWLA";

        for(int i = 0; i<args.length; i++) {
          if(args[i].equalsIgnoreCase("-base")) {
            basePath=args[++i];
            ac.setBasePath(basePath);
            apiInstance.setApiClient(ac);
          } else if(args[i].equalsIgnoreCase("-o")) {
            format=args[++i];
          } else if(id==null) {
            id=args[i];
          } else if(importer==null) {
            importer=args[i];
          } else { // loop over files
            String file = args[i];
            System.err.println("processing "+file);
            try {
                //apiInstance.addFile(id, importer, new File(file));
                ApiResponse apiresponse = apiInstance.addFileWithHttpInfo(id, importer, format, new File(file));
                System.out.println("# doc: "+file);
                System.out.println("# response: "+apiresponse.getStatusCode());
                System.out.println("# headers: "+apiresponse.getHeaders());

                Response response= (Response)apiresponse.getData();
                System.out.println("# format: "+response.getFormat());
                System.out.println(response.getValue());
                //   JSonValue errorMessageValue = (JSonValue) response.getData().get("error");
                //   if (errorMessageValue != null) {
                //     errorMessage = (String) errorMessageValue.getValue();
                //   }
                // }

                System.out.println();
            } catch (ApiException e) {
                System.err.println("Exception when calling CorpusApi#addData");
                e.printStackTrace();
            }
          }
        }
    }
}
