package org.dspace.service.impl;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public class SqsService {

    /*

sqs.accesskey=AKIAX2YFANB47UCDYT7E
sqs.accesssecret=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
sqs.data_queue_uri=https://sqs.eu-west-1.amazonaws.com/538492037241/orchestrator-data-queue.fifo
sqs.command_queue_uri=https://sqs.eu-west-1.amazonaws.com/538492037241/orchestrator-command-queue.fifo
sqs.heartbeat_queue_uri=https://sqs.eu-west-1.amazonaws.com/538492037241/orchestrator-heartbeat-queue.fifo

    */

    private static ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    private SqsService() { }

    private static SqsAsyncClient sqsClient = null;

    public static int sendStartCommand(String[] args) {
        return sendCommand(args, true, null, UUID.randomUUID().toString());
    }

    public static int sendFinishCommand(String[] args, String status) {
        return sendCommand(args, false, status, UUID.randomUUID().toString());
    }

    public static void sendHeartbeat(String[] args) {
        String taskId = "";
        for (int i = 0; i < args.length - 1; i++) {
            if ("-task_id".equals(args[i])) {
                taskId = args[i + 1];
            }
        }
        if (StringUtils.isEmpty(taskId)) {
            throw new RuntimeException("Missing task id");
        }

        SqsAsyncClient client = getSqsClient();

        String messageBody = "{\"taskId\":\"" + taskId + "}";

        SendMessageRequest sendMessageRequest = SendMessageRequest
            .builder()
            .messageBody(messageBody)
            //prevent deduplication
            .messageDeduplicationId(UUID.randomUUID().toString())
            .queueUrl(configurationService.getProperty("sqs.heartbeat_queue_uri"))
            //static string prevent wrong ordering in async queue
            .messageGroupId("dummystring")
            .build();
        SendMessageResponse response = null;
        try {
            response = client.sendMessage(sendMessageRequest).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (response != null && response.messageId() != null) {
            System.out.println("Heartbeat time: " + System.currentTimeMillis());
        } else {
            System.err.println("Error on heartbeat send: " + System.currentTimeMillis());
        }

    }


    public static int sendCommand(String[] args, boolean isStart, String exitStatus, String deduplication) {
        if (!isStart && exitStatus == null) {
            throw new RuntimeException("Missing exit status");
        }
        String taskId = "";
        for (int i = 0; i < args.length - 1; i++) {
            if ("-task_id".equals(args[i])) {
                taskId = args[i + 1];
            }
        }
        if (StringUtils.isEmpty(taskId)) {
            throw new RuntimeException("Missing task id");
        }

        SqsAsyncClient client = getSqsClient();
        String messageBody = getSqsPayload(taskId, isStart ? "STARTED" : "FINISHED", isStart ? null : exitStatus);

        System.out.println("Message: " + messageBody);

        SendMessageRequest sendMessageRequest = SendMessageRequest
            .builder()
            .messageBody(messageBody)
            //prevent deduplication
            .messageDeduplicationId(deduplication)
            .queueUrl(configurationService.getProperty("sqs.command_queue_uri"))
            //static string prevent wrong ordering in async queue
            .messageGroupId("dummystring")
            .build();
        SendMessageResponse response = null;
        try {
            response = client.sendMessage(sendMessageRequest).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (response != null && response.messageId() != null) {
            System.out.println("Command send: " + messageBody);
            return 0;
        } else {
            System.err.println("Error sending command: " + messageBody);
            return 1;
        }
    }

    public static int enqueueCommand(String serviceId, String[] args, String deduplication) {
        SqsAsyncClient client = getSqsClient();
        String plainCommand = "";
        for (String arg : args) {
            plainCommand += (arg + " ");
        }
        String messageBody = "{\"serviceId\":" + serviceId + ",\"plainCommand\":\"" + plainCommand + "\"}";
        System.out.println("Enqueue: " + messageBody);
        /*
         * The message group ID is the tag that specifies that a message belongs to a specific message group.
         * Messages that belong to the same message group are always processed one by one, in a strict order
         * relative to the message group (however, messages that belong to different message groups might
         * be processed out of order).
         */
        SendMessageRequest sendMessageRequest = SendMessageRequest
            .builder()
            .messageBody(messageBody)
            .messageDeduplicationId(deduplication)
            .queueUrl(configurationService.getProperty("sqs.data_queue_uri"))
            .messageGroupId("dummystring")
            .build();
        SendMessageResponse response = null;
        try {
            response = client.sendMessage(sendMessageRequest).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (response != null && response.messageId() != null) {
            System.out.println("Command queued: " + messageBody);
            return 0;
        } else {
            System.err.println("Cannot enqueue operation: " + messageBody);
            return 1;
        }
    }


    private static SqsAsyncClient getSqsClient() {

        if (sqsClient == null) {

            AwsCredentials awsCredentials = new AwsCredentials() {

                @Override
                public String secretAccessKey() {
                    return configurationService.getProperty("sqs.accesssecret");
                }

                @Override
                public String accessKeyId() {
                    return configurationService.getProperty("sqs.accesskey");
                }
            };

            AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(awsCredentials);

            sqsClient = SqsAsyncClient
                .builder()
                .httpClient(AwsCrtAsyncHttpClient.builder().build())
                .region(Region.EU_WEST_1)
                .credentialsProvider(awsCredentialsProvider)
                .build();

        }
        return sqsClient;
    }

    private static String getSqsPayload(String taskId, String phase, String exitStatus) {
        return "{\"taskId\":\"" + taskId + "\",\"taskPhaseEnum\":\"" +
            phase + "\",\"exitStatusEnum\":\"" + exitStatus + "\"}";
    }


}
