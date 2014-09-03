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

import com.unibet.maven.domain.Metadata;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * <p>This mojo will try to resolve and process metadata artifacts for all direct and transitive (if defined)
 * dependencies. Considering {@link com.unibet.maven.DependencyMetadataGenerateMojo#applyOnPreviousVersions}
 * feature all upper dependency versions shall be considered as well.</p>
 *
 * @author Ilja Bobkevic <ilja.bobkevic@unibet.com>
 */
@Mojo(name = "verify", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class DependencyMetadataVerifyMojo extends AbstractDependencyMetadataMojo {

    /**
     * Download transitively, retrieving the specified artifact and all of its dependencies.
     */
    @Parameter(property = "dependency.metadata.transitive", defaultValue = "false")
    private boolean transitive;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Artifact> dependencies = getDependencies(transitive);

        for (Artifact dependencyArtifact : dependencies) {
            Artifact metadataArtifact = artifactFactory.createArtifactWithClassifier(dependencyArtifact.getGroupId(),
                    dependencyArtifact.getArtifactId(), dependencyArtifact.getVersion(), METADATA_ARTIFACT_TYPE,
                    METADATA_ARTIFACT_CLASSIFIER);
            try {
                resolver.resolve(metadataArtifact, remoteRepositories, localRepository);
                logger.debug("Artifact {} found", metadataArtifact);
                Metadata metadata = parseMetadataJson(metadataArtifact.getFile());
                if (metadata.formatVersion == this.formatVersion) {
                    if (metadata.fail) {
                        logger.error("------------------------------------------------------------------------");
                        logger.error("Metadata source: {}", metadataArtifact);
                        logger.error("{}" + metadata.message);
                        logger.error("------------------------------------------------------------------------");
                        throw new MojoFailureException("There were dependency metadata failures");
                    } else {
                        logger.warn("------------------------------------------------------------------------");
                        logger.warn("Metadata source: {}", metadataArtifact);
                        logger.warn("{}" + metadata.message);
                        logger.warn("------------------------------------------------------------------------");
                    }
                }
            } catch (ArtifactResolutionException e) {
                throw new MojoExecutionException("Failed resolving metadata artifact " + metadataArtifact, e);
            } catch (ArtifactNotFoundException e) {
                logger.debug("Artifact {} NOT found", metadataArtifact);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Artifact> getDependencies(boolean transitive) throws MojoExecutionException {
        Set<Artifact> artifacts;
        try {
            artifacts = project.createArtifacts(this.artifactFactory, null, null);
            if (transitive) {
                ArtifactResolutionResult arr = artifactResolver.resolveTransitively(artifacts, project.getArtifact(),
                        project.getManagedVersionMap(), localRepository, remoteRepositories, artifactMetadataSource);
                artifacts.addAll(arr.getArtifacts());
            }
        } catch (InvalidDependencyVersionException | ArtifactNotFoundException | ArtifactResolutionException e) {
            throw new MojoExecutionException("Failed getting project dependencies");
        }
        return artifacts;
    }

    private Metadata parseMetadataJson(File file) throws MojoExecutionException {
        Metadata metadata;
        try {
            metadata = OBJECT_MAPPER.readValue(file, Metadata.class);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed parsing metadata file " + file, e);
        }
        return metadata;
    }
}
