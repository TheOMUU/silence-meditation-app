package com.example.silence;

import android.content.Context;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class EmotionAI {

    HashMap<String, JSONArray> emotionData = new HashMap<>();

    public EmotionAI(Context context){

        try{

            InputStream is = context.getAssets().open("emotion_dataset.json");

            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            String json = scanner.hasNext() ? scanner.next() : "";

            JSONObject object = new JSONObject(json);

            Iterator<String> keys = object.keys();

            while(keys.hasNext()){

                String emotion = keys.next();

                JSONArray words = object.getJSONArray(emotion);

                emotionData.put(emotion,words);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public String predictEmotion(String text){

        if(text == null || text.trim().isEmpty()){
            return "neutral";
        }

        // Clean text
        text = text.toLowerCase();
        text = text.replaceAll("[^a-zA-Z ]", "");

        HashMap<String,Integer> scores = new HashMap<>();

        for(String emotion : emotionData.keySet()){

            JSONArray words = emotionData.get(emotion);

            int score = 0;

            for(int i=0;i<words.length();i++){

                String keyword = words.optString(i).toLowerCase();

                if(text.contains(keyword)){
                    score += 2;   // increase weight
                }

                // partial match improvement
                String[] tokens = text.split(" ");

                for(String token : tokens){
                    if(token.equals(keyword)){
                        score += 3;
                    }
                }

            }

            scores.put(emotion,score);
        }

        String bestEmotion = "neutral";
        int highestScore = 0;

        for(String emotion : scores.keySet()){

            int score = scores.get(emotion);

            if(score > highestScore){
                highestScore = score;
                bestEmotion = emotion;
            }
        }

        return bestEmotion;
    }

}