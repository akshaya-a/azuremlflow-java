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

    /**
     * CHANGE THESE VALUES
     */
    // https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli?view=azure-cli-latest#password-based-authentication
    private final static String CLIENT_ID = "<your_SP_client_id>";
    // DO NOT HARDCODE THIS IN - set the environment variable so the SP password is
    // not accidentally committed
    private final static String PASSWORD = System.getenv("AZUREMLFLOW_SP_PASSWORD");
    // Something following the below template
    private final static String TRACKING_URI = "https://eastus2.experiments.azureml.net/history/v1.0/subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.MachineLearningServices/workspaces/{workspaceName}";
    //
    // END VALUES TO CONFIGURE
    //

    public static void main(String[] args) throws Exception {
        System.out.println("Hello AzureML!");

        AuthenticationResult result = getAccessTokenFromUserCredentials();
        String armToken = result.getAccessToken();
        if (armToken == null) {
            throw new Exception("Provide a valid ARM token.");
        }

        MlflowHostCredsProvider credsProvider = new BasicMlflowHostCreds(TRACKING_URI, armToken);
        MlflowClient client = new MlflowClient(credsProvider);

        String experimentId = client.createExperiment("azuremlflow-java");
        RunInfo runInfo = client.createRun(experimentId);

        client.logMetric(runInfo.getRunUuid(), "accuracy", 0.9);

        System.out.println("Goodbye AzureML!");
    }

    private static AuthenticationResult getAccessTokenFromUserCredentials() throws Exception {
        AuthenticationResult result;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            AuthenticationContext context = new AuthenticationContext(AUTHORITY, false, service);
            Future<AuthenticationResult> future = context.acquireToken(TARGET_RESOURCE,
                    new ClientCredential(CLIENT_ID, PASSWORD), null);
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
