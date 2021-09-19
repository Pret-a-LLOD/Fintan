# Quick start and demo
## Install
```shell
git clone https://github.com/acoli-repo/fintan-backend.git
cd fintan-backend/
. build.sh 
```

## Run an example
```
cd samples/xslt/apertium/
. _apertium_demo.sh 

```

## Build problems
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
