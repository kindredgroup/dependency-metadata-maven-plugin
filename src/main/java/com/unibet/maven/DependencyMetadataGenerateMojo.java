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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.IOException;

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
     * If true metadata from upper dependency versions shall be processed.
     */
    @Parameter(property = "dependency.metadata.applyOnPreviousVersions", defaultValue = "false")
    private boolean applyOnPreviousVersions;

    @Component
    private MavenProjectHelper projectHelper;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Metadata metadata = new Metadata();
        metadata.formatVersion = this.formatVersion;
        metadata.message = this.message;
        metadata.fail = this.fail;
        metadata.applyOnPreviousVersions = this.applyOnPreviousVersions;
        // Make sure build directory exists
        new File(project.getBuild().getDirectory()).mkdirs();
        try {
            OBJECT_MAPPER.writeValue(getArtifactFile(), metadata);
        } catch (IOException e) {
            throw new MojoFailureException("Failed creating metadata artifact file " + getArtifactFile(), e);
        }

        logger.info("Metadata artifact generated: {}", getArtifactFile());
    }
}
