[back to top](README.md)

# Build your own custom components

## Decide how to structure your project

There are two ways how to structure your project to be compatible with Fintan.

**Method 1:** use Fintan as a part of your own project. This can be done by adding a Maven dependency for fintan-core or fintan-backend to your Java project. This will allow you to directly use the Fintan API and existing cor or backend components and workflows in your project.

```
<dependency>
	<groupId>org.acoli.fintan</groupId>
	<artifactId>fintan-core</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
```

**Method 2:** create a pull request to the [Fintan Backend](https://github.com/acoli-repo/fintan-backend) including necessary dependencies, a wrapper component and, ideally, an example pipeline.


## Build a wrapper component
Independently from the method of integration, a wrapper component is needed to access your converter in Fintan pipelines. As described in [Software concept](1-software-concept.md) and [Core Components](3a-core-components) Fintan primarily utilizes 4 abstract classes to define its components. In order to build your own custom components it is necessary to at least provide a wrapper class which is a subclass of one of these 4 abstract classes:
`StreamTransformerGenericIO` for processing serialized RDF or generic data
`StreamLoader` to produce segmented RDF streams from serialized or generic data
`StreamRdfUpdater` to transform segmented RDF streams
`StreamWriter` to produce serializations or exports from segmented RDF streams

![Fintan Core class diagram (reduced)](img/core-classes.PNG "Fintan Core class diagram (reduced)")

With this, the following methods are available to your component. 

```
public ObjectNode getConfig() {
	return config;
}

public void setConfig(ObjectNode config) {
	this.config = config;
}

public In getInputStream() {
	return inputStreams.get(FINTAN_DEFAULT_STREAM_NAME);
}

public In getInputStream(String name) {
	return inputStreams.get(name);
}

public void setInputStream(In inputStream) throws IOException {
	this.inputStreams.put(FINTAN_DEFAULT_STREAM_NAME, inputStream);
}

public void setInputStream(In inputStream, String name) throws IOException {
	this.inputStreams.put(name, inputStream);
}

public Out getOutputStream() {
	return outputStreams.get(FINTAN_DEFAULT_STREAM_NAME);
}

public Out getOutputStream(String name) {
	return outputStreams.get(name);
}

public void setOutputStream(Out outputStream) throws IOException {
	this.outputStreams.put(FINTAN_DEFAULT_STREAM_NAME, outputStream);
}

public void setOutputStream(Out outputStream, String name) throws IOException {
	this.outputStreams.put(name, outputStream);
}

public String[] listInputStreamNames() {
	return inputStreams.keySet().toArray(new String[] {});
}

public String[] listOutputStreamNames() {
	return outputStreams.keySet().toArray(new String[] {});
}

public abstract void run(); //from Runnable interface
```

The `run()` method is called by the `FintanManager` when a pipeline is started. Each instance of your component is hereby run as a separate thread, so please take care of concurrent access (esp. to static fields and methods) when using external APIs. The typings of `In` and `Out` depend on the implemented core class. For generic I/O the default Java `InputStream` and `OutputStream` classes are used. For the segmented RDF streams, the class `FintanStreamHandler<Model>` implements a BlockingQueue of Apache Jena Models which is optimized for synchronized read and write operations which are made available to the stream components through the interfaces `FintanInputStream<Model>` and `FintanOutputStream<Model>`:

```
public interface FintanInputStream<T> {
	
	public T read() throws InterruptedException;
	
	public boolean canRead();
	
	public boolean active();
	
	public void terminate();

}
```

```
public interface FintanOutputStream<T> {
	
	public void write(T m) throws InterruptedException;
	
	public boolean canWrite();
	
	public boolean active();

	public void terminate();
}
```

Each component can be supplied with multiple input and output stream slots by the `FintanManager`. One default slot can be accessed by `getInputStream()` and `getOutputStream()` respectively. Multiple named stream slots can be addressed using `getInputStream(String name)` and `getOutputStream(String name)` respectively. If you want to work with multiple RDF graphs inside your components, please make sure that the stream slots internally correspond to graphs of the same name, as this is a general design pattern of Fintan, albeit not enforced. Please also make sure to `close()` or `terminate()` all output streams after the last piece of data is written to a respective stream slot. This will be the signal for subsequent components not to wait for further data any longer. 

Examples of how to work with supplied streams can be taken from any of the classes available in the [four main packages](https://github.com/acoli-repo/fintan-core/tree/master/src/main/java/org/acoli/fintan).

Furthermore, it is possible to `@Override` the I/O setter methods, e.g. in order to disallow specific named streams by throwing an Exception. If such an Exception is thrown, the FintanManager will terminate with the provided error message, before executing the pipeline.

**Important:** all data should be treated as streams in Fintan. While it is definitely possible, and sometimes required to implement additional means of supplying data to a components (e.g. SPARQL/XSL scripts read from disk), it is strongly discouraged for bulk data. Whenever you read from files or web sources, please consider the methods provided in the [util package](https://github.com/acoli-repo/fintan-core/tree/master/src/main/java/org/acoli/fintan/core/util) in order to provide a consistent user experience.

## Build a Factory
Now your wrapper component should be functional. However, the `FintanManager` requires an additional factory interface to be implemented, in order to create instances of your component. 

```
public interface FintanStreamComponentFactory {
	
	public FintanStreamComponent buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException, ParseException;
	public FintanStreamComponent buildFromCLI(String[] args) throws IOException, IllegalArgumentException, ParseException;

}
```


The `buildFromJsonConf` method is mandatory, as it is called by the `FintanManager` and provided with a component configuration object from a JSON pipeline. The `buildFormCLI` method is currently unused, except in CoNLL-RDF, and can just be left to return `null`.

The `FintanManager` uses a set of default packages for easy access to component classes. A class can be referenced inside a JSON pipeline by its local name, if it is part of one of these packages:
* `org.acoli.fintan.genericIO`
* `org.acoli.fintan.load`
* `org.acoli.fintan.rdf`
* `org.acoli.fintan.write`

For different packages, the fully qualified name `package.class` is needed.

Factory classes also underly naming constraints and the Factory interface must either be:
* placed in the same package as the invoked class <`MyComponent`> and accordingly called <`MyComponentFactory`>
or
* implemented by the invoked class itself (so it has a double function). This is done for most core components but might not be the best option for complex classes.


## Integrate your component in a workflow
For creating instances of your converter component inside a pipeline, it is necessary to provide a component configuration for that specific instance. Component configurations always have a `class` entry telling the `FintanManager` which class to invoke. Furthermore, a unique `componentInstance` can be used to directly address a specific instance of your converter. For more details see the [pipelines section](2-run-pipelines.md). 

```
    { 
      "componentInstance" : "converter123",
      "class" : "MyComponent",
      "configEntry1" : "true",
      "configEntry2" : "sth else"
    }
```

The naming of all the other configuration entries is free to choose. The factory method described above will always be supplied with the full component configuration entry, as long as it is referenced either within the `pipeline` or the `components` array of a Fintan JSON configuration. All parsing should be done in the Factory method, except for the two aforementioned parameters `class` and `componentInstance`, which are handled by the `FintanManager`.

Inside a `pipeline`. Default stream slots are connected automatically. For more details see the [pipelines section](2-run-pipelines.md). 

```
{
"input" : "System.in"
, "output" : "System.out"
, "pipeline" : [ 
    { 
      "class" : "MyComponent",
      "configEntry1" : "true",
      "configEntry2" : "sth else"
    }
  ]
, "components" : []
, "streams" : []
}
```

Inside `components`. All stream slots must be connected manually. For more details see the [pipelines section](2-run-pipelines.md). 

```
{
"input" : "System.in"
, "output" : "System.out"
, "pipeline" : []
, "components" : [ 
    { 
      "componentInstance" : "converter123",
      "class" : "MyComponent",
      "configEntry1" : "true",
      "configEntry2" : "sth else"
    }
  ]
, "streams" : []
}
```

**Congratulations! You have now built your own Fintan component!**
