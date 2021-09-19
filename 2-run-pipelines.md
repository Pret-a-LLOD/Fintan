[back to top](README.md)

# Develop and run pipelines
In the previous section we described the general architecture of Fintan and the layout of its repositories. Now we will focus on how to actually write transformation pipelines and how these look like in both the JSON configuration files, as well as the user interface.

## JSON configurations and I/O
For writing pipelines, Fintan uses an extended version of the JSON configuration schema used for CoNLL-RDF. Simple pipelines with a single stream of data can still adhere to the original format. Fintan is thus backward compatible with all existing CoNLL-RDF pipelines:

* Define `input` as a path or URI to read from. Optionally, `System.in` reads from shell input.
* Define `output` as a path to write to. Optionally, `System.out` writes to the shell’s standard out.
* Define `pipeline` as a list of configured components. The parameters are specific for each component. The output of one component is used as the input for the subsequent component. The first component will read from input, the last component will write to output. 

The following example shows a pipeline which reads unsegmented RDF data from a file, splits it into segments and writes Turtle format with segment delimiters to the standard shell output.

```
{
"input" : "en-ud-train.conllu.gz.linked.ttl"
, "output" : "System.out"
, "pipeline" : [ 
    { 
      "class" : "RDFStreamSplitter",
      "lang" : "ttl",
      "iteratorQuery" : "iterate_sentences.sparql",
      "constructQuery" : "construct_sentences.sparql",
      "tdbPath" : null
    },
    { 
      "class" : "RDFStreamWriter",
      "lang" : "ttl",
      "delimiter" : "##TEST#OUTPUT##"
    }
  ]
}
```

In contrast to CoNLL-RDF, Fintan not only allows for a single stream to be processed, but is able to split and merge streams. Hereby, each FintanStreamComponent allows for an indefinite amount of input and output stream slots which are designed to correspond to a specific graph for data processing. Like with RDF graphs, there is a default stream slot and multiple named stream slots, which should follow a valid URI schema, at least for components which directly process RDF data (Updaters, the output of Loaders, and the input of Writers).

To accommodate multiple streams, two additional arrays were added to the configurations schema:
* The `components` array is a list of isolated instances of FintanStreamComponent. The general syntax is like the original pipeline array, with the only difference being that neither component’s default stream slot is linked. It is furthermore required that each instance gets an additional unique componentInstance identifier, so it can be addressed from within the streams array. The identifier is optional for the original pipeline array.
* The `streams` array in turn connects the components by using the following attributes: 
    * `readsFromInstance`: the identifier of the componentInstance to read from.
    * `readsFromInstanceGraph`: the output stream slot / graph //of the componentInstance to read from. `null` or undefined corresponds to the default graph / slot
    * `readsFromSource`: can be a path/URL or `System.in` (same as input, excludes the other two read options)
    * `writesToInstance`: the identifier of the componentInstance to read from.
    * `writesToInstanceGraph`: the input stream slot / graph of the componentInstance to write to. `null` or undefined corresponds to the default graph / slot
    * `writesToDestination`: can be a path to a file or `System.out` (same as output, excludes the other two write options)

The following example pipeline, like the previous example, also splits unsegmented RDF data into segments, but with two major differences:
* It does not apply the iterate-select method of the RDFStreamSplitter, but rather uses the recursive-update method which reads data from a set of input graphs and constructs the output in specified output graphs. This requires named stream slots to be connected to the respective graphs.
* It therefore uses the components and streams arrays to define the pipeline and connect the named stream slots. Input and Output remain the same.

```
{
"components" : [ 
    { 
     "componentInstance" : "sentenceSplitter",
     "class" : "RDFStreamSplitter",
     "lang" : "ttl",
     "initUpdate" : "update_init_construct_sentences.sparql",
     "recursiveUpdate" : "update_rec_construct_sentences.sparql",
     "tdbPath" : null
    },
    { 
      "componentInstance" : "ttlWriter",
      "class" : "RDFStreamWriter",
      "lang" : "ttl",
      "delimiter" : "##TEST#OUTPUT##"
    }
  ]
, "streams" : [
    {
      "readsFromSource" : "en-ud-train.conllu.gz.linked.ttl",
      "writesToInstance" : "sentenceSplitter",
      "writesToInstanceGraph" : "http://input"
    },
    {
      "readsFromInstance" : "sentenceSplitter",
      "readsFromInstanceGraph" : "http://output",
      "writesToInstance" : "ttlWriter"
    },
    {
      "readsFromInstance" : "ttlWriter",
      "writesToDestination" : "System.out"
    }
  ]
}
```

It is possible to mix both configuration syntaxes, which may be useful e.g. for many pipelines which only use the default streams, but pipe out intermediate results to files. For this case, the main pipeline will stay within the pipeline array, but additional streams or components can be added and connected to it as well.

## Running pipelines with parameters
Since Fintan is designed to work both within integrated containers and as a CLI tool to be integrated in existing complex workflows, the FintanManager and the JSON configurations additionally support parameterization. Within any preconfigured pipeline, it is possible to define wildcards for command line arguments which will be replaced during runtime:
* `<$param0>` for the first command line argument
* `<$param1>` for the second command line argument
...

The following section of the Apertium pipeline directly passes command line arguments to the respective XSL scripts (for more details, please refer to section 3):

```
  "components" : [
    ...
    { 
    "componentInstance" : "dix2src",
    "class" : "XSLTStreamTransformer",
    "xsl" : "dix2src-ttl.xsl LANG=<$param1> dc_source=<$param0>"
    },
    { 
      "componentInstance" : "dix2tgt",
      "class" : "XSLTStreamTransformer",
      "xsl" : "dix2tgt-ttl.xsl LANG=<$param2> dc_source=<$param0>"
    },
    ...
```

To run a Fintan pipeline on the command line, you only need to adhere to the following steps, which are also described in the Fintan backend repository:


* git clone the Fintan backend:

```
git clone https://github.com/acoli-repo/fintan-backend.git
```

* build the backend with all its dependencies:

```
cd fintan-backend/
. build.sh
```

* run the pipeline above:

```
run.sh -c apertium.json -p https://someURI en es
```

The -c option defines the JSON configuration, while the -p option defines the parameters.

## Workflow manager
