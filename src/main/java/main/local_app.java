

// Main Queue will be communication between local app to Manager



package main;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sqs.model.Message;
import org.apache.commons.lang3.time.StopWatch;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class local_app{

    private static String ID = String.valueOf((new Date()).getTime());


    public static void main(String[]args) throws IOException {
        StopWatch.start();
        boolean shouldTerminate = false;
        if(args.length < 1){
            System.out.println("no input file\n");
            System.exit(0);
        }

        String Q_main = "Q_main";

        //SQS creation
        try {
            sqs_act.create_sqs(Q_main);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        try {
            sqs_act.create_sqs(Q_main);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //S3 creation
        String bucket_name = "bucket251194";
        try {
            S3_Modify.createBucket(bucket_name);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }

        String n = null;
        int numFiles ;
        //check if terminate active and upload files
        if(isInteger(args[args.length - 1])) {
            numFiles = args.length - 1;
            n = args[args.length - 1];
        }
        else {
            numFiles = args.length - 2;
            n = args[args.length - 2];
        }

//        upload files
        for (int i = 0; i< numFiles; i++ ) {
            S3_Modify.multipartUpload(bucket_name, args[i]);
        }
        //Manager creation
        Ec2Client ec2Client = Ec2Client.create();
        if (!ManagerExist(ec2Client))
            ManagerCreation(ec2Client);
        ManagerActivation(ec2Client); //start it anyway

//        send n as first msg
        sqs_act.sendMsg(n,Q_main);
//        // send sqs msg
        for (int i =0; i< numFiles; i++) {
            sqs_act.sendMsg("input_"+args[i], Q_main);
        }

        // wait for done msg
        Message done = waitForDoneMessage();
        downloadSummeryFile(done);
        if (shouldTerminate) {
            System.out.println("terminating manager");
            terminateManager();
        }

        stopWatch.stop();
        System.out.println("runtime: " + stopWatch.getTime() + " milliseconds");


    }

    public static void terminateManager() {
        sqs_act.sendMsg("terminate","Q_main_send");
    }

    public static void downloadSummeryFile(Message doneMessage) throws IOException {
        System.out.println("downloading html");
        S3_Modify.getObject("bucket251194", doneMessage.body(), System.getProperty("user.dir") + File.separator + "output-" + (new Date()).getTime() + ".html");
    }

    public static Message waitForDoneMessage() {
        boolean stop = false;
        Message doneMessage = null;

        while(!stop) {
            doneMessage = sqs_act.getNewMsg_delete("Q_main_get");
            if (doneMessage != null) {
                stop = true;
            }
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException var5) {
                var5.printStackTrace();
            }
        }
        return doneMessage;
    }

    private static void ManagerActivation(Ec2Client ec2){
        RebootInstancesRequest request = RebootInstancesRequest.builder()
                .instanceIds(Manager.getManager_id())
                .build();
        ec2.rebootInstances(request);

//        StartInstancesRequest request = StartInstancesRequest.builder()
//                .instanceIds(Manager.getManager_id())
//                .build();
//        ec2.startInstances(request);
        System.out.println("Manager active\n");
    }

    private static void ManagerCreation(Ec2Client client){
        String script = "#! /bin/bash\n" + "java -jar /home/ec2-user/AWS/DSPS_Ass1-1.0-SNAPSHOT.jar";

        create_node.create(client,"Manager",script,"Manager");
    }

    private static boolean ManagerExist(Ec2Client ec2) {
        String nextToken = null;
        do {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder().maxResults(6).nextToken(nextToken).build();
            DescribeInstancesResponse response = ec2.describeInstances(request);

            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    List<Tag> tags = instance.tags();
                    for (Tag tag : tags) {
                        if (tag != null && tag.key().equals("Manager")) {
                            Manager.update_manager_id(instance.instanceId());
                            return true;
                        }
                    }
                }
            }
            nextToken = response.nextToken();
        } while (nextToken != null);
        return false;
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException | NullPointerException e) {
            return false;
        }
        return true;
    }


}
