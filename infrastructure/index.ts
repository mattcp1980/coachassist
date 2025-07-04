import * as pulumi from "@pulumi/pulumi";
import * as gcp from "@pulumi/gcp";

// Get configuration values from the stack.
const gcpConfig = new pulumi.Config("gcp");
const stackConfig = new pulumi.Config();

const deployService = stackConfig.getBoolean("deployService") || false;
const location = gcpConfig.require("region");
const projectId = gcpConfig.require("project");
const repoName = stackConfig.require("repoName");
const serviceName = stackConfig.require("serviceName");

// Enable required Google Cloud services.
const cloudrunApi = new gcp.projects.Service("cloudrun-api", {
    service: "run.googleapis.com",
});

const artifactRegistryApi = new gcp.projects.Service("artifact-registry-api", {
    service: "artifactregistry.googleapis.com",
});

// Create an Artifact Registry repository to store the Docker image.
const repository = new gcp.artifactregistry.Repository(repoName, {
    repositoryId: repoName,
    format: "DOCKER",
    location: location,
    description: "Docker repository for CoachAssist service",
}, { dependsOn: [artifactRegistryApi] });

// Conditionally deploy the Cloud Run service based on the 'deployService' flag.
if (deployService) {
    const imageName = pulumi.interpolate`${location}-docker.pkg.dev/${projectId}/${repository.repositoryId}/${serviceName}:latest`;

    // Create the Cloud Run service, pointing to the image in Artifact Registry.
    const service = new gcp.cloudrun.Service(serviceName, {
        location: location,
        template: {
            spec: {
                containers: [{
                    image: imageName,
                    ports: [{ containerPort: 8080 }],
                }],
            },
        },
    }, { dependsOn: [cloudrunApi, repository] });

    // Create an IAM member to allow unauthenticated (public) access to the service.
    new gcp.cloudrun.IamMember("iam-public-access", {
        location: location,
        project: projectId,
        service: service.name,
        role: "roles/run.invoker",
        member: "allUsers",
    });

    // Export the URL of the deployed service.
    exports.serviceUrl = service.statuses.apply(statuses => statuses[0]?.url || "");
}