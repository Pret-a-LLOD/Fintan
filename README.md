![Fintan](https://github.com/acoli-repo/fintan-doc/blob/main/img/Fintan.PNG)
# Fintan
**Flexible INtegrated Transformation and Annotation eNgineering platform**

The Fintan platform is an effort of combining existing converter frameworks with stream-based graph transformation and a workflow management engine in order to create integrated transformation pipelines for various input and output format. It has been developed to address the challenge of **Transforming language resources and language data** within Task 3.3 of the ([**Prêt-à-LLOD**](https://cordis.europa.eu/project/id/825182/results)) project: Research and Innovation Action of the H2020 programme (ERC, grant agreement 825182).

For more information please refer to the full [**Software Documentation**](https://github.com/acoli-repo/fintan-doc).

![Fintan UI](https://github.com/acoli-repo/fintan-doc/blob/main/img/Fintan-UI.PNG)

## Usage

Clone this repository including sub-modules:

		$> git clone https://github.com/Pret-a-LLOD/Fintan.git --recurse-submodules --remote-submodules

Build the Fintan backend:

		$> cd Fintan/backend/
		$> (. build.sh)
		$> cd ../..

Test the Fintan backend:

		$> cd Fintan/backend/samples/xslt/apertium/
		$> . _apertium_demo.sh

Build the Fintan frontend:

		$> cd Fintan/ui/
		$> npm install
		$> cd ../..

Run the Fintan frontend

		$> (cd Fintan/ui/; npm start &)

When the container is running, use your browser to go to web address: http://localhost:3009

The frontend allows you to configure and export Fintan workflows. These can then be executed by the backend.


## Sub-Repositories
- [**Frontend**](https://github.com/acoli-repo/fintan-ui) for designing Fintan pipelines
- [**Service**](https://github.com/acoli-repo/fintan-service) for running Fintan pipelines inside integrated Docker containers
- [**Backend**](https://github.com/acoli-repo/fintan-backend) for executing Fintan pipelines on the command line
	- includes [**Core API**](https://github.com/acoli-repo/fintan-core) for stream-based graph processing.
	- wraps fully integrated converter components
- [**Documentation**](https://github.com/acoli-repo/fintan-doc)
- external [**Loader** components](loaders) for various formats, partly compatible, but yet to be fully integrated:
	- 9 [LLODifier](https://github.com/acoli-repo/LLODifier) converters for syntax ([TIGER/XML](https://github.com/acoli-repo/LLODifier/tree/master/tiger), [Penn TreeBank format](https://github.com/acoli-repo/LLODifier/tree/master/ptb)), morphology/glossing ([UniMorph](https://github.com/acoli-repo/LLODifier/tree/master/unimorph), [FLEx](https://github.com/acoli-repo/LLODifier/tree/master/flex), [Toolbox](https://github.com/acoli-repo/LLODifier/tree/master/toolbox), [Xigt](https://github.com/acoli-repo/LLODifier/tree/master/xigt)), philological editions ([TEI/XML](https://github.com/acoli-repo/LLODifier/tree/master/tei)) and transcription formats ([ELAN](https://github.com/acoli-repo/LLODifier/tree/master/elan), [Exmaralda](https://github.com/acoli-repo/LLODifier/tree/master/exmaralda))
	- 11 [CoNLL-Merge converters](https://github.com/acoli-repo/conll-merge/tree/master/cmd) for standard NLP tools ([Stanford Core](https://github.com/acoli-repo/conll-merge/blob/master/cmd/stanford-coreNLP2conll.xsl)), syntax ([Penn TreeBank format](https://github.com/acoli-repo/conll-merge/blob/master/cmd/ptb.parse2conll.sh), [PROIEL format](https://github.com/acoli-repo/conll-merge/blob/master/cmd/proiel2conll.xsl)), semantics ([PropBank/NomBank](https://github.com/acoli-repo/conll-merge/tree/master/cmd/propbank2conll),[Semafor SRL](https://github.com/acoli-repo/conll-merge/blob/master/cmd/semafor2conll.xsl)), coreference ([OntoNotes named entity annotations](https://github.com/acoli-repo/conll-merge/blob/master/cmd/ontonotes.name2conll.sh), [OntoNotes coreference annotations](https://github.com/acoli-repo/conll-merge/blob/master/cmd/ontonotes.coref2conll.sh)), transcriptions ([Exmaralda](https://github.com/acoli-repo/conll-merge/blob/master/cmd/exm2conll.xsl)), discourse semantics ([RST Discourse Treebank](https://github.com/acoli-repo/conll-merge/tree/master/cmd/rst2conll), [Penn Discourse Treebank](https://github.com/acoli-repo/conll-merge/tree/master/cmd/pdtb2conll), [Penn Discourse Graphbank](https://github.com/acoli-repo/conll-merge/tree/master/cmd/pdgb2conll)
	- a generic converter for XML-based corpus formats ([XML2CoNLL](https://github.com/acoli-repo/xml2conll))
	- [TBX2RDF](https://github.com/cfaeth/tbx2rdf) forked to work within Fintan, but not stable, yet.
