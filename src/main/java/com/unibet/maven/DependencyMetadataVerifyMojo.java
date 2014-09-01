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
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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

    @Component
    private ArtifactResolver resolver;

    /**
     * Download transitively, retrieving the specified artifact and all of its dependencies.
     */
    @Parameter(property = "dependency.metadata.transitive", defaultValue = "false")
    private boolean transitive;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Artifact> dependencies = getDependencies(transitive);

        Iterator<Artifact> artifactIterator = dependencies.iterator();
        while (artifactIterator.hasNext()) {
            Artifact artifact = artifactIterator.next();
            List<ArtifactVersion> versions = getHigherOrEqualVersions(artifact);

            for (ArtifactVersion version : versions) {
                Artifact metadataArtifact = artifactFactory.createArtifactWithClassifier(artifact.getGroupId(),
                        artifact.getArtifactId(), version.toString(), METADATA_ARTIFACT_TYPE, METADATA_ARTIFACT_CLASSIFIER);
                for (ArtifactRepository repository : remoteRepositories) {
                    try {
                        resolver.resolve(metadataArtifact, remoteRepositories, localRepository);
                        logger.debug("Artifact {} found in {}", metadataArtifact, repository.getUrl());
                        Metadata metadata = parseMetadataJson(metadataArtifact.getFile());
                        if (metadata.formatVersion == this.formatVersion &&
                                (metadata.applyOnPreviousVersions || metadataArtifact.getVersion().equals(artifact.getVersion()))) {
                            if (metadata.fail) {
                                logger.error("------------------------------------------------------------------------");
                                logger.error("Metadata source: {}", metadataArtifact);
                                logger.error("{} - " + metadata.message, artifact);
                                logger.error("------------------------------------------------------------------------");
                                throw new MojoFailureException("There were dependency metadata failures");
                            } else {
                                logger.warn("------------------------------------------------------------------------");
                                logger.warn("Metadata source: {}", metadataArtifact);
                                logger.warn("{} - " + metadata.message, artifact);
                                logger.warn("------------------------------------------------------------------------");
                            }
                        }
                    } catch (ArtifactResolutionException e) {
                        throw new MojoExecutionException("Failed resolving metadata artifact " + metadataArtifact, e);
                    } catch (ArtifactNotFoundException e) {
                        logger.debug("Artifact {} NOT found in {}", metadataArtifact, repository.getUrl());
                    }
                }
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

    @SuppressWarnings("unchecked")
    private List<ArtifactVersion> getHigherOrEqualVersions(Artifact artifact) throws MojoExecutionException {
        List<ArtifactVersion> higherVersions;
        try {
            List<ArtifactVersion> versions = artifactMetadataSource.retrieveAvailableVersions(artifact,
                    localRepository, remoteRepositories);
            higherVersions = new ArrayList<>(versions.size());
            for (ArtifactVersion version : versions) {
                if (version.compareTo(artifact.getSelectedVersion()) >= 0) {
                    higherVersions.add(version);
                }
            }
            // Sort to have lower versions first
            Collections.sort(higherVersions);
        } catch (OverConstrainedVersionException | ArtifactMetadataRetrievalException e) {
            throw new MojoExecutionException("Failed getting higher or equal versions for " + artifact, e);
        }
        return higherVersions;
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
