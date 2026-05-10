.PHONY: help clean build dev run test package install lint format

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

help:
	@echo "$(BLUE)Juntos Alpha - Quarkus Backend$(NC)"
	@echo ""
	@echo "$(GREEN)Available commands:$(NC)"
	@echo "  $(YELLOW)make dev$(NC)           - Start dev server with hot reload (port 8080)"
	@echo "  $(YELLOW)make build$(NC)         - Compile the project"
	@echo "  $(YELLOW)make run$(NC)           - Build and run production JAR"
	@echo "  $(YELLOW)make clean$(NC)         - Clean build artifacts"
	@echo "  $(YELLOW)make test$(NC)          - Run tests"
	@echo "  $(YELLOW)make package$(NC)       - Package into JAR"
	@echo "  $(YELLOW)make install$(NC)       - Install dependencies (mvn install)"
	@echo "  $(YELLOW)make format$(NC)        - Format code (requires google-java-format)"
	@echo "  $(YELLOW)make check-java$(NC)    - Check Java version"
	@echo "  $(YELLOW)make health$(NC)        - Health check endpoint"
	@echo ""

check-java:
	@echo "$(BLUE)Java Version:$(NC)"
	@java -version

dev:
	@echo "$(BLUE)Starting dev server...$(NC)"
	mvn quarkus:dev -Dquarkus.enforceBuildGoal=false

build:
	@echo "$(BLUE)Building project...$(NC)"
	mvn clean compile

test:
	@echo "$(BLUE)Running tests...$(NC)"
	mvn test

package:
	@echo "$(BLUE)Packaging application...$(NC)"
	mvn clean package -DskipTests

run: package
	@echo "$(BLUE)Running application...$(NC)"
	@if [ -f target/juntos-alpha-*-runner.jar ]; then \
		java -jar target/juntos-alpha-*-runner.jar; \
	else \
		echo "$(RED)JAR not found. Try 'make build' first.$(NC)"; \
	fi

clean:
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	mvn clean
	rm -rf target/

install:
	@echo "$(BLUE)Installing dependencies...$(NC)"
	mvn install

format:
	@echo "$(BLUE)Formatting code...$(NC)"
	@command -v google-java-format >/dev/null 2>&1 || { echo "$(YELLOW)Installing google-java-format...$(NC)"; npm install -g google-java-format; }
	find src -name "*.java" -type f -exec google-java-format -i {} \;
	@echo "$(GREEN)✓ Code formatted$(NC)"

health:
	@echo "$(BLUE)Health Check:$(NC)"
	@echo "  Ping: $$(curl -s http://localhost:8080/ping)"
	@echo "  Home: $$(curl -s http://localhost:8080/)"

lint:
	@echo "$(BLUE)Checking code quality...$(NC)"
	mvn checkstyle:check 2>/dev/null || echo "$(YELLOW)Checkstyle not configured$(NC)"

version:
	@echo "$(BLUE)Project Version:$(NC)"
	@mvn help:describe -Ddetail=false 2>/dev/null | grep "Version:" || grep "<version>" pom.xml | head -1

info:
	@echo "$(BLUE)Project Information:$(NC)"
	@echo "  Name: $$(grep '<artifactId>juntos-alpha' pom.xml | sed 's/.*<artifactId>//;s/<\/.*//')"
	@echo "  Java: $$(java -version 2>&1 | head -1)"
	@echo "  Maven: $$(mvn -v 2>/dev/null | head -1)"
	@echo "  Port: 8080"
	@echo "  Database: In-memory H2"
