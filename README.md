# BankApp CI/CD for AKS

BankApp is a Spring Boot 3 banking demo with user registration, login, deposits,
withdrawals, transfers, and transaction history. GitHub Actions builds and tests the
application, scans the source, publishes a container image to GitHub Container
Registry (GHCR), and deploys it to Azure Kubernetes Service (AKS).

## Technology

- Java 17 and Spring Boot 3.3
- Spring MVC, Thymeleaf, Spring Security, and Spring Data JPA
- MySQL 8
- Maven, Docker, Kubernetes, GitHub Actions, and AKS

## Run locally

### Prerequisites

- JDK 17 with `JAVA_HOME` pointing to its installation directory
- MySQL 8 running locally
- Maven is optional because the repository includes the Maven Wrapper

Create the database:

```sql
CREATE DATABASE bankappdb;
```

Set the database password for the current PowerShell session:

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
```

Optional environment variables, with their defaults, are:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/bankappdb?useSSL=false&serverTimezone=UTC` | JDBC connection URL |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | none; required | MySQL password |

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080/register`, create an account, then sign in. The first
page after sign-in is `/dashboard`.

Run the automated tests with:

```powershell
.\mvnw.cmd test
```

## Docker

Package the application before building the image. The Dockerfile expects the
packaged JAR in the `app` directory.

```powershell
.\mvnw.cmd package
New-Item -ItemType Directory -Force app
Copy-Item target\bankapp-*.jar app\app.jar
docker build -t bankapp:local .
docker run --rm -p 8080:8080 `
	-e DB_URL="jdbc:mysql://host.docker.internal:3306/bankappdb?useSSL=false&serverTimezone=UTC" `
	-e DB_USERNAME=root `
	-e DB_PASSWORD="your-local-mysql-password" `
	bankapp:local
```

## AKS deployment

The `ds.yml` manifest deploys MySQL, BankApp, and a 5 GiB MySQL persistent volume
claim. It reads the MySQL root password from a Kubernetes secret named
`bankapp-secrets`; no database password is stored in the manifest.

For a manual deployment, create the secret and apply the manifest:

```powershell
kubectl create secret generic bankapp-secrets `
	--from-literal=mysql-root-password="your-strong-password"
kubectl apply -f ds.yml
kubectl set image deployment/bankapp bankapp=ghcr.io/<github-owner>/bankapp:<image-tag>
kubectl rollout status deployment/bankapp --timeout=120s
kubectl get service bankapp-service
```

Replace `<github-owner>` and `<image-tag>` with the GHCR image owner and tag. For a
private GHCR package, configure an `imagePullSecret` on the `bankapp` deployment or
make the package public before deployment.

## GitHub Actions

The workflow at `.github/workflows/cicd.yml` runs on pushes to `main` and performs:

1. Maven compilation and unit tests on Temurin JDK 17.
2. Trivy filesystem and Gitleaks source scans.
3. JAR packaging and GHCR image publishing with `latest` and commit-SHA tags.
4. AKS deployment using the existing `bankapp-secrets` database secret.

Configure these repository secrets before pushing to `main`:

| Secret | Purpose |
| --- | --- |
| `TOKEN_GITHUB` | GitHub token with permission to publish to GHCR |
| `AZURE_CREDENTIALS` | Azure service-principal credentials accepted by `azure/login` |

The workflow currently targets resource group `rg-product-catalog` and AKS cluster
`aks-product-catalog`. Update those names in the workflow for a different cluster.
Create `bankapp-secrets` in that cluster before the first workflow deployment, using
the AKS deployment command above.

## Security notes

- Database credentials are supplied through environment variables locally and a
	Kubernetes secret in AKS.
- The application rejects zero and negative monetary amounts and prevents transfers
	to the originating account.
- Transfers run in a database transaction so balance and transaction-record updates
	commit or roll back together.
- This is a demonstration application. Before production use, enable CSRF protection,
	use least-privilege database credentials, manage secrets with a vault, add database
	persistence and resource limits, and review the Kubernetes RBAC policy.

## Troubleshooting

- **`JAVA_HOME is not defined correctly`**: Install JDK 17 and set `JAVA_HOME` to the
	JDK directory, then open a new terminal. Verify with `java -version`.
- **Database connection refused**: Ensure MySQL is running, `bankappdb` exists, and
	`DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` match the server.
- **AKS pod cannot pull image**: Verify the GHCR package visibility or configure an
	`imagePullSecret` with credentials that can read the package.
- **Deployment rollout times out**: Inspect pod state and logs with
	`kubectl get pods` and `kubectl logs deployment/bankapp`.
