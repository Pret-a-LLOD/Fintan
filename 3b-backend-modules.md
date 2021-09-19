[back to top](README.md)

# Backend modules
These components are not part of the Core API since they rely on additional dependencies apart from Apache Jena. They are managed as additional imports of the Fintan backend and included in the build script.
## CoNLL-RDF
As mentioned in previous sections, CoNLL-RDF is the spiritual predecessor of Fintan and shares many common functionalities, like stream-based graph processing. The software suite encompasses support for corpora in many formats:
* any CoNLL dialect
* Penn treebank-like bracket structures for syntax trees
* mixed XML-TSV formats
* TenTen
* ...

CoNLL-RDF is now also part of the Fintan backend. All classes and modules available in the CoNLL-RDF library are refactored to directly implement the Fintan core architecture. The JSON-configuration scheme is backward compatible with CoNLL-RDF, so no changes to existing configuration files need to be made. They can be directly loaded and processed by the FintanManager class. For instructions on how to use CoNLL-RDF, please refer to the tutorials in the CoNLL-RDF repository and publications referenced there.

## Read generic TSV/CSV data
While CoNLL-RDF in principle does support any kind of TSV data, it is limited to converting it to the CoNLL-RDF data model. Using the Updater this can in principle be transformed into any other data model (e.g. OntoLex-Lemon, as we described for the Universal Morphologies in the previous report D3.2). However, this might not be the most efficient way of converting tabular data beyond the scope of text corpora and tab separated values..

For this reason, Fintan supports an additional way of converting generic TSV data with the TarqlStreamTransformer. This class is implementing a wrapper for the Tarql library which is designed to apply (slightly modified) SPARQL syntax for directly querying tabular input formats. A description of the general syntax and how to write Tarql queries can be found on the Tarql website.

For convenience, multiple input and output streams can be supplied to a single Transformer configuration. In this case the data read from one input stream will be transformed and piped out to the output stream of the same name. Furthermore, the Fintan implementation supports segmented stream processing, in a similar way to the RDFUpdater (although not yet parallelized). These two parameters trigger this functionality
* `delimiterIn` activates the segmented processing, like the delimiter parameters in the core classes, it corresponds to the full text content of a single delimiting line. If it is unspecified or null, the data is processed as bulk.
* `delimiterOut` optionally defines the delimiter line written to the output stream after each segment. It only applies if delimiterIn is set as well.

The other parameters for the TarqlStreamTransformer correspond to the parameters of the Tarql CLI:
query: path to the tarql query. The query must be of `CONSTRUCT` or `DESCRIBE` syntax. The resulting data will always be in Turtle format.
* `delimiterCSV`: delimiting character for the CSV dialect.
* `tabs`: (`true`/`false`) enables TSV support.
* `quoteChar`: for CSV.
* `escapeChar`: for CSV, optional.
* `encoding`: defaults to UTF-8.
* `headerRow`: (`true`/`false`) is used to define column names in Tarql. If no header row is present, the default column names are `?col1` - `?colN`. In segmented processing, Fintan duplicates the header row for each segment.
* `baseIRI`: base IRI for resolving relative IRIs.
* `write-base`: writes @base for Turtle output.
* `dedup`: window size in which to remove duplicate triples.

## XSLT transformation
The XSLTStreamTransformer uses the Saxon HE library to transform XML data using XSL transformation. It is implementing the StreamTransformerGenericIO interface and thus does not directly produce segmented RDF streams. It can be used stand-alone or for preprocessing steps in combination with the Loader or Splitter classes. 

In the current implementation, it supports only a single input and output stream. Therefore, only the default streams can be connected. The XSL scripts for conversion along their respective parameters can be supplied in a JSON configuration as follows:
* `"xsl" : "path/to/script.xsl param1=value1 paramN=valueN"`
* The parameters, values etc. can be passed on from the command line using the <$param0> wildcards as described in [Pipelines](2-run-pipelines.md).
* The general syntax is the same as with the original Saxon CLI tools.

## TBX2RDF
TBX2RDFStreamTransformer

## 