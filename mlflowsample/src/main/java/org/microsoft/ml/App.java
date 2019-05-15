package org.microsoft.ml;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;

import org.mlflow.api.proto.Service.RunInfo;
import org.mlflow.tracking.MlflowClient;
import org.mlflow.tracking.creds.BasicMlflowHostCreds;
import org.mlflow.tracking.creds.MlflowHostCredsProvider;

/**
 * Hello world!
 *
 */
public class App {

    // https://github.com/Azure-Samples/active-directory-java-native-headless/blob/master/src/main/java/PublicClient.java
    private final static String AUTHORITY = "https://login.microsoftonline.com/common/";
    private final static String TARGET_RESOURCE = "https://management.core.windows.net/";

    // https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli?view=azure-cli-latest#password-based-authentication
    private final static String CLIENT_ID = "<your_SP_client_id>";

    public static void main(String[] args) throws Exception {
        System.out.println("Hello AzureML!");

        AuthenticationResult result = getAccessTokenFromUserCredentials();

        String trackingUri = "https://eastus2.experiments.azureml.net/history/v1.0/subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.MachineLearningServices/workspaces/{workspaceName}";
        String armToken = result.getAccessToken();
        if (armToken == null) {
            throw new Exception("Provide a valid ARM token.");
        }

        MlflowHostCredsProvider credsProvider = new BasicMlflowHostCreds(trackingUri, armToken);
        MlflowClient client = new MlflowClient(credsProvider);

        String experimentId = client.createExperiment("azuremlflow-java");
        RunInfo runInfo = client.createRun(experimentId);

        client.logMetric(runInfo.getRunUuid(), "accuracy", 0.9);

        System.out.println("Goodbye AzureML!");
    }

    private static AuthenticationResult getAccessTokenFromUserCredentials() throws Exception {
        AuthenticationContext context;
        AuthenticationResult result;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(AUTHORITY, false, service);
            Future<AuthenticationResult> future = context.acquireToken(TARGET_RESOURCE,
                    new ClientCredential(CLIENT_ID, System.getenv("AZUREMLFLOW_SP_PASSWORD")), null);
            result = future.get();
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new Exception("authentication result was null");
        }
        return result;
    }
}
