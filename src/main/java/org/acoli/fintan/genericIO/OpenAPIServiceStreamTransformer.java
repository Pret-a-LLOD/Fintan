/*
 * Copyright [2021] [ACoLi Lab, Prof. Dr. Chiarcos, Christian Faeth, Goethe University Frankfurt]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.acoli.fintan.genericIO;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.acoli.fintan.core.FintanManager;
import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.acoli.fintan.core.StreamTransformerGenericIO;
import org.acoli.fintan.swagger.client.ApiClient;
import org.acoli.fintan.swagger.client.ApiException;
import org.acoli.fintan.swagger.client.ApiResponse;
import org.acoli.fintan.swagger.client.Configuration;
import org.acoli.fintan.swagger.client.Pair;
import org.acoli.fintan.swagger.client.ProgressRequestBody;
import org.acoli.fintan.swagger.client.ProgressResponseBody;
import org.acoli.fintan.swagger.client.model.Response;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.reflect.TypeToken;


/**
 * Template for a FintanStreamComponent wrapping a generic OpenAPI Service.
 * It is using a generic Swagger Codegen Client.
 * Originally designed for wrapping a pepper/powla converter.
 * May run other services which are similarly structured.
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class OpenAPIServiceStreamTransformer extends StreamTransformerGenericIO implements FintanStreamComponentFactory {
	
	/**
	 * The following parameters activate segmented processing:
	 * * `delimiterIn` activates the segmented processing, like the delimiter parameters in the core classes, it corresponds to the full text content of a single delimiting line. If it is unspecified or null, the data is processed as bulk.
	 * * `delimiterOut` optionally defines the delimiter line written to the output stream after each segment. It only applies if delimiterIn is set as well.
	 * 
	 * The following parameters determine how to supply data to the Service API
	 * * `supplyMethod` currently supports two options:
	 * 		* "blob" for directly supplying data (or data segments, determined by delimiterIn) as a blob
	 * 		* "cachedFile" for instead completely fetching the stream in a temp file and instead supplying the filename
	 * * `cachePath`: (OPTIONAL) defines where to place the temp folder. Default is fileCache/<instanceIdentifier> relative to the execution folder.
	 *
	 * The other parameters correspond to OpenAPI / Swagger v2 specifications:
	 * * `apiURI`: the base URI for accessing the service
	 * * `apiMethodPath`: the path of the API method to be called, relative to the apiURI. Parameters can be written in curly brackets: /path/{id}
	 * * `apiMethodOperation`: supported operations for Swagger v2: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
	 * 
	 * * `acceptTypes`: formats to be supplied with the Accept header, e.g. "application/json"
	 * * `contentTypes`: content types to be supplied with the Content-Type header, e.g. "multipart/form-data"
	 * 
	 * * `useDataAsParam`: Determines how the data is being supplied to the API: 
	 * 		* Syntax: "<paramType>:::<paramName>" 
	 * 		* "form:::data" places each blob or cacheFile URL into the formParams under the key "data"
	 * * `useStreamNameAsParam`: (OPTIONAL) determines, whether a unique streamID should be supplied as a parameter
	 * 		* Syntax: "<paramType>:::<paramName>" 
	 * 		* streamID consists of the stream name concatenated with a hashCode for the data blob
	 * 		* "path:::id" places each unique streamID into the pathParams under the key "id"
	 * 
	 * * `pathParams`: Parameters to be set in the path, e.g.: 
	 * 		For an "apiMethodPath": "/path/{id}" 
	 * 		and a "pathParams" : {"id" : "test123"}, 
	 * 		the resulting request would go to: <apiURI>/path/test123
	 * * `queryParams`: List of query parameters. 
	 * 		Syntax: "queryParams" : { "<param1>" : "<value1>", "<param2>" : "<value2>", ... }
	 * * `collectionQueryParams`: List of collection query parameters. 
	 * 		Syntax: "collectionQueryParams" : { "<param1>" : "<value1>", "<param2>" : "<value2>", ... }
	 * * `headerParams`: List of header parameters. 
	 * 		Syntax: "headerParams" : { "<param1>" : "<value1>", "<param2>" : "<value2>", ... }
	 * * `formParams`: List of form parameters. 
	 * 		Syntax: "formParams" : { "<param1>" : "<value1>", "<param2>" : "<value2>", ... }
	 */
	public OpenAPIServiceStreamTransformer buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		OpenAPIServiceStreamTransformer transformer = new OpenAPIServiceStreamTransformer();
		transformer.setConfig(conf);
		
		if (conf.hasNonNull("delimiterIn")) {
			transformer.setSegmentDelimiterIn(conf.get("delimiterIn").asText());
		}
		if (conf.hasNonNull("delimiterOut")) {
			transformer.setSegmentDelimiterOut(conf.get("delimiterOut").asText());
		}
		
		
		String supplyMethod = "blob";
		if (conf.hasNonNull("supplyMethod")) {
			supplyMethod = conf.get("supplyMethod").asText();
		}
		if (supplyMethod.equals("cachedFile")) {
			if (conf.hasNonNull("cachePath")) {
				transformer.initCache(conf.get("cachePath").asText());
			} else {
				transformer.initCache(null);
			}
		}
		transformer.setSupplyMethod(supplyMethod);
		
		
		if (conf.hasNonNull("apiURI")) {
			transformer.setApiURI(conf.get("apiURI").asText());
		}
		if (conf.hasNonNull("apiMethodPath")) {
			transformer.setApiMethodPath(conf.get("apiMethodPath").asText());
		}
		if (conf.hasNonNull("apiMethodOperation")) {
			transformer.setApiMethodOperation(conf.get("apiMethodOperation").asText());
		}


		if (conf.hasNonNull("acceptTypes")) {
			Iterator<JsonNode> iter = conf.get("acceptTypes").elements();
			while (iter.hasNext()) {
				JsonNode entry = iter.next();
				transformer.getAcceptTypes().add(entry.asText());
			}
		}

		if (conf.hasNonNull("contentTypes")) {
			Iterator<JsonNode> iter = conf.get("contentTypes").elements();
			while (iter.hasNext()) {
				JsonNode entry = iter.next();
				transformer.getContentTypes().add(entry.asText());
			}
		}
		

		if (conf.hasNonNull("useDataAsParam")) {
			transformer.setUseDataAsParam(conf.get("useDataAsParam").asText());
		}
		if (conf.hasNonNull("useStreamNameAsParam")) {
			transformer.setUseStreamNameAsParam(conf.get("useStreamNameAsParam").asText());
		}
		
		
		if (conf.hasNonNull("pathParams")) {
			Iterator<String> iter = conf.get("pathParams").fieldNames();
			while (iter.hasNext()) {
				String entry = iter.next();
				transformer.getPathParams().put(entry, conf.get("pathParams").get(entry).asText());
			}
		}

		if (conf.hasNonNull("queryParams")) {
			Iterator<String> iter = conf.get("queryParams").fieldNames();
			while (iter.hasNext()) {
				String entry = iter.next();
				transformer.getQueryParams().add(new Pair(entry, conf.get("queryParams").get(entry).asText()));
			}
		}

		if (conf.hasNonNull("collectionQueryParams")) {
			Iterator<String> iter = conf.get("collectionQueryParams").fieldNames();
			while (iter.hasNext()) {
				String entry = iter.next();
				transformer.getCollectionQueryParams().add(new Pair(entry, conf.get("collectionQueryParams").get(entry).asText()));
			}
		}

		if (conf.hasNonNull("headerParams")) {
			Iterator<String> iter = conf.get("headerParams").fieldNames();
			while (iter.hasNext()) {
				String entry = iter.next();
				transformer.getHeaderParams().put(entry, conf.get("headerParams").get(entry).asText());
			}
		}

		if (conf.hasNonNull("formParams")) {
			Iterator<String> iter = conf.get("formParams").fieldNames();
			while (iter.hasNext()) {
				String entry = iter.next();
				transformer.getFormParams().put(entry, conf.get("formParams").get(entry).asText());
			}
		}
		
		return transformer;
	}

	public OpenAPIServiceStreamTransformer buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	public static final String DEFAULT_STREAM_CACHEFILE = "FINTAN_DEFAULT_STREAM_CACHEFILE.zip";
	
	
	private String supplyMethod = "blob";
	private String cachePath = FintanManager.DEFAULT_CACHE_PATH;
	private String cachePathImport = FintanManager.DEFAULT_CACHE_PATH+"import/";
	private String cachePathExport = FintanManager.DEFAULT_CACHE_PATH+"export/";

	//only supported for supplyMethod "blob"
	private String segmentDelimiterIn;
	private String segmentDelimiterOut;

	
	private String apiMethodPath;
	private String apiMethodOperation;
	private String useStreamNameAsParam;
	private String useDataAsParam;
		  
	private List<String> acceptTypes;
	private List<String> contentTypes;
	
	private HashMap<String,String> pathParams;
	private List<Pair> queryParams;
	private List<Pair> collectionQueryParams;
	private HashMap<String,String> headerParams;
	private HashMap<String,String> formParams;
	
	private ApiClient ac;

	public OpenAPIServiceStreamTransformer() {
		acceptTypes = new ArrayList<String>();
		contentTypes = new ArrayList<String>();
		pathParams = new HashMap<String,String>();
		queryParams = new ArrayList<Pair>();
		collectionQueryParams = new ArrayList<Pair>();
		headerParams = new HashMap<String,String>();
		formParams = new HashMap<String,String>();
		
		
	    ac = Configuration.getDefaultApiClient();
	    ac.setConnectTimeout(Integer.MAX_VALUE);
	    ac.setReadTimeout(Integer.MAX_VALUE);
	    ac.setWriteTimeout(Integer.MAX_VALUE);

	}
	
	public void setApiURI(String uri) {
        ac.setBasePath(uri);
	}
	
	public String getApiMethodPath() {
		return apiMethodPath;
	}

	public void setApiMethodPath(String apiMethodPath) {
		this.apiMethodPath = apiMethodPath;
	}
	
	public String getApiMethodOperation() {
		return apiMethodOperation;
	}

	public void setApiMethodOperation(String apiMethodOperation) {
		this.apiMethodOperation = apiMethodOperation;
	}

	public String getUseStreamNameAsParam() {
		return useStreamNameAsParam;
	}

	public void setUseStreamNameAsParam(String useStreamNameAsParam) {
		this.useStreamNameAsParam = useStreamNameAsParam;
	}

	public String getUseDataAsParam() {
		return useDataAsParam;
	}

	public void setUseDataAsParam(String useDataAsParam) {
		this.useDataAsParam = useDataAsParam;
	}
	

	public List<String> getAcceptTypes() {
		return acceptTypes;
	}

	public List<String> getContentTypes() {
		return contentTypes;
	}

	public HashMap<String, String> getPathParams() {
		return pathParams;
	}

	public List<Pair> getQueryParams() {
		return queryParams;
	}

	public List<Pair> getCollectionQueryParams() {
		return collectionQueryParams;
	}

	public HashMap<String, String> getHeaderParams() {
		return headerParams;
	}

	public HashMap<String, String> getFormParams() {
		return formParams;
	}

	public String getCachePath() {
		return cachePath;
	}
	

	public String getSegmentDelimiterIn() {
		return segmentDelimiterIn;
	}

	public void setSegmentDelimiterIn(String segmentDelimiter) {
		this.segmentDelimiterIn = segmentDelimiter;
	}

	public String getSegmentDelimiterOut() {
		return segmentDelimiterOut;
	}

	public void setSegmentDelimiterOut(String segmentDelimiterOut) {
		this.segmentDelimiterOut = segmentDelimiterOut;
	}

	
	public String getSupplyMethod() {
		return supplyMethod;
	}

	public void setSupplyMethod(String supplyMethod) {
		this.supplyMethod = supplyMethod;
	}

	public String getCachePathImport() {
		return cachePathImport;
	}

	public String getCachePathExport() {
		return cachePathExport;
	}
	
	public void initCache(String path) {
		if (path == null) path = FintanManager.DEFAULT_CACHE_PATH;
		if (!path.endsWith("/")) path+="/";
		File f = new File(path+this.getClass().getName()+this.hashCode()+"/");
		if (f.exists() && f.isDirectory()) {
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				LOG.error("Could not delete directory <"+f.getAbsolutePath()+">. "
						+ "Preexisting data may corrupt the current stream! "
						+ "Error message:"+e);
			}
		}
		f.mkdirs();
		
		this.cachePath = f.getAbsolutePath()+"/";
		this.cachePathImport = this.cachePath+"import/";
		File fsub = new File(cachePathImport);
		fsub.mkdir();
		
		this.cachePathExport = this.cachePath+"export/";
		fsub = new File(cachePathExport);
		fsub.mkdir();
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(f)));
	}
	
	private void processStream() throws Exception {
		
		for (String name:listInputStreamNames()) {
			
			if (getOutputStream(name) == null) {
				LOG.info("Input stream '"+name+"' does not have a corresponding output stream and is thus dropped.");
				continue;
			}

			PrintStream out = new PrintStream(getOutputStream(name));
			
			if (supplyMethod.equals("cachedFile")) {
				//read all inputStreams to cachePathImport
				String cachedObjectPath;
				if (name == FINTAN_DEFAULT_STREAM_NAME) {
					//write default InputStream to DEFAULT_STREAM_CAHCEFILE
					cachedObjectPath = cachePathImport + DEFAULT_STREAM_CACHEFILE;
				} else {
					//write named InputStreams to file of the same name.
					cachedObjectPath = cachePathImport + name;
				}

				File file = new File(cachedObjectPath);

				FileUtils.copyInputStreamToFile(getInputStream(name), file);

				writeResults(callApiWithExceptionLogging(name+file.hashCode(), file, null, null), out);

				LOG.debug(cachedObjectPath);
				LOG.debug("# doc: "+file.getAbsolutePath());
				
				file.delete();

			} else if (supplyMethod.equals("blob")) {
				
				BufferedReader in = new BufferedReader(new InputStreamReader(getInputStream()));
				String segment = "";

				for(String line = in.readLine(); line !=null; line=in.readLine()) {
					if (!line.equals(segmentDelimiterIn)) {
						//if segmentDelimiterIn is null, this is never true --> full stream is read as blob.
						// regular line
						segment+=line+"\n";
					} else {
						// end of segment
						writeResults(callApiWithExceptionLogging(name+segment.hashCode(), segment, null, null), out);
						if (segmentDelimiterOut != null) out.println(segmentDelimiterOut);

						// clear segment cache
						segment = "";
					}
				}
				//final segment in case there is no segmentDelimiter in last row
				writeResults(callApiWithExceptionLogging(name+segment.hashCode(), segment, null, null), out);
			}
			
			out.close();
		}
		
	}
	
	private ApiResponse callApiWithExceptionLogging(String streamName, Object data, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
		try {
			return callApi(streamName, data, progressListener, progressRequestListener);
		} catch (ApiException e) {
			LOG.error("API Error at instance " + this.getInstanceName() + " at stream object " + streamName);
			LOG.error("API Error code: " + e.getCode());
			LOG.error("API Error message: " + e.getMessage());
			LOG.error("API Error header: " + e.getResponseHeaders());
			LOG.error("API Error body: " + e.getResponseBody());
			throw e;
		}
	}
	
	private ApiResponse callApi(String streamName, Object data, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
		Object localVarPostBody = null;

		String localVarPath = getApiMethodPath();

        // create path and map static variables
		for (String var:getPathParams().keySet()) {
			localVarPath = localVarPath.replaceAll("\\{" + var + "\\}", ac.escapeString(getPathParams().get(var)));
		}
		
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        localVarQueryParams.addAll(getQueryParams());
        
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        localVarCollectionQueryParams.addAll(getCollectionQueryParams());

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        localVarHeaderParams.putAll(getHeaderParams());

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();
        localVarFormParams.putAll(getFormParams());
        
        //inject object identifier (stream name) as specified in useStreamNameAsParam
        if (getUseStreamNameAsParam().startsWith("path")) {
        	localVarPath = localVarPath.replaceAll("\\{" + getUseStreamNameAsParam().split(":::")[1] + "\\}", streamName);
        } else if (getUseStreamNameAsParam().startsWith("form")) {
            localVarFormParams.put(getUseStreamNameAsParam().split(":::")[1], streamName);
        } else if (getUseStreamNameAsParam().startsWith("query")) {
        	localVarQueryParams.add(new Pair(getUseStreamNameAsParam().split(":::")[1], streamName));
        } else if (getUseStreamNameAsParam().startsWith("header")) {
        	localVarHeaderParams.put(getUseStreamNameAsParam().split(":::")[1], streamName);
        }
        
        //inject data as specified in useDataAsParam
        if (getUseDataAsParam().startsWith("form")) {
            localVarFormParams.put(getUseDataAsParam().split(":::")[1], data);
        } else if (getUseDataAsParam().startsWith("query")) {
        	localVarQueryParams.add(new Pair(getUseDataAsParam().split(":::")[1], String.valueOf(data)));
        } else if (getUseDataAsParam().startsWith("header")) {
        	localVarHeaderParams.put(getUseDataAsParam().split(":::")[1], String.valueOf(data));
        }

        //inject accept header
        final String[] localVarAccepts = getAcceptTypes().toArray(new String[1]);
        final String localVarAccept = ac.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        //inject content types
        final String[] localVarContentTypes = getContentTypes().toArray(new String[1]);
        final String localVarContentType = ac.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            ac.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {  };
        
        
        com.squareup.okhttp.Call call = ac.buildCall(localVarPath, getApiMethodOperation(), localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
        Type localVarReturnType = new TypeToken<Response>(){}.getType();
        return ac.execute(call, localVarReturnType);
	}
	
	private void writeResults(ApiResponse apiresponse, PrintStream out) {
		LOG.debug("# response: "+apiresponse.getStatusCode());
		LOG.debug("# headers: "+apiresponse.getHeaders());
		
		Response response= (Response)apiresponse.getData();
		LOG.debug("# format: "+response.getFormat());
		out.println(response.getValue());
	}

	public void run() {
		try {
			processStream();	
		} catch (Exception e) {
			LOG.error(e, e);
			System.exit(1);
		}
	}

	@Override
	public void start() {
		run();
	}
}
