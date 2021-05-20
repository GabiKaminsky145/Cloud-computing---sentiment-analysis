# Cloud-computing sentiment-analysis


The projects were build using three main classes: Local application, Manager, and Worker. All the classes can be viewed in the src directory.
The local App is responsible to build the framework of the program on the AWS services: queues, S3 container (bucket), Manager (main) instance. The local app uploads the input files that the user sends to S3 bucket, then it's waiting for the HTML file and downloaded it.
The manager is starting to run when he created a script command. The manager is responsible to create worker instances based on the number of the input files and based on the workers' availability, Then it sending all the files to analysis to the workers' queue. When the manager gets all the results, it builds the HTML result file and sends it to the main queue.
The worker is getting a new message from the queue, and using the StanfordNLP analysis to detect sarcasm, it's sending the result back to the manager.


