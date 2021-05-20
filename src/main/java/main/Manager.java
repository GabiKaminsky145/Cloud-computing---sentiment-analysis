package main;

import com.amazonaws.services.s3.model.S3Object;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

public class Manager {

    private static final String Q_main = "Q_main";
    private static final String Q_Workers = "Q_worker";

    private static String manager_id;

    public static String getManager_id(){
        return manager_id;
    }

    public static void update_manager_id(String manager_id){
        Manager.manager_id = manager_id;
    }


    public static void main(String[] args) {

        boolean terminate = false;

        List<Message> messageList = sqs_act.getListMsg(Q_main);
        int n = 0;
        try {
            n = Integer.parseInt(messageList.get(0).body());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        Ec2Client ec2Client = Ec2Client.create();
        int worker_count = messageList.size() / n + 1;
        String node_id;

        String script = "#! /bin/bash\n" +
                "cd AWS\n" +
                "java -cp project.jar Worker";

        int initial = worker_count;
        for (int i = 0; i < worker_count; i++) {
            create_node.create(ec2Client, "Worker_" + i,script, "Worker_" + i);

        }
        Message msg = null;

        sqs_act.create_sqs(Q_Workers);

        //start infinite loop

        while (!terminate) {

            //get messages from local app
            messageList = sqs_act.getListMsg(Q_main);
            for (int i = 0; i < messageList.size(); i++) {
                msg = sqs_act.getNewMsg_delete(Q_main);
                if (msg != null && msg.body().startsWith("input"))
                    sqs_act.sendMsg(msg.body().substring(6), Q_Workers);
                else if (msg != null && msg.body().equals("terminate")) {
                    terminate = true;
                    closeManager();
                }
            }

            int count_curr_workers = 0;
            // create new instances
            DescribeInstancesResponse instancesRequest = ec2Client.describeInstances();
            List<Reservation> resvations = instancesRequest.reservations();
            for (Reservation r : resvations){
                List<Instance> instances = r.instances();
                for (Instance i : instances){
                    boolean state = i.state().name().name().equals("Running");
                    List<Tag> tags = i.tags();
                    for (Tag t : tags){
                        if (t.value().startsWith("Worker") && state )
                            count_curr_workers ++;
                    }
                }
            }
            // create new instances if needed
            messageList = sqs_act.getListMsg(Q_main);
            worker_count = messageList.size() / n + 1;
            int new_instances_workers = worker_count - count_curr_workers;
            for (int i = 0 ; i< new_instances_workers; i++){
                create_node.create(ec2Client,"Worker_"+ (initial+i),script,"Worker_"+ (initial+i));
            }

            //get messages from workers
            messageList = sqs_act.getListMsg(Q_Workers);
            for (int i = 0; i < messageList.size() ; i++) {
                msg = sqs_act.getNewMsg_delete(Q_Workers);
                if (msg != null && msg.body().startsWith("output"))
                    sqs_act.sendMsg(msg.body().substring(7), Q_main);
            }
        }

        //finished handle input
    }

    private static void handleWorker(List<Message> messageList){
        Ec2Client ec2Client = Ec2Client.create();
        S3Object[] files = new S3Object[messageList.size()];
        for (int i = 0; i < files.length ; i++) {
            files[i] =  S3_Modify.getFile("bucket251194",messageList.get(i).body().substring(6));
            sqs_act.create_sqs("Q_"+messageList.get(i).body());
            create_node.create(ec2Client,files[i].getKey(),"",files[i].getKey());
        }
    }

    private static void closeManager(){

        Ec2Client ec2Client = Ec2Client.create();
        DescribeInstancesResponse instancesRequest = ec2Client.describeInstances();
        for (int i = 0; i < instancesRequest.reservations().size() ; i++) {
            List<Instance> instances = instancesRequest.reservations().get(i).instances();
            for (int j = 0; j < instances.size() ; j++) {
                TerminateInstancesRequest terminateRequest = (TerminateInstancesRequest)TerminateInstancesRequest.builder()
                        .instanceIds(instances.get(j).instanceId())
                        .build();
                ec2Client.terminateInstances(terminateRequest);
            }
        }
    }

}
