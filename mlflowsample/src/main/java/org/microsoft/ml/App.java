package org.microsoft.ml;

import org.mlflow.api.proto.Service.RunInfo;
import org.mlflow.tracking.MlflowClient;
import org.mlflow.tracking.creds.BasicMlflowHostCreds;
import org.mlflow.tracking.creds.MlflowHostCredsProvider;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello AzureML!");

        String trackingUri = "https://eastus2.experiments.azureml.net//history/v1.0/subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.MachineLearningServices/workspaces/{workspaceName}";
        String armToken = System.getenv("MLFLOW_TRACKING_TOKEN");
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
}
