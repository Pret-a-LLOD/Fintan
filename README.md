
![Fintan](img/Fintan.PNG)
# Software Documentation
This directory contains the documentation for the Fintan-backend and how to build and run it as a stand-alone software. It furthermore describes how to use Fintan in your own applications or create your own custom modules.

## Table of Contents:

[Quick Start Guide](quick-start.md)

[1. Software concept](1-software-concept.md)

[2. Developing and running pipelines](2-run-pipelines.md)

[3.a Fintan Core Modules](3a-core-modules.md)

[3.b Fintan Backend Modules (External API)](3b-backend-modules.md)


## Authors and Maintainers
* **Christian Fäth** - faeth@em.uni-frankfurt.de
* **Christian Chiarcos** - chiarcos@informatik.uni-frankfurt.de
* **Maxim Ionov** 
* **Leo Gottfried** 

See also the list of [contributors](https://github.com/acoli-repo/fintan-core/graphs/contributors) who participated in this project.

## Reference
* Fäth C., Chiarcos C., Ebbrecht B., Ionov M. (2020), Fintan - Flexible, Integrated Transformation and Annotation eNgineering. In: Proceedings of the 12th Language Resources and Evaluation Conference. LREC 2020. pp 7212-7221.

## Acknowledgments
This repository has been created in context of
* Applied Computational Linguistics ([ACoLi](http://acoli.cs.uni-frankfurt.de))
* Prêt-á-LLOD. Ready-to-use Multilingual Linked Language Data for Knowledge Services across Sectors ([Pret-a-LLOD](https://cordis.europa.eu/project/id/825182/results))
  * Research and Innovation Action of the H2020 programme (ERC, grant agreement 825182)
  * In this project, CoNLL-RDF has been applied/developed/restructured to serve as backend of the Flexible Integrated Transformation and Annotation Engineering ([FINTAN](https://github.com/Pret-a-LLOD/Fintan)) Platform.

## Licenses
The repositories for Fintan are being published under multiple licenses. All native code and documentation falls under an Apache 2.0 licence. [LICENSE.main](LICENSE.main.txt). The examples in the backend repository contain data and some SPARQL scripts from external sources: CC-BY 4.0 for all data from universal dependencies and SPARQL scripts from the CoNLL-RDF repository, see [LICENSE.data](LICENSE.data.txt). The included Apertium data maintains its original copyright, i.e., GNU GPL 3.0, see [LICENSE.data.apertium](LICENSE.data.apertium.txt). Code from external dependencies and submodules is not redistributed with this package but fetched directly from the respective source repositories during build process and thus adheres to the respective Licenses. 

### LICENCE.main (Apache 2.0)
```
├── https://github.com/acoli-repo/fintan-doc/ 
├── https://github.com/acoli-repo/fintan-core/  
├── https://github.com/acoli-repo/fintan-backend/
│	└──[ see exceptions below ]
├── https://github.com/acoli-repo/fintan-service/ 
└── https://github.com/acoli-repo/fintan-ui/ 
```
### LICENCE.data (CC-BY 4.0)
```
└── https://github.com/acoli-repo/fintan-backend/  
	├── samples/conll-rdf/  
	│	└──[ all scripts and data ]
	└── samples/splitter/  
		├── en-ud-dev.conllu.gz.linked.ttl
		├── en-ud-tiny.conllu.gz.linked.ttl
		└── en-ud-train.conllu.gz.linked.ttl
```
### LICENCE.data.apertium (GNU GPL 3.0)
```
└── https://github.com/acoli-repo/fintan-backend/  
	└── samples/xslt/apertium/data
```

Please cite *Fäth C., Chiarcos C., Ebbrecht B., Ionov M. (2020), Fintan - Flexible, Integrated Transformation and Annotation eNgineering. In: Proceedings of the 12th Language Resources and Evaluation Conference. LREC 2020. pp 7212-7221.*.
