package main;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.stanford.nlp.pipeline.SentimentAnnotator;
import org.json.simple.parser.ParseException;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Worker{
    static sentimentAnalysisHandler sentiment = new sentimentAnalysisHandler();
    static namedEntityRecognitionHandler namedEntity = new namedEntityRecognitionHandler();


    public static void main(String[] args) throws IOException, ParseException {
        
        while (true){
            List<Message> messageList = sqs_act.getListMsg("Q_Workers");
            for (int i = 0; i < messageList.size() ; i++) {
                Message msg = sqs_act.getNewMsg_delete("Q_Workers");
                if (msg != null && msg.body().startsWith("input")){
                    Gson gson = new Gson();
                    File jsonFile = Paths.get(msg.body().substring(7)).toFile();
                    JsonObject jsonObject = gson.fromJson(new FileReader(jsonFile), JsonObject.class);
                    JsonArray reviews = jsonObject.getAsJsonArray("reviews");
                    ArrayList<String> reviewsList = new ArrayList<>();
                    for (int j = 0; j < reviews.size() ; j++) {
                        reviewsList.add(reviews.get(j).toString());
                    }

                    String ner = namedEntity.getEntities(reviewsList.get(0));
                    int sen = sentiment.findSentiment(reviewsList.get(0));
                    sqs_act.sendMsg("output_"+ner + sen , "Q_Workers");
                }
            }
        }
    }
}
