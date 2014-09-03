package com.unibet.maven;

/*
 * Copyright 2014 North Development AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import java.util.List;

/**
 * <p>Abstract for dependency metadata plugin mojos</p>
 *
 * @author Ilja Bobkevic <ilja.bobkevic@unibet.com>
 */
public abstract class AbstractDependencyMetadataMojo extends AbstractMojo {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String METADATA_ARTIFACT_TYPE = "json";
    public static final String METADATA_ARTIFACT_CLASSIFIER = "metadata";
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    protected ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * This variable will be used to determine whether plugin supports received metadata format
     */
    @Parameter(property = "dependency.metadata.formatVersion", defaultValue = "2")
    protected int formatVersion;

    @Component
    protected ArtifactFactory artifactFactory;

    @Component
    protected ArtifactResolver artifactResolver;

    @Component
    protected ArtifactMetadataSource artifactMetadataSource;

    @Component
    protected ArtifactResolver resolver;

}
