# Case studies for the application of Fintan/CoNLL-RDF technology

As tab-separated values (TSV) formats are particularly important in language technology, we provide a specialized Fintan customization for these, [CoNLL-RDF](https://github.com/acoli-repo/conll-rdf).
The case studies listed below have either been implemented as generic Fintan pipelines or with CoNLL-RDF, but they are all runnable within Fintan.

> Note: One main difference between Fintan and CoNLL-RDF functionalities is that Fintan supports processing non-TSV data. 
> While the processing of XML data, for example, has to be performed as part of preprocessing in CoNLL-RDF pipelines (which thus require wrapper scripts), 
it can be directly integrated into a Fintan workflow. Older use cases normally provide wrapper scripts, but we recommend to implement 
them as Fintan workflows as these are more portable.

## Contents

  * [Simple Conversion, Linking and Labelling tasks](#simple-conversion-linking-and-labelling-tasks)
  * [Creating Annotations (Syntax)](#creating-annotations-syntax)
  * [Merging Annotations (Semantics)](#merging-annotations-semantics)
  * [Querying Annotations](#querying-annotations)

## Simple Conversion, Linking and Labelling tasks

- Apertium Dictionaries
  - conversion of 50+ bilingual dictionaries to OntoLex-Lemon, normalization of their annotations and export as TIAD-TSV
  - data and code: https://github.com/acoli-repo/acoli-dicts/tree/master/stable/apertium
  - scripts: [wrapper script](https://github.com/acoli-repo/acoli-dicts/blob/master/stable/apertium/build.sh), [XSLT](https://github.com/acoli-repo/acoli-dicts/blob/master/stable/apertium/dix2trans-ttl.xsl), [SPARQL](https://github.com/acoli-repo/acoli-dicts/blob/master/stable/apertium/ontolex2tsv.sparql)

- CoNLL-RDF examples
  - various transformation between different formats
  - [data and scripts](https://github.com/acoli-repo/conll-rdf/tree/master/examples)

## Creating Annotations (Syntax)

Fintan allows to implement rule-based graph transformation and enrichment with external resources sich as dictionaries. As illustrators, we implemented a number of rule-based, non-lexicalized parsers. While rule-based parsing is considered to be largely superseded by machine learning methods, it is without alternatives in scenarios where no training data exists, e.g., for historical and low-resource languages.

- Baumbank Mittelhochdeutsch (syntactic annotation of the Reference Corpus Middle High German)
  - rule-based annotation
  - data: https://github.com/acoli-repo/germhist/tree/master/ReM/full_corpus
  - scripts: [wrapper script](https://github.com/acoli-repo/germhist/blob/master/ReM/Makefile), [SPARQL](https://github.com/acoli-repo/germhist/tree/master/ReM/res/sparql)
  
- Eletronic Text Corpus of Syntactically Annotated Neo-Sumerian
  - rule-based annotation
  - resulting data: https://github.com/cdli-gh/ETCSANS
    (note that this data results from merging the results of several techniques, one of them was rule-based annotation with SPARQL, another one was annotation projection)
  - [scripts](https://github.com/cdli-gh/mtaac_syntax_pipeline)

## Merging Annotations (Semantics)

Aside from transforming simple annotations to more complex annotations (syntactic parsing), Fintan also allows to integrate divergent annotations, e.g., the output of *different* annotation tools, into a consistent and potentially richer output representation.

- first corpus for Role and Reference Grammar (RRG Corpus)
  - Role and Reference Grammar is a cross-linguistically applicable semantics-based framework for syntactic annotation, with special focus on argument linking 
  - The corpus was created by aligning, decomposing and re-integrating semantic annotations from PropBank and syntactic annotations from the Universal Dependencies  
  - data: https://github.com/acoli-repo/conll-rdf
  - scripts: https://github.com/acoli-repo/RRG/blob/master/release/scripts-v0.14.zip

## Querying Annotations

With Fintan/CoNLL-RDF, SPARQL can be used to perform corpus queries. This has been the basis for an experimental corpus management system, that generates SPARQL queries from the more established CQP (Corpus Query Processor) query language:

- CQP4RDF
  - [tutorial](https://github.com/acoli-repo/cqp4rdf/blob/master/QueryGeneratorTutorial.md) 
  - Note that this does not directly use Fintan for running the SPARQL query. However, it *requires* CoNLL-RDF data as data basis, and the CoNLL-RDF costumization of Fintan is the reference implementation for producing such data.

