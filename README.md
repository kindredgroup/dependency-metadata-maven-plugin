dependency-metadata-maven-plugin
================================
Multi purpose maven dependency metadata handling plugin designed to resolve metadata for direct and transitive maven project dependencies and act on upon it.

Initial use case for this plugin was an ability to deprecate in house produced libraries, but it's definitely not limited to this.

Metadata is serialized as JSON file.

Goals
-----
* **verify** - Used to resolve and process (verify) metadata
* **generate** - Generates deprecation metadata artifact for the current project
* **deploy** - Deploy existing metadata artifact to remote repository

Examples
--------
Include metadata verification before project compilation:

```
<project>
    ...
    <build>
        ...
        <plugins>
            ...
            <plugin>
                <groupId>com.unibet.maven</groupId>
                <artifactId>dependency-metadata-maven-plugin</artifactId>
                <version>1.0.0</version>
                <executions>
                    <execution>
                        <id>verify-artifacts</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            ...
        </plugins>
        ...
    <build>
    ...
</project>
```

Generate default metadata artifact for the current project:
```
mvn com.unibet.maven:dependency-metadata-maven-plugin:generate
```

Deploy existing metadata artifact to the alternative repository:
```
mvn com.unibet.maven:dependency-metadata-maven-plugin:deploy -DaltDeploymentRepository=my-metadata-repository::default::http://my.repository.com/nexus/content/repositories/metadata
```

TODO
----
* Implement dependency artifact filtering
* Implement remote repository filtering


License
------
Apache 2