# Fintan Loaders

external loader components for future integration within Fintan

Note that the formal integration of externally developed loaders into the architecture and the frontend will require some adjustments, and then, the corresponding GitHub submodule should be removed here. 
Without additional code integration, external loader components can be combined with the Fintan backend in shell scripts, e.g., on a Unix/Linux bash shell:

	$> cd llodifier/unimorph
	$> bash -e ./unimorph2lemon.sh data/sqi.gz | \
	   ../../../backend/run.sh CoNLLRDFUpdater WITH-USER-PARAMETERS

This way of execution requires that the corresponding loader script writes into the standard output stream, and that it does not create a new file, instead. Before a loader can be formally integrated with Fintan, this needs to be confirmed.

Aside from loader components listed here, the Fintan backend natively supports tabular formats (TSV, CSV via CoNLL-RDF) with extensions for XML markup (CoNLL-RDF tree extensions).

## External loaders

- LLODifier, see [llodifier/*/](https://github.com/acoli-repo/LLODifier)
- CoNLL-Merge converters, see [conll-merge/cmd/](https://github.com/acoli-repo/conll-merge/tree/master/cmd)
- XML2CoNLL, see [xml2conll](https://github.com/acoli-repo/xml2conll)
- TBX2RDF, see [tbx2rdf](https://github.com/cimiano/tbx2rdf.git)
