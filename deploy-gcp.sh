#!/bin/bash
# A script to build and deploy the Ktor service to Google Cloud Run.

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
if [ -z "$1" ]; then
  echo "Usage: $0 <gcp-project-id>"
  echo "Example: $0 my-gcp-project-123"
  exit 1
fi

export PROJECT_ID="$1"
export REGION="europe-west1" # Example region, choose one close to you.

# The GCS bucket to store Pulumi state. MUST be globally unique.
# See documentation for instructions on creating this bucket.
# IMPORTANT: Change this to the bucket name you created in Step 1.
export PULUMI_STATE_BUCKET="coach-assist-pulumi-state-2025"

# --- Service Configuration ---
export REPO_NAME="coach-assist-repo"
export SERVICE_NAME="coach-assist-service"

# --- Derived Variables (do not change) ---
export IMAGE_TAG="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}/${SERVICE_NAME}:latest"

# --- 1. Provision Infrastructure (pre-build) ---
echo "--- Ensuring infrastructure exists with Pulumi ---"
# This first run creates the Artifact Registry repository.
(cd infrastructure && \
    echo "--- Installing infrastructure dependencies ---" && \
    npm install && \
    # Unset a potentially conflicting environment variable to ensure --local works reliably.
    unset PULUMI_BACKEND_URL && \
    # Log in to the GCS backend to store state remotely.
    pulumi login "gs://${PULUMI_STATE_BUCKET}" && \
    pulumi stack select dev --create && \
    pulumi config set gcp:project "${PROJECT_ID}" && \
    pulumi config set gcp:region "${REGION}" && \
    pulumi config set repoName "${REPO_NAME}" && \
    pulumi config set serviceName "${SERVICE_NAME}" && \
    pulumi config set deployService false && \
    pulumi up --yes)

# --- 2. Build the Application ---
echo "--- Building the backend application JAR ---"
(cd backend && ./gradlew clean shadowJar)

# --- 3. Build and Push Docker Image ---
echo "--- Authenticating Docker with Artifact Registry ---"
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --project="${PROJECT_ID}"

echo "--- Pruning Docker build cache to ensure a fresh build ---"
docker system prune -af

echo "--- Building and pushing the Docker image: ${IMAGE_TAG} ---"
docker build --no-cache -t "${IMAGE_TAG}" -f backend/Dockerfile .
docker push "${IMAGE_TAG}"

# --- 4. Final Deployment ---
echo "--- Updating Cloud Run service with new image via Pulumi ---"
# This second run will update the Cloud Run service with the new image we just pushed.
(cd infrastructure && \
    pulumi config set deployService true && \
    pulumi up --yes --skip-preview)

echo "--- Deployment complete! ---"