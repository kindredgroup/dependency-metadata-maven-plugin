package com.unibet.maven;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public abstract class AbstractDependencyMetadataMojo extends AbstractMojo {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String METADATA_ARTIFACT_TYPE = "json";
    public static final String METADATA_ARTIFACT_CLASSIFIER = "metadata";
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    protected ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}")
    protected List<ArtifactRepository> remoteRepositories;

    @Parameter(property = "dependency.metadata.formatVersion", defaultValue = "1")
    protected int formatVersion;

    @Component
    protected ArtifactFactory artifactFactory;

    @Component
    protected ArtifactResolver artifactResolver;

    @Component
    protected ArtifactMetadataSource artifactMetadataSource;
    private File artifactFile;

    public File getArtifactFile() {
        if (null == artifactFile) {
            String filename = project.getArtifactId() + "-" + project.getVersion() +
                    "-" + METADATA_ARTIFACT_CLASSIFIER + "." + METADATA_ARTIFACT_TYPE;
            artifactFile = new File(project.getBuild().getDirectory() + File.separator + filename);
        }
        return artifactFile;
    }
}
