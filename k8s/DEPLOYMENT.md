# Kubernetes Deployment Guide

This directory contains all Kubernetes manifests for deploying Juntos Alpha to GKE.

## Prerequisites

1. **GKE Cluster**: A running GKE cluster (you've already created `falcon-cluster`)
2. **kubectl**: Configured to access your cluster
3. **gke-gcloud-auth-plugin**: Required for kubectl authentication
   ```bash
   gcloud components install gke-gcloud-auth-plugin
   ```
4. **kustomize** (optional): For easier manifest management
5. **cert-manager**: For SSL/TLS certificates (required for Ingress)

## Files Overview

- **namespace.yaml** - Creates the `juntos` namespace
- **configmap.yaml** - Application configuration
- **secret.yaml** - Sensitive data (database credentials, API keys)
- **serviceaccount.yaml** - Service account for RBAC
- **role.yaml** - RBAC permissions
- **rolebinding.yaml** - Binds role to service account
- **deployment.yaml** - Main application deployment
- **service.yaml** - Load balancer service
- **ingress.yaml** - HTTP(S) routing with SSL/TLS
- **hpa.yaml** - Horizontal Pod Autoscaler
- **networkpolicy.yaml** - Network security policies
- **kustomization.yaml** - Kustomize overlay for easy deployment

## Setup Steps

### 1. Install cert-manager (if not already installed)

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.0/cert-manager.yaml
```

### 2. Configure Domain

Edit the Ingress configuration:
```bash
# Update juntos.example.com to your actual domain
sed -i 's/juntos\.example\.com/your-domain.com/g' k8s/ingress.yaml
```

### 3. Update Secrets

Edit `secret.yaml` with production credentials:
```bash
kubectl edit secret juntos-alpha-secrets -n juntos
```

### 4. Update Container Image

Replace `PROJECT_ID` with your GCP project ID in deployment:
```bash
sed -i 's/PROJECT_ID/project-falcon-490804/g' k8s/deployment.yaml
```

Or use kustomize:
```bash
# Update kustomization.yaml with your image tag
kustomize build . | kubectl apply -f -
```

## Deployment

### Option A: Using kubectl directly

```bash
# Deploy all manifests in order
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/serviceaccount.yaml
kubectl apply -f k8s/role.yaml
kubectl apply -f k8s/rolebinding.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/networkpolicy.yaml
kubectl apply -f k8s/ingress.yaml
```

### Option B: Using kustomize

```bash
kustomize build . | kubectl apply -f -
```

### Option C: Apply all at once

```bash
kubectl apply -f k8s/
```

## Verification

### 1. Check deployment status

```bash
# Watch deployment
kubectl rollout status deployment/juntos-alpha -n juntos

# Check pods
kubectl get pods -n juntos
kubectl describe pod <pod-name> -n juntos
```

### 2. Check service

```bash
kubectl get svc -n juntos
kubectl describe svc juntos-alpha -n juntos
```

### 3. Check ingress

```bash
kubectl get ingress -n juntos
kubectl describe ingress juntos-alpha -n juntos

# Check ingress IP (may take a few minutes)
kubectl get ingress -n juntos -w
```

### 4. Check logs

```bash
# Follow logs from deployment
kubectl logs -f deployment/juntos-alpha -n juntos

# Check specific pod
kubectl logs <pod-name> -n juntos
```

### 5. Check health endpoints

```bash
# Port forward to test locally
kubectl port-forward svc/juntos-alpha 8080:80 -n juntos

# In another terminal
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info
```

## Monitoring

### Metrics

The deployment exports Prometheus metrics at `/actuator/prometheus`:

```bash
# Port forward to Prometheus
kubectl port-forward svc/juntos-alpha 8080:80 -n juntos
curl http://localhost:8080/actuator/prometheus
```

### Logs

Structured logs are sent to stdout in ECS format:

```bash
# Stream logs
kubectl logs -f deployment/juntos-alpha -n juntos

# Search logs
kubectl logs deployment/juntos-alpha -n juntos | grep "error"
```

## Scaling

### Manual Scaling

```bash
# Scale to 5 replicas
kubectl scale deployment juntos-alpha --replicas=5 -n juntos
```

### Automatic Scaling (HPA)

The `hpa.yaml` automatically scales between 2-10 replicas based on:
- CPU utilization > 70%
- Memory utilization > 80%

```bash
# Check HPA status
kubectl get hpa -n juntos
kubectl describe hpa juntos-alpha -n juntos
```

## Updates & Rollouts

### Update image

```bash
# Set new image
kubectl set image deployment/juntos-alpha \
  app=gcr.io/project-falcon-490804/juntos-alpha:v1.0.0 \
  -n juntos

# Check rollout status
kubectl rollout status deployment/juntos-alpha -n juntos
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/juntos-alpha -n juntos

# Check rollout history
kubectl rollout history deployment/juntos-alpha -n juntos
```

## Troubleshooting

### Pods not starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n juntos

# Check events
kubectl get events -n juntos --sort-by='.lastTimestamp'
```

### ImagePullBackOff error

```bash
# Verify image exists in registry
gcloud container images list --project=project-falcon-490804

# Check image pull secret if using private registry
kubectl get secret -n juntos
```

### Ingress not working

```bash
# Check cert-manager
kubectl get certificates -n juntos
kubectl describe certificate juntos-alpha-tls -n juntos

# Check ingress controller
kubectl get ingress -n juntos
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller
```

### Network Policy blocking traffic

```bash
# Temporarily disable network policy for debugging
kubectl delete networkpolicy juntos-alpha -n juntos

# After fixing, reapply
kubectl apply -f k8s/networkpolicy.yaml
```

## Cleanup

### Delete all resources

```bash
kubectl delete namespace juntos
```

Or use kustomize:

```bash
kustomize build . | kubectl delete -f -
```

## Additional Resources

- [GKE Documentation](https://cloud.google.com/kubernetes-engine/docs)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [cert-manager Documentation](https://cert-manager.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
