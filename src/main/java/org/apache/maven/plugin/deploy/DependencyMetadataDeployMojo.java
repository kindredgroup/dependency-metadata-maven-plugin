package org.apache.maven.plugin.deploy;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.unibet.maven.AbstractDependencyMetadataMojo.METADATA_ARTIFACT_CLASSIFIER;
import static com.unibet.maven.AbstractDependencyMetadataMojo.METADATA_ARTIFACT_TYPE;

/**
 * <p>Mojo used to deploy existing metadata artifact to remote repository.</p>
 * <p>Mostly copied from maven-deploy-plugin {@link org.apache.maven.plugin.deploy.DeployMojo} class</p>
 * <p>Please note that only metadata artifact will be deploy, even excluding project's pom.xml</p>
 *
 * @author Ilja Bobkevic <ilja.bobkevic@unibet.com>
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class DependencyMetadataDeployMojo extends AbstractDeployMojo {

    private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile("(.+)::(.+)::(.+)");

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * Specifies an alternative repository to which the project artifacts should be deployed ( other than those
     * specified in &lt;distributionManagement&gt; ). <br/>
     * Format: id::layout::url
     * <dl>
     * <dt>id</dt>
     * <dd>The id can be used to pick up the correct credentials from the settings.xml</dd>
     * <dt>layout</dt>
     * <dd>Either <code>default</code> for the Maven2 layout or <code>legacy</code> for the Maven1 layout. Maven3 also
     * uses the <code>default</code> layout.</dd>
     * <dt>url</dt>
     * <dd>The location of the repository</dd>
     * </dl>
     */
    @Parameter(property = "altDeploymentRepository")
    private String altDeploymentRepository;

    /**
     * The alternative repository to use when the project has a snapshot version.
     *
     * @see DeployMojo#altDeploymentRepository
     */
    @Parameter(property = "altSnapshotDeploymentRepository")
    private String altSnapshotDeploymentRepository;

    /**
     * The alternative repository to use when the project has a final version.
     *
     * @see DeployMojo#altDeploymentRepository
     */
    @Parameter(property = "altReleaseDeploymentRepository")
    private String altReleaseDeploymentRepository;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Artifact metadataArtifact = artifactFactory.createArtifactWithClassifier(project.getGroupId(),
                project.getArtifactId(), project.getVersion().toString(),
                METADATA_ARTIFACT_TYPE, METADATA_ARTIFACT_CLASSIFIER);

        String filename = project.getArtifactId() + "-" + project.getVersion() +
                "-" + METADATA_ARTIFACT_CLASSIFIER + "." + METADATA_ARTIFACT_TYPE;
        File metadataArtifactFile = new File(project.getBuild().getDirectory() + File.separator + filename);

        ArtifactRepository repo = getDeploymentRepository(project);

        try {
            deploy(metadataArtifactFile, metadataArtifact, repo, getLocalRepository());
        } catch (ArtifactDeploymentException e) {
            throw new MojoExecutionException("Failed deploying metadata artifact!", e);
        }
    }


    private ArtifactRepository getDeploymentRepository(MavenProject project)
            throws MojoExecutionException, MojoFailureException {
        ArtifactRepository repo = null;

        String altDeploymentRepo;
        if (ArtifactUtils.isSnapshot(project.getVersion()) && altSnapshotDeploymentRepository != null) {
            altDeploymentRepo = altSnapshotDeploymentRepository;
        } else if (!ArtifactUtils.isSnapshot(project.getVersion()) && altReleaseDeploymentRepository != null) {
            altDeploymentRepo = altReleaseDeploymentRepository;
        } else {
            altDeploymentRepo = altDeploymentRepository;
        }

        if (altDeploymentRepo != null) {
            getLog().info("Using alternate deployment repository " + altDeploymentRepo);

            Matcher matcher = ALT_REPO_SYNTAX_PATTERN.matcher(altDeploymentRepo);

            if (!matcher.matches()) {
                throw new MojoFailureException(altDeploymentRepo, "Invalid syntax for repository.",
                        "Invalid syntax for alternative repository. Use \"id::layout::url\".");
            } else {
                String id = matcher.group(1).trim();
                String layout = matcher.group(2).trim();
                String url = matcher.group(3).trim();

                ArtifactRepositoryLayout repoLayout = getLayout(layout);

                repo = repositoryFactory.createDeploymentArtifactRepository(id, url, repoLayout, true);
            }
        }

        if (repo == null) {
            repo = project.getDistributionManagementArtifactRepository();
        }

        if (repo == null) {
            String msg =
                    "Deployment failed: repository element was not specified in the POM inside"
                            + " distributionManagement element or in -DaltDeploymentRepository=id::layout::url parameter";

            throw new MojoExecutionException(msg);
        }

        return repo;
    }
}
