package com.example.silence;

import java.util.*;

/*
 =====================================================
 EMOTION CLASSIFIER
 Naive Bayes + Synonym Expansion

 This improves detection by expanding words into
 emotional synonyms automatically.
 =====================================================
*/

public class EmotionClassifier {

    // emotion -> (word -> count)
    private Map<String, Map<String, Integer>> wordCounts = new HashMap<>();

    // emotion -> number of training sentences
    private Map<String, Integer> emotionCounts = new HashMap<>();

    // vocabulary
    private Set<String> vocabulary = new HashSet<>();


    /* =====================================================
       STOP WORDS (ignored words)
    ===================================================== */

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "i","am","is","are","the","a","an","to","and","or",
            "of","in","on","for","with","this","that","it",
            "my","me","you","your","we","they","them",
            "be","been","being","was","were","do","does","did",
            "very","really","just","so","too"
    ));


    /* =====================================================
       SYNONYM MAP
       Expands emotional vocabulary
    ===================================================== */

    private static final Map<String,List<String>> SYNONYMS = new HashMap<>();

    static {

        SYNONYMS.put("sad", Arrays.asList(
                "sad","lonely","hopeless","miserable","down",
                "heartbroken","empty","worthless","depressed",
                "grief","sorrow","melancholy","unhappy","broken",
                "hurt","devastated","bad","rejected","ignored",
                "excluded","unwanted","leftout","abandoned",
                "notinvited","forgotten"
        ));

        SYNONYMS.put("anxiety", Arrays.asList(
                "anxious","worried","nervous","panic","fear","uneasy",
                "restless","jittery","tense","scared","dread",
                "afraid","overthinking","uneasy","pressure"
        ));

        SYNONYMS.put("anger", Arrays.asList(
                "angry","furious","rage","irritated","annoyed",
                "frustrated","hostile","resentful","mad",
                "outraged","aggressive","bitter","provoked"
        ));

        SYNONYMS.put("stress", Arrays.asList(
                "stress","overwhelmed","pressure","overworked",
                "burned","burnout","overloaded","strained",
                "exhausted","tension","overwhelming"
        ));

        SYNONYMS.put("fatigue", Arrays.asList(
                "tired","exhausted","drained","fatigued","sleepy",
                "lethargic","weak","lowenergy","wornout",
                "energyless","sluggish"
        ));

        SYNONYMS.put("happy", Arrays.asList(
                "happy","joyful","excited","grateful","peaceful",
                "optimistic","cheerful","delighted","inspired",
                "content","confident","motivated","blessed"
        ));


    }


    /* =====================================================
       TEXT PREPROCESSING
    ===================================================== */

    private List<String> preprocess(String text){

        text = text.toLowerCase();
        text = text.toLowerCase();

// normalize emotional phrases
        text = text.replace("not invited","notinvited");
        text = text.replace("left out","leftout");
        text = text.replaceAll("[^a-z ]"," ");

        String[] tokens = text.split("\\s+");

        List<String> words = new ArrayList<>();

        for(String token : tokens){

            if(token.length() < 2) continue;
            if(STOP_WORDS.contains(token)) continue;

            words.add(token);

            // add synonyms automatically
            for(List<String> synList : SYNONYMS.values()){
                if(synList.contains(token)){
                    words.addAll(synList);
                }
            }
        }

        return words;
    }


    /* =====================================================
       TRAIN MODEL
    ===================================================== */

    public void train(String emotion, String text){

        List<String> words = preprocess(text);

        emotionCounts.put(
                emotion,
                emotionCounts.getOrDefault(emotion,0)+1
        );

        wordCounts.putIfAbsent(emotion,new HashMap<>());

        Map<String,Integer> counts = wordCounts.get(emotion);

        for(String word : words){

            vocabulary.add(word);

            counts.put(
                    word,
                    counts.getOrDefault(word,0)+1
            );
        }
    }


    /* =====================================================
       PREDICT EMOTION
    ===================================================== */

    public String predict(String text){

        List<String> words = preprocess(text);

        if(words.isEmpty()) return "neutral";

        double bestScore = Double.NEGATIVE_INFINITY;
        String bestEmotion = "neutral";

        for(String emotion : emotionCounts.keySet()){

            Map<String,Integer> counts = wordCounts.get(emotion);

            int totalWords = 0;

            for(int c : counts.values()){
                totalWords += c;
            }

            double score = Math.log(emotionCounts.get(emotion));

// small bias for emotional categories
            if(!emotion.equals("happiness")){
                score += 0.2;
            }

            for(String word : words){

                int count = counts.getOrDefault(word,0) + 1;

                double probability =
                        (double)count /
                                (totalWords + vocabulary.size());

                score += Math.log(probability);
            }

            if(score > bestScore){
                bestScore = score;
                bestEmotion = emotion;
            }
        }

        return bestEmotion;
    }
}