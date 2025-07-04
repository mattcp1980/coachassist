package com.coachassist.infra

import com.pulumi.Pulumi
import com.pulumi.core.Output
import com.pulumi.gcp.artifactregistry.Repository
import com.pulumi.gcp.artifactregistry.RepositoryArgs
import com.pulumi.gcp.cloudrun.Service
import com.pulumi.gcp.cloudrun.ServiceArgs
import com.pulumi.gcp.cloudrun.inputs.ServiceTemplateArgs
import com.pulumi.gcp.cloudrun.inputs.ServiceTemplateSpecArgs
import com.pulumi.gcp.cloudrun.inputs.ServiceTemplateSpecContainerArgs
import com.pulumi.gcp.cloudrun.inputs.ServiceTemplateSpecContainerPortArgs
import com.pulumi.gcp.cloudrun.IamMember
import com.pulumi.gcp.cloudrun.IamMemberArgs
import com.pulumi.Config
import com.pulumi.gcp.projects.Service as ProjectService
import com.pulumi.gcp.projects.ServiceArgs as ProjectServiceArgs

fun main() {
    Pulumi.run { ctx ->
        val gcpConfig = Config.of("gcp")
        val stackConfig = Config.of()
        val deployService = stackConfig.getBoolean("deployService").orElse(false)

        val location = gcpConfig.require("region")
        val projectId = gcpConfig.require("project")
        val repoName = stackConfig.require("repoName")
        val serviceName = stackConfig.require("serviceName")

        val cloudrunApi = ProjectService("cloudrun-api", ProjectServiceArgs.builder()
            .service("run.googleapis.com")
            .build())
        val artifactRegistryApi = ProjectService("artifact-registry-api", ProjectServiceArgs.builder()
            .service("artifactregistry.googleapis.com")
            .build())

        val repository = Repository(
            repoName,
            RepositoryArgs.builder()
                .repositoryId(repoName)
                .format("DOCKER")
                .location(location)
                .description("Docker repository for CoachAssist service")
                .build(),
            com.pulumi.resources.CustomResourceOptions.builder()
                .dependsOn(artifactRegistryApi)
                .build()
        )

        if (deployService) {
            val imageName = Output.format("%s-docker.pkg.dev/%s/%s/%s:latest", location, projectId, repository.repositoryId(), serviceName)

            val service = Service(
                serviceName,
                ServiceArgs.builder()
                    .location(location)
                    .template(ServiceTemplateArgs.builder()
                        .spec(ServiceTemplateSpecArgs.builder()
                            .containers(ServiceTemplateSpecContainerArgs.builder()
                                .image(imageName)
                                .ports(ServiceTemplateSpecContainerPortArgs.builder()
                                    .containerPort(8080)
                                    .build())
                                .build())
                            .build())
                        .build())
                    .build(),
                com.pulumi.resources.CustomResourceOptions.builder()
                    .dependsOn(cloudrunApi, repository)
                    .build()
            )

            IamMember(
                "iam-public-access",
                IamMemberArgs.builder()
                    .location(location)
                    .project(projectId)
                    .service(service.name())
                    .role("roles/run.invoker")
                    .member("allUsers")
                    .build()
            )

            ctx.export("serviceUrl", service.statuses().applyValue { statuses ->
                if (statuses != null && statuses.isNotEmpty()) statuses[0].url() else ""
            })
        }
    }
}