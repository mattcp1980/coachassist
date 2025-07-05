import * as pulumi from "@pulumi/pulumi";
import * as gcp from "@pulumi/gcp";

// Get configuration values
const gcpConfig = new pulumi.Config("gcp");
const stackConfig = new pulumi.Config();

const location = gcpConfig.require("region");
const projectId = gcpConfig.require("project");
const repoName = stackConfig.get("repoName") || "coach-assist-repo";
const serviceName = stackConfig.get("serviceName") || "coach-assist-service";

// Enable required Google Cloud services
const cloudrunApi = new gcp.projects.Service("cloudrun-api", {
    service: "run.googleapis.com",
});

const artifactRegistryApi = new gcp.projects.Service("artifact-registry-api", {
    service: "artifactregistry.googleapis.com",
});

const vertexAiApi = new gcp.projects.Service("vertex-ai-api", {
    service: "aiplatform.googleapis.com",
});

const firestoreApi = new gcp.projects.Service("firestore-api", {
    service: "firestore.googleapis.com",
});

// Provision Firestore database
const database = new gcp.firestore.Database("database", {
    locationId: location,
    type: "FIRESTORE_NATIVE",
}, { dependsOn: [firestoreApi] });

// Create Artifact Registry repository
const repository = new gcp.artifactregistry.Repository(repoName, {
    repositoryId: repoName,
    format: "DOCKER",
    location: location,
    description: "Docker repository for CoachAssist service",
}, { dependsOn: [artifactRegistryApi] });

// Define image name with dynamic tag support
const imageTag = stackConfig.get("imageTag") || "latest";
const imageName = pulumi.interpolate`${location}-docker.pkg.dev/${projectId}/${repoName}/${serviceName}:${imageTag}`;

// Create deployment timestamp to force updates
const deploymentTime = new Date().toISOString();

// Create custom service account
const serviceAccount = new gcp.serviceaccount.Account("coach-assist-sa", {
    accountId: "coach-assist-sa",
    displayName: "Coach Assist Cloud Run Service Account",
});

// Grant Firestore access
const firestoreAccess = new gcp.projects.IAMMember("coach-assist-sa-firestore", {
    project: projectId,
    role: "roles/datastore.user",
    member: pulumi.interpolate`serviceAccount:${serviceAccount.email}`,
});

// Grant Vertex AI access for Gemini
const vertexAiAccess = new gcp.projects.IAMMember("coach-assist-sa-vertex", {
    project: projectId,
    role: "roles/aiplatform.user",
    member: pulumi.interpolate`serviceAccount:${serviceAccount.email}`,
});

// Create Cloud Run service
const service = new gcp.cloudrun.Service(serviceName, {
    location: location,
    template: {
        spec: {
            serviceAccountName: serviceAccount.email,
            containers: [{
                image: imageName,
                ports: [{ containerPort: 8080 }],
                envs: [
                    {
                        name: "GEMINI_API_KEY",
                        value: stackConfig.requireSecret("geminiApiKey"),
                    },
                    {
                        name: "GOOGLE_CLOUD_PROJECT",
                        value: projectId,
                    },
                    {
                        name: "GRPC_VERBOSITY",
                        value: "ERROR",
                    },
                    // PORT is automatically set by Cloud Run - DO NOT include it here
                ],
                resources: {
                    limits: {
                        memory: "1Gi",
                        cpu: "1000m",
                    },
                    requests: {
                        memory: "512Mi",
                        cpu: "500m",
                    },
                },
                livenessProbe: {
                    httpGet: {
                        path: "/health",
                        port: 8080,
                    },
                    initialDelaySeconds: 30,
                    timeoutSeconds: 10,
                    periodSeconds: 30,
                    failureThreshold: 3,
                },
                startupProbe: {
                    httpGet: {
                        path: "/health",
                        port: 8080,
                    },
                    initialDelaySeconds: 10,
                    timeoutSeconds: 10,
                    periodSeconds: 10,
                    failureThreshold: 18,
                },
            }],
            timeoutSeconds: 300,
            containerConcurrency: 80,
        },
        metadata: {
            annotations: {
                "run.googleapis.com/startup-cpu-boost": "true",
                "run.googleapis.com/execution-environment": "gen2",
                "run.googleapis.com/cpu-throttling": "false",
                "autoscaling.knative.dev/minScale": "0",
                "autoscaling.knative.dev/maxScale": "10",
                "app.deploy-time": deploymentTime,
                "app.image-tag": imageTag,
            },
        },
    },
}, { dependsOn: [cloudrunApi, vertexAiApi, database, firestoreAccess, vertexAiAccess] });

// Allow public access
const iamMember = new gcp.cloudrun.IamMember("iam-public-access", {
    location: location,
    project: projectId,
    service: service.name,
    role: "roles/run.invoker",
    member: "allUsers",
}, { dependsOn: [service] });

// Export useful values
export const serviceUrl = service.statuses.apply(statuses => {
    if (statuses && statuses.length > 0 && statuses[0].url) {
        return statuses[0].url;
    }
    return "";
});

export const serviceAccountEmail = serviceAccount.email;
export const repositoryUrl = repository.name;
export const imageNameOutput = imageName;
export const deploymentTimestamp = deploymentTime;
export const currentImageTag = imageTag;