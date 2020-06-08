# TODO

* ``-XX:-UsePerfData -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:+UseSerialGC -Xmx16m``

```
mvn -pl bootstrap-cli clean package -DskipTests && rm -rf java-light/ && $JAVA_HOME/bin/jlink --no-header-files --no-man-pages --compress=0 --strip-debug --module-path bootstrap-cli/target/bootstrap-cli-0.0.1-SNAPSHOT.jar:bootstrap-cli/target/dependency/ --add-modules org.likide.bootstrap --verbose --output java-light --launcher bootstrap=org.likide.bootstrap/org.likide.bootstrap.Bootstrap --strip-java-debug-attributes --add-options="-XX:-UsePerfData -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:+UseSerialGC -Xmx16m" && time ./java-light/bin/bootstrap --miniconda-url http://fake
```
time ./java-light/bin/java -XX:DumpLoadedClassList=loaded.lst -m org.likide.bootstrap/org.likide.bootstrap.Bootstrap --help
time ./java-light/bin/java --add-modules org.likide.bootstrap,info.picocli,org.apache.logging.log4j,org.apache.logging.log4j.core,org.apache.logging.log4j.slf4j -Xshare:dump -XX:SharedClassListFile=loaded.lst -XX:SharedArchiveFile=app-cds.jsa
time ./java-light/bin/java -Xshare:on -XX:SharedArchiveFile=app-cds.jsa -XX:-UsePerfData -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:+UseSerialGC -Xmx3M -m org.likide.bootstrap/org.likide.bootstrap.Bootstrap
-- -Dorg.apache.logging.log4j.simplelog.StatusLogger.level=ALL


```
mvn -pl bootstrap-cli clean package -DskipTests \
  && rm -rf java-light/ \
  && $JAVA_HOME/bin/jlink --no-header-files --no-man-pages --compress=0 --strip-debug --module-path bootstrap-cli/target/bootstrap-cli-0.0.1-SNAPSHOT.jar:bootstrap-cli/target/dependency/ --add-modules org.likide.bootstrap,org.likide.bootstrap,org.likide.bootstrap.tinylog --verbose --output java-light --launcher bootstrap=org.likide.bootstrap/org.likide.bootstrap.Bootstrap --strip-java-debug-attributes --add-options="-XX:-UsePerfData -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:+UseSerialGC -Xmx16m" \
  && time ./java-light/bin/bootstrap --miniconda-url http://fake
```