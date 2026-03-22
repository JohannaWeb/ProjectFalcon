#!/bin/bash

# Deployment script for Juntos Alpha on GKE
set -e

CLUSTER_NAME="${1:-falcon-cluster}"
ZONE="${2:-us-central1-a}"
PROJECT_ID="project-falcon-490804"

echo "Deploying Juntos Alpha to GKE..."
echo "Cluster: $CLUSTER_NAME"
echo "Zone: $ZONE"
echo "Project: $PROJECT_ID"

# Get cluster credentials
echo "Getting cluster credentials..."
gcloud container clusters get-credentials "$CLUSTER_NAME" --zone "$ZONE" --project "$PROJECT_ID"

# Check if kustomize is available
if command -v kustomize &> /dev/null; then
    echo "Using kustomize to deploy..."
    kustomize build . | kubectl apply -f -
else
    echo "Using kubectl to deploy manifests in order..."
    kubectl apply -f namespace.yaml
    kubectl apply -f configmap.yaml
    kubectl apply -f secret.yaml
    kubectl apply -f serviceaccount.yaml
    kubectl apply -f role.yaml
    kubectl apply -f rolebinding.yaml
    kubectl apply -f deployment.yaml
    kubectl apply -f service.yaml
    kubectl apply -f hpa.yaml
    kubectl apply -f networkpolicy.yaml
    kubectl apply -f ingress.yaml
fi

echo ""
echo "Deployment applied!"
echo ""
echo "Checking deployment status..."
kubectl rollout status deployment/juntos-alpha -n juntos --timeout=5m

echo ""
echo "Getting service details..."
kubectl get svc -n juntos

echo ""
echo "Checking ingress..."
kubectl get ingress -n juntos

echo ""
echo "Checking pod status..."
kubectl get pods -n juntos

echo ""
echo "Deployment complete! Access your app at https://34.160.170.172.nip.io"
echo ""
echo "Useful commands:"
echo "   kubectl logs -f deployment/juntos-alpha -n juntos"
echo "   kubectl port-forward svc/juntos-alpha 8080:80 -n juntos"
echo "   kubectl get hpa -n juntos"
