package com.example.silence;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor; // ✅ FIXED: Missing import
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Iterator;

public class AndroidBridge {

    /* =====================================================
       GLOBAL VARIABLES
    ===================================================== */

    private Context context;
    private WebView webView;
    private DatabaseHelper db;
    private String currentUserEmail;

    public AndroidBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        this.db = new DatabaseHelper(context);
    }

    /* =====================================================
       LOAD AI MODEL FROM DATASET
    ===================================================== */

    private EmotionClassifier loadAIFromDataset(){

        EmotionClassifier ai = new EmotionClassifier();

        try{

            InputStream is = context.getAssets().open("emotion_dataset.json");

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");

            JSONObject dataset = new JSONObject(json);

            Iterator<String> emotions = dataset.keys();

            while(emotions.hasNext()){

                String emotion = emotions.next();

                JSONArray sentences = dataset.getJSONArray(emotion);

                for(int i = 0; i < sentences.length(); i++){

                    String text = sentences.getString(i);

                    ai.train(emotion, text);
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return ai;
    }

    /* =====================================================
       EMOTION ANALYSIS + EXERCISE GENERATION
    ===================================================== */

    @JavascriptInterface
    public void analyzeEmotion(String problem) {

        if(problem == null || problem.trim().isEmpty()) return;

        if(currentUserEmail == null){
            SharedPreferences prefs =
                    context.getSharedPreferences("session",0);
            currentUserEmail = prefs.getString("user_email",null);
        }

        if(currentUserEmail == null) return;

        EmotionClassifier ai = loadAIFromDataset();

        String lower = problem.toLowerCase();

        String emotion;

        /* =====================================================
           PRIORITY RULE DETECTION
        ===================================================== */

        if(lower.contains("stress") ||
                lower.contains("stressed") ||
                lower.contains("pressure") ||
                lower.contains("overwhelmed")){

            emotion = "stress";

        }else if(lower.contains("anxious") ||
                lower.contains("panic") ||
                lower.contains("nervous") ||
                lower.contains("worried")){

            emotion = "anxiety";

        }else if(lower.contains("sad") ||
                lower.contains("lonely") ||
                lower.contains("rejected") ||
                lower.contains("not invited")){

            emotion = "sadness";

        }else if(lower.contains("angry") ||
                lower.contains("rage") ||
                lower.contains("frustrated")){

            emotion = "anger";

        }else if(lower.contains("tired") ||
                lower.contains("exhausted") ||
                lower.contains("burnout")){

            emotion = "fatigue";

        }else{

            emotion = ai.predict(problem);
        }

        /* =====================================================
           FALLBACK DETECTION
        ===================================================== */

        if(emotion.equals("neutral")){

            if(lower.contains("stress") || lower.contains("overwhelmed"))
                emotion = "stress";

            else if(lower.contains("panic") || lower.contains("anxious"))
                emotion = "anxiety";

            else if(lower.contains("sad") || lower.contains("lonely"))
                emotion = "sadness";

            else if(lower.contains("angry") || lower.contains("frustrated"))
                emotion = "anger";

            else if(lower.contains("tired") || lower.contains("exhausted"))
                emotion = "fatigue";

            else if(lower.contains("happy") || lower.contains("grateful"))
                emotion = "happiness";
        }

        /* =====================================================
           EXERCISE GENERATION
        ===================================================== */

        String exercise;

        switch(emotion){

            case "stress":

                exercise =
                        "STRESS RESET ROUTINE\n\n" +
                                "1. Box Breathing\n" +
                                "Inhale 4 sec → Hold 4 sec → Exhale 4 sec → Hold 4 sec\n\n" +
                                "2. Shoulder Release\n" +
                                "Lift shoulders for 5 seconds then release slowly\n\n" +
                                "3. Focus on the next small step.";

                break;

            case "anxiety":

                exercise =
                        "ANXIETY GROUNDING\n\n" +
                                "5 things you see\n" +
                                "4 things you feel\n" +
                                "3 things you hear\n" +
                                "2 things you smell\n" +
                                "1 thing you taste\n\n" +
                                "Then breathe slowly.";

                break;

            case "sadness":

                exercise =
                        "SELF COMPASSION\n\n" +
                                "Place a hand on your heart.\n" +
                                "Take slow breaths.\n\n" +
                                "Say:\n" +
                                "'This moment will pass.'";

                break;

            case "anger":

                exercise =
                        "ANGER RESET\n\n" +
                                "Take 3 deep breaths.\n" +
                                "Clench fists 5 seconds then release.";

                break;

            case "fatigue":

                exercise =
                        "ENERGY RESET\n\n" +
                                "Close your eyes.\n" +
                                "Take slow breaths for 2 minutes.";

                break;

            case "happiness":

                exercise =
                        "POSITIVE REFLECTION\n\n" +
                                "Think of 3 things you are grateful for.";

                break;

            default:

                exercise =
                        "MINDFUL BREATHING\n\n" +
                                "Focus on breathing slowly for 2 minutes.";
        }

        /* =====================================================
           CRISIS DETECTION
        ===================================================== */

        int riskFlag = 0;

        if(
                lower.contains("suicide") ||
                        lower.contains("kill myself") ||
                        lower.contains("end my life") ||
                        lower.contains("want to die") ||
                        lower.contains("self harm")
        ){
            riskFlag = 1;

            exercise =
                    "⚠ IMPORTANT SUPPORT MESSAGE\n\n" +
                            "You may be going through something very difficult.\n\n" +
                            "Please consider contacting someone you trust or a professional counselor.\n\n" +
                            "If you are in danger, please contact emergency services.";
        }

        /* =====================================================
           SAVE DATA
        ===================================================== */

        db.insertActivity(currentUserEmail, problem, emotion, riskFlag);
        db.saveChat(currentUserEmail, problem, emotion);

        String safeEmotion = emotion.replace("'","\\'");
        String safeExercise = exercise.replace("'","\\'").replace("\n","\\n");

        String js = "showAIResult('" + safeEmotion + "','" + safeExercise + "')";

        webView.post(() -> webView.evaluateJavascript(js,null));
    }

    /* =====================================================
       CHAT HISTORY
    ===================================================== */

    @JavascriptInterface
    public void getChatHistory(){

        if(currentUserEmail == null){

            SharedPreferences prefs =
                    context.getSharedPreferences("session",0);

            currentUserEmail = prefs.getString("user_email",null);
        }

        if(currentUserEmail == null){

            webView.post(() ->
                    webView.evaluateJavascript("showChatHistory([])",null)
            );
            return;
        }

        Cursor cursor = db.getChatHistory(currentUserEmail);

        JSONArray historyArray = new JSONArray();

        try{

            while(cursor.moveToNext()){

                JSONObject item = new JSONObject();

                item.put("id", cursor.getInt(0));
                item.put("message", cursor.getString(1));
                item.put("emotion", cursor.getString(2));

                historyArray.put(item);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        cursor.close();

        String js = "showChatHistory(" + historyArray.toString() + ")";

        webView.post(() -> webView.evaluateJavascript(js,null));
    }

    /* =====================================================
       USER AUTH
    ===================================================== */

    @JavascriptInterface
    public void registerUser(String name,String email,String password){

        boolean success = db.registerUser(name,email,password,"user");

        if(success){

            webView.post(() -> {
                webView.evaluateJavascript("alert('Registration Successful!')",null);
                webView.loadUrl("file:///android_asset/login.html");
            });

        }else{

            webView.post(() ->
                    webView.evaluateJavascript("alert('Email already exists')",null)
            );
        }
    }

    @JavascriptInterface
    public void loginUser(String email,String password){

        String role = db.loginUser(email,password);

        if(role == null){

            webView.post(() ->
                    webView.evaluateJavascript("alert('Invalid email or password')",null)
            );
            return;
        }

        if(role.equals("banned")){

            webView.post(() ->
                    webView.evaluateJavascript("alert('Your account has been banned by admin')",null)
            );
            return;
        }

        currentUserEmail = email;

        SharedPreferences prefs =
                context.getSharedPreferences("session",0);

        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("is_logged_in",true);
        editor.putString("user_email",email);
        editor.putString("user_role",role);

        editor.apply();

        if(role.equals("admin")){

            webView.post(() ->
                    webView.loadUrl("file:///android_asset/admin.html")
            );

        }else{

            webView.post(() ->
                    webView.loadUrl("file:///android_asset/index.html")
            );
        }
    }

    @JavascriptInterface
    public void logout(){

        SharedPreferences prefs =
                webView.getContext().getSharedPreferences("session",0);

        prefs.edit().clear().apply();

        currentUserEmail = null;

        webView.post(() -> {
            webView.clearHistory();
            webView.loadUrl("file:///android_asset/login.html");
        });
    }

    @JavascriptInterface
    public void openRegister(){

        webView.post(() ->
                webView.loadUrl("file:///android_asset/register.html")
        );
    }

    /* =====================================================
   ADMIN DASHBOARD
===================================================== */

    @JavascriptInterface
    public void getStats(){

        int users = db.getUserCount();
        int entries = db.getEmotionCount();

        String json = "{ \"totalUsers\": "+users+", \"totalEntries\": "+entries+" }";

        webView.post(() ->
                webView.evaluateJavascript("showStats("+json+")", null)
        );
    }


/* =====================================================
   ADMIN USERS LIST
===================================================== */

    @JavascriptInterface
    public void getAllUsers(){

        JSONArray arr = new JSONArray();

        Cursor c = db.getAllUsers();

        try{

            while(c.moveToNext()){

                JSONObject obj = new JSONObject();

                obj.put("id", c.getInt(0));
                obj.put("name", c.getString(1));
                obj.put("email", c.getString(2));

                arr.put(obj);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        c.close();

        webView.post(() ->
                webView.evaluateJavascript("showUsers("+arr.toString()+")", null)
        );
    }


/* =====================================================
   ADMIN USER ACTIVITY
===================================================== */

    @JavascriptInterface
    public void getUserActivity(){

        JSONArray arr = new JSONArray();

        Cursor c = db.getAllActivity();

        try{

            while(c.moveToNext()){

                JSONObject obj = new JSONObject();

                obj.put("email", c.getString(0));
                obj.put("problem", c.getString(1));
                obj.put("emotion", c.getString(2));

                arr.put(obj);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        c.close();

        webView.post(() ->
                webView.evaluateJavascript("showActivity("+arr.toString()+")", null)
        );
    }


/* =====================================================
   HIGH RISK USERS
===================================================== */

    @JavascriptInterface
    public void getHighRiskUsers(){

        JSONArray arr = new JSONArray();

        Cursor c = db.getHighRiskUsers();

        try{

            while(c.moveToNext()){

                JSONObject obj = new JSONObject();

                obj.put("email", c.getString(0));
                obj.put("problem", c.getString(1));
                obj.put("emotion", c.getString(2));

                arr.put(obj);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        c.close();

        webView.post(() ->
                webView.evaluateJavascript("showHighRisk("+arr.toString()+")", null)
        );
    }


/* =====================================================
   DELETE USER
===================================================== */

    @JavascriptInterface
    public void deleteUser(int id){

        db.deleteUser(id);

        webView.post(() -> {

            webView.evaluateJavascript("loadUsers()", null);
            webView.evaluateJavascript("loadStats()", null);
            webView.evaluateJavascript("loadActivity()", null);
            webView.evaluateJavascript("loadHighRisk()", null);

        });
    }
    @JavascriptInterface
    public void deleteHistory() {

        db.clearChatHistory(currentUserEmail);

        webView.post(() ->
                webView.evaluateJavascript(
                        "historyCleared()", null
                )
        );
    }

    @JavascriptInterface
    public void deleteSingleChat(int id){

        db.deleteChat(id);

        webView.post(() ->
                webView.evaluateJavascript("openHistory()", null)
        );
    }


}