package org.microsoft.ml;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;

import org.mlflow.api.proto.Service.Experiment;
import org.mlflow.api.proto.Service.Metric;
import org.mlflow.api.proto.Service.RunInfo;
import org.mlflow.tracking.MlflowClient;
import org.mlflow.tracking.creds.BasicMlflowHostCreds;
import org.mlflow.tracking.creds.MlflowHostCredsProvider;

public class App {

    // Azure Resource Management is the target for which we're acquiring the token
    private final static String TARGET_RESOURCE = "https://management.core.windows.net/";

    //
    // CONFIGURE THESE VALUES
    //

    // https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli?view=azure-cli-latest#password-based-authentication
    private final static String _tenantId = "<YOUR SERVICE PRINCIPAL TENANT ID>";
    private final static String AUTHORITY = String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token",
            _tenantId);
    private final static String CLIENT_ID = "<YOUR SERVICE PRINCIPAL CLIENT ID>";
    // DO NOT HARDCODE THIS IN - set the environment variable so the SP password is
    // not accidentally committed
    private final static String PASSWORD_UNUSED_LEFT_FOR_CLARITY = System.getenv("AZUREMLFLOW_SP_PASSWORD");

    // Something following the below template
    private final static String _subscriptionId = "<YOUR AZURE ML WORKSPACE'S SUBSCRIPTION ID>";
    private final static String _resourceGroup = "<YOUR AZURE ML WORKSPACE'S RESOURCE GROUP NAME>";
    private final static String _workspaceName = "<YOUR AZURE ML WORKSPACE NAME>";
    private final static String TRACKING_URI = String.format(
            "https://eastus2.experiments.azureml.net/history/v1.0/subscriptions/%s/resourceGroups/%s/providers/Microsoft.MachineLearningServices/workspaces/%s",
            _subscriptionId, _resourceGroup, _workspaceName);
    //
    // END VALUES TO CONFIGURE
    //

    public static void main(String[] args) throws Exception {
        // Use this to diagnose tracking issues
        // System.setProperty("javax.net.debug", "all");
        System.out.println("Hello AzureML!");

        // Authenticate against and fetch a token from Azure Resource Manager (ARM)
        AuthenticationResult result = getAccessTokenFromUserCredentials(AUTHORITY, TARGET_RESOURCE, CLIENT_ID);
        String armToken = result.getAccessToken();
        if (armToken == null) {
            throw new Exception("Provide a valid ARM token.");
        }

        // Instantiate the MLflow client using bearer token authorization with the
        // recently fetched token
        MlflowHostCredsProvider credsProvider = new BasicMlflowHostCreds(TRACKING_URI, armToken);
        MlflowClient client = new MlflowClient(credsProvider);

        // Create the MLflow Experiment if it doesn't exist
        String experimentName = "azuremlflow-java";
        Optional<Experiment> experiment = client.getExperimentByName(experimentName);
        String experimentId = experiment.isPresent() ? experiment.get().getExperimentId()
                : client.createExperiment(experimentName);

        // Create a new tracked run in the above experiment
        RunInfo runInfo = client.createRun(experimentId);
        String runId = runInfo.getRunUuid();

        // Log some super cool metrics
        String metricToLog = "accuracy";
        client.logMetric(runId, metricToLog, 0.1);
        client.logMetric(runId, metricToLog, 0.2);
        client.logMetric(runId, metricToLog, 0.5);
        client.logMetric(runId, metricToLog, 0.9);

        // Fetch previously logged metrics from the tracking server
        List<Metric> metrics = client.getMetricHistory(runId, metricToLog);
        for (Metric metric : metrics) {
            System.out.println(String.format("Logged %f", metric.getValue()));
        }

        // :'(
        System.out.println("Goodbye AzureML!");
    }

    // https://github.com/Azure-Samples/active-directory-java-native-headless/blob/master/src/main/java/PublicClient.java
    private static AuthenticationResult getAccessTokenFromUserCredentials(String loginAuthorityUrl,
            String targetResourceForToken, String clientId) throws Exception {
        AuthenticationResult result;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            AuthenticationContext context = new AuthenticationContext(loginAuthorityUrl, false, service);
            Future<AuthenticationResult> future = context.acquireToken(targetResourceForToken,
                    new ClientCredential(clientId, System.getenv("AZUREMLFLOW_SP_PASSWORD")), null);
            result = future.get();
        } finally {
            if (service != null) {
                service.shutdown();
            }
        }

        if (result == null) {
            throw new Exception("Authentication result was null");
        }
        return result;
    }
}
