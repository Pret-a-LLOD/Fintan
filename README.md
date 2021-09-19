![Fintan](https://github.com/acoli-repo/fintan-doc/blob/main/img/Fintan.PNG)
## Quick start and demo
### Install
```shell
git clone https://github.com/acoli-repo/fintan-backend.git
cd fintan-backend/
. build.sh 
cd samples/xslt/apertium/
. _apertium_demo.sh 

```

### Build problems
```
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-javadoc-plugin</artifactId>
        <configuration>
          <source>8</source>
        </configuration>
        <version>2.10.4</version>
        <executions>
          <execution>
            <id>attach-javadocs</id>
            <goals>
              <goal>jar</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
```
Should you encounter build problems with tarql, possibly javadoc is not installed. In this case, just remove this section from the tarql/pom.xml. Fintan will run without it. Rerun build.sh

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
This repository is being published under multiple licenses. Apache 2.0 is used for native code, see [LICENSE.main](LICENSE.main.txt). CC-BY 4.0 for all data from universal dependencies and SPARQL scripts, see [LICENSE.data](LICENSE.data.txt). The included Apertium data maintains its original copyright, i.e., GNU GPL 3.0, see [LICENSE.data.apertium](LICENSE.data.apertium.txt). All code from external dependencies and submodules adheres to the Licenses specified in the respective source repositories.


Please cite *Fäth C., Chiarcos C., Ebbrecht B., Ionov M. (2020), Fintan - Flexible, Integrated Transformation and Annotation eNgineering. In: Proceedings of the 12th Language Resources and Evaluation Conference. LREC 2020. pp 7212-7221.*.
