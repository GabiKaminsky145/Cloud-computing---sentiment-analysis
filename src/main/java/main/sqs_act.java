package main;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

public class sqs_act {

    public static SqsClient sqs;

    public static void create_sqs(String name){
        sqs = SqsClient.builder().region(Region.US_EAST_1).build();


        try {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(name)
                    .build();
            CreateQueueResponse create_result = sqs.createQueue(request);
        } catch (QueueNameExistsException e) {
            throw e;

        }
    }

    public static void sendMsg (String msg,String q_name){

        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(q_name)
                .build();
        String queueUrl = sqs.getQueueUrl(getQueueRequest).queueUrl();

        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(msg)

                .build();
        sqs.sendMessage(send_msg_request);
    }

    public static void DeleteMsg(Message msg,String Q_name){

        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(Q_name)
                .build();

        String queueUrl = sqs.getQueueUrl(getQueueRequest).queueUrl();

        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(msg.receiptHandle())
                .build();
        sqs.deleteMessage(deleteRequest);
    }


    public static Message getNewMsg_delete(String Q_name){

        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(Q_name)
                .build();

        String queueUrl = sqs.getQueueUrl(getQueueRequest).queueUrl();

        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .build();

        List<Message> messages = sqs.receiveMessage(receiveRequest).messages();

        Message tmp = null;
        for (Message message : messages) {
            tmp = message;
            DeleteMsg(message,Q_name);
            return tmp;
        }
        return null;
    }

    public static List<Message> getListMsg(String Q_name){
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(Q_name)
                .build();

        String queueUrl = sqs.getQueueUrl(getQueueRequest).queueUrl();

        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .build();
        return sqs.receiveMessage(receiveRequest).messages();
    }

    public static void close(String Q_name){

    }

}
