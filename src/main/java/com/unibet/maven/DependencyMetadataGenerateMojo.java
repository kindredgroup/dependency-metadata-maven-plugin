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
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Metadata artifact generation mojo</p>
 *
 * @author Ilja Bobkevic <ilja.bobkevic@unibet.com>
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = false)
public class DependencyMetadataGenerateMojo extends AbstractDependencyMetadataMojo {

    /**
     * Message to be displayed during maven execution
     */
    @Parameter(property = "dependency.metadata.message", defaultValue = "Artifact has been deprecated!" +
            " Please consider updating the version!")
    private String message;

    /**
     * If true maven execution will fail immediately, otherwise only warning will be displayed.
     */
    @Parameter(property = "dependency.metadata.fail", defaultValue = "false")
    private boolean fail;

    /**
     * If true metadata for lower dependency versions will be generated.
     */
    @Parameter(property = "dependency.metadata.applyOnPreviousVersions", defaultValue = "false")
    private boolean applyOnPreviousVersions;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Artifact artifact = project.getArtifact();
        List<ArtifactVersion> versions = new ArrayList<>(1);
        try {
            versions.add(artifact.getSelectedVersion());
        } catch (OverConstrainedVersionException e) {
            throw new MojoFailureException("Failed getting selected version for artifact " + artifact, e);
        }

        if (applyOnPreviousVersions) {
            versions.addAll(getLowerVersions(artifact));
        }

        for (ArtifactVersion version : versions) {
            Artifact metadataArtifact = artifactFactory.createArtifactWithClassifier(artifact.getGroupId(),
                    artifact.getArtifactId(), version.toString(), METADATA_ARTIFACT_TYPE,
                    METADATA_ARTIFACT_CLASSIFIER);
            try {
                logger.debug("Resolving metadata artifact {} in {} and {}", metadataArtifact, remoteRepositories,
                        logger);
                resolver.resolve(metadataArtifact, remoteRepositories, localRepository);
                logger.info("Metadata artifact {} already exists. Skipping...", metadataArtifact);
            } catch (ArtifactResolutionException e) {
                throw new MojoExecutionException("Failed resolving metadata artifact " + metadataArtifact, e);
            } catch (ArtifactNotFoundException e) {
                logger.debug("Metadata artifact {} not found", metadataArtifact);

                String filename = project.getArtifactId() + "-" + version.toString() +
                        "-" + METADATA_ARTIFACT_CLASSIFIER + "." + METADATA_ARTIFACT_TYPE;
                File artifactFile = new File(project.getBuild().getDirectory() + File.separator + filename);

                Metadata metadata = new Metadata();
                metadata.formatVersion = this.formatVersion;
                metadata.message = this.message;
                metadata.fail = this.fail;

                new File(project.getBuild().getDirectory()).mkdirs();
                try {
                    OBJECT_MAPPER.writeValue(artifactFile, metadata);
                } catch (IOException ioe) {
                    throw new MojoFailureException("Failed creating metadata artifact file " + artifactFile, ioe);
                }
                project.addAttachedArtifact(metadataArtifact);
                logger.info("Metadata artifact generated: {}", artifactFile);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<ArtifactVersion> getLowerVersions(Artifact artifact) throws MojoExecutionException {
        List<ArtifactVersion> lowerVersions;
        try {
            List<ArtifactVersion> versions = artifactMetadataSource.retrieveAvailableVersions(artifact,
                    localRepository, remoteRepositories);
            lowerVersions = new ArrayList<>(versions.size());
            for (ArtifactVersion version : versions) {
                if (version.compareTo(artifact.getSelectedVersion()) < 0) {
                    lowerVersions.add(version);
                }
            }
        } catch (OverConstrainedVersionException | ArtifactMetadataRetrievalException e) {
            throw new MojoExecutionException("Failed getting lower versions for " + artifact, e);
        }
        return lowerVersions;
    }
}
