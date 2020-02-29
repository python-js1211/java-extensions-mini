# Binaries
The "Structurizr for Java" binaries are hosted on [Maven Central](https://repo1.maven.org/maven2/com/structurizr/) and the dependencies for use with Maven, Ivy, Gradle, etc are as follows.

Name                                                  | Description
----------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------
com.structurizr:structurizr-analysis:1.3.4            | Provides analysis capabilities, using reflection on compiled bytecode to find components.
com.structurizr:structurizr-spring:1.3.4              | Extends structurizr-analysis to help find Spring components that correspond to Java types annotated ```@Controller```, ```@RestController```, ```@Component```, ```@Service``` and ```@Repository```, plus those that extend ```JpaRepository```.
com.structurizr:structurizr-annotations:1.3.4         | A very small, standalone, library that allows you to add software architecture hints into your own code.
com.structurizr:structurizr-plantuml:1.3.4            | Provides the ability to export view definitions to PlantUML diagram definitions.
com.structurizr:structurizr-dot:1.3.4                 | Provides the ability to export view definitions to a DOT file, so they can be rendered with graphviz.
com.structurizr:structurizr-websequencediagrams:1.3.4 | Provides the ability to export dynamic view definitions to WebSequenceDiagrams diagram definitions.
com.structurizr:structurizr-adr-tools:1.3.4           | Imports architecture decision records (ADRs) from the adr-tools tooling.