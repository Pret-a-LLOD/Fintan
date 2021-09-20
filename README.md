![Fintan](https://github.com/acoli-repo/fintan-doc/blob/main/img/Fintan.PNG)
# Fintan
Flexible INtegrated Transformation and Annotation eNgineering platform

## Software Documentation
[fintan-doc](https://github.com/acoli-repo/fintan-doc)

## Notes

- **Frontend** currently developed under https://github.com/unlp_nuig/teanga (not public)
- **Service** for running Fintan pipelines inside integrated Docker containers
- [**Backend**](https://github.com/acoli-repo/fintan-backend) 
	- includes [**Core API**](https://github.com/acoli-repo/fintan-core) for stream-based graph processing.
	- wraps fully integrated converter components
- [**Documentation**](https://github.com/acoli-repo/fintan-doc)
- external [**Loader** components](loaders) for various formats, partly compatible, but yet to be fully integrated:
	- [CoNLL-RDF](https://github.com/acoli-repo/conll-rdf) contains
		- several **Loaders** (> 20 tabular formats, incl. tabular formats with XML extensions)
		- exemplary **Updater modules** (SPARQL Update scripts)
		- several **Writers** (RDF formats, canonical CoNLL-RDF, tabular formats, Dot/GraphViz)
	- 9 [LLODifier](https://github.com/acoli-repo/LLODifier) converters for syntax ([TIGER/XML](https://github.com/acoli-repo/LLODifier/tree/master/tiger), [Penn TreeBank format](https://github.com/acoli-repo/LLODifier/tree/master/ptb)), morphology/glossing ([UniMorph](https://github.com/acoli-repo/LLODifier/tree/master/unimorph), [FLEx](https://github.com/acoli-repo/LLODifier/tree/master/flex), [Toolbox](https://github.com/acoli-repo/LLODifier/tree/master/toolbox), [Xigt](https://github.com/acoli-repo/LLODifier/tree/master/xigt)), philological editions ([TEI/XML](https://github.com/acoli-repo/LLODifier/tree/master/tei)) and transcription formats ([ELAN](https://github.com/acoli-repo/LLODifier/tree/master/elan), [Exmaralda](https://github.com/acoli-repo/LLODifier/tree/master/exmaralda))
	- 11 [CoNLL-Merge converters](https://github.com/acoli-repo/conll-merge/tree/master/cmd) for standard NLP tools ([Stanford Core](https://github.com/acoli-repo/conll-merge/blob/master/cmd/stanford-coreNLP2conll.xsl)), syntax ([Penn TreeBank format](https://github.com/acoli-repo/conll-merge/blob/master/cmd/ptb.parse2conll.sh), [PROIEL format](https://github.com/acoli-repo/conll-merge/blob/master/cmd/proiel2conll.xsl)), semantics ([PropBank/NomBank](https://github.com/acoli-repo/conll-merge/tree/master/cmd/propbank2conll),[Semafor SRL](https://github.com/acoli-repo/conll-merge/blob/master/cmd/semafor2conll.xsl)), coreference ([OntoNotes named entity annotations](https://github.com/acoli-repo/conll-merge/blob/master/cmd/ontonotes.name2conll.sh), [OntoNotes coreference annotations](https://github.com/acoli-repo/conll-merge/blob/master/cmd/ontonotes.coref2conll.sh)), transcriptions ([Exmaralda](https://github.com/acoli-repo/conll-merge/blob/master/cmd/exm2conll.xsl)), discourse semantics ([RST Discourse Treebank](https://github.com/acoli-repo/conll-merge/tree/master/cmd/rst2conll), [Penn Discourse Treebank](https://github.com/acoli-repo/conll-merge/tree/master/cmd/pdtb2conll), [Penn Discourse Graphbank](https://github.com/acoli-repo/conll-merge/tree/master/cmd/pdgb2conll)
	- a generic converter for XML-based corpus formats ([XML2CoNLL](https://github.com/acoli-repo/xml2conll))
