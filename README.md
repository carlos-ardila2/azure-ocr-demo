# Survey Application

## Purpose
This project is a survey application developed using Java and Gradle. It leverages Azure Functions to handle serverless operations and integrates with various Azure services such as Azure AI Document Intelligence, Azure Storage Queue, and Azure Identity.

## Deployment to Azure Cloud

### Prerequisites
- Java Development Kit (JDK) 21
- Gradle
- Azure CLI
- An Azure subscription

### Steps to Deploy

1. **Clone the repository:**
   ```sh
   git clone <repository-url>
   cd <repository-directory>
   ```

2. **Login to Azure:**
   ```sh
   az login
   ```

3. **Create a Resource Group:**
   ```sh
   az group create --name <resource-group-name> --location <location>
   ```

4. **Deploy the application:**
   ```sh
   az deployment group create --resource-group <resource-group-name> --template-file azuredeploy.json --parameters @azuredeploy.parameters.json
   ```

5. **Access the application:**
   ```sh
   az functionapp show --name <function-app-name> --resource-group <resource-group-name> --query "defaultHostName" --output tsv
   ```

6. **Clean up:**
   ```sh
   az group delete --name <resource-group-name> --yes --no-wait
   ```

## Environment Variables
The `.env` file should contain the following variables:

- `ORG_GRADLE_PROJECT_azure_subscription`: Your Azure subscription ID.
- `ORG_GRADLE_PROJECT_azure_resourceGroup`: The name of the resource group where the Azure Function will be deployed.
- `ORG_GRADLE_PROJECT_azure_appName`: The name of your Azure Function app.
- `ORG_GRADLE_PROJECT_azure_pricingTier`: The pricing tier for your Azure Function app (e.g., `Consumption`, `Premium`).
- `ORG_GRADLE_PROJECT_azure_region`: The Azure region where your resources will be deployed (e.g., `eastus`, `westeurope`).
- `AZURE_QUEUE_ENDPOINT`: The endpoint for your Azure Storage Queue.
- `AZURE_QUEUE_SAS_TOKEN`: The SAS token for accessing your Azure Storage Queue.
- `AZURE_QUEUE_NAME`: The name of your Azure Storage Queue.
- `AZURE_DOC_INTELLIGENCE_ENDPOINT`: The endpoint for your Azure AI Document Intelligence service.
- `AZURE_DOC_INTELLIGENCE_KEY`: The key for your Azure AI Document Intelligence service.

These variables are used in the `build.gradle` file to configure the deployment settings for the Azure Function.

## Additional Information
- **Source Compatibility:** Java 21
- **Target Compatibility:** Java 21
- **Dependencies:** The project uses several Azure SDKs and other libraries as specified in the `build.gradle` file.

For more detailed information, refer to the official Azure Functions documentation and the Azure SDK for Java documentation.
```
