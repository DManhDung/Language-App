package com.example.translateapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.translateapp.Model.APIresponse;
import com.example.translateapp.Model.Definitions;
import com.example.translateapp.Model.Meanings;
import com.example.translateapp.Model.Phonetics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SpellCheckerSessionListener{

    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    private boolean isVoiceButton = false, isDicButton =false;
    //private SpellCheckerSession mScs;

    public static EditText editText;
    TextView targetView, downloadModels;
    Button switchLang, button, camButton, spellButton, dicButton, voiceButton,delButton;
    ImageView mic;
    private Spinner sourceLang, targetLang;
    ProgressDialog progressDialog;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText= findViewById(R.id.sourceText);
        targetView= findViewById(R.id.targetText);
        downloadModels = findViewById(R.id.downloadModels);
        button= findViewById(R.id.button);
        switchLang = findViewById(R.id.buttonSwitchLang);
        sourceLang = findViewById(R.id.sourceLangSelector);
        targetLang = findViewById(R.id.targetLangSelector);
        delButton = findViewById(R.id.delButton);
        mic = findViewById(R.id.micButton);
        camButton = findViewById(R.id.cameraButton);
        spellButton = findViewById(R.id.spellButton);
        dicButton = findViewById(R.id.dicButton);
        voiceButton = findViewById(R.id.voiceButton);

        targetView.setMovementMethod(new ScrollingMovementMethod());

        progressDialog = new ProgressDialog(this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);


        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }



        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                editText.setText("");
                editText.setHint("Listening...");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                editText.setHint("Input text here");

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                editText.setText(data.get(0));
                translateEditText();
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        mic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                    speechRecognizer.stopListening();
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLang.getSelectedItem().toString().substring(0,2));
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
                return false;
            }
        });

        final ArrayAdapter<Language> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,this.getAvailableLanguages());
        sourceLang.setAdapter(adapter);
        targetLang.setAdapter(adapter);
        sourceLang.setSelection(adapter.getPosition(new Language ("en")));
        targetLang.setSelection(adapter.getPosition(new Language("vi")));

        switchLang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Editable oldSource = editText.getText();
                int sourceLangPosition = sourceLang.getSelectedItemPosition();
                sourceLang.setSelection(targetLang.getSelectedItemPosition());
                targetLang.setSelection(sourceLangPosition);
                editText.setText(targetView.getText());
                targetView.setText(oldSource);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View it) {
                translateEditText();
            }
        });

        delButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setText("");
                targetView.setText("");
            }
        });

        camButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this,CamereActivity.class);
                startActivity(i);
            }
        });

        spellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchSuggestion(editText.getText().toString());
            }
        });


        dicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RequestManager manager = new RequestManager(MainActivity.this);
                String targetWord = editText.getText().toString();
                isDicButton = true;
                manager.getWordMeaning(listener,targetWord);

            }
        });

        voiceButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                RequestManager manager = new RequestManager(MainActivity.this);
                String targetWord = editText.getText().toString();
                isVoiceButton = true;
                downloadModels.setText("Waiting for audio");
                manager.getWordMeaning(listener,targetWord);
            }
        });

    }


    //Language and translator
    static class Language implements Comparable<Language>{
        private final String code;
        Language(String code){
            this.code=code;
        }
        String getDisplayName(){
            return new Locale(code).getDisplayName();
        }
        public boolean equals(Object o){
            if (o == this){
                return true;
            }
            if(!(o instanceof Language)){
                return false;
            }
            Language otherLang= (Language) o;
            return otherLang.code.equals(code);
        }
        @NonNull
        public String toString(){
            return code+" - "+getDisplayName();
        }
        @Override
        public int hashCode() {
            return code.hashCode();
        }
        @Override
        public int compareTo(@NonNull Language o){
            return this.getDisplayName().compareTo(o.getDisplayName());
        }
    }

    List<Language> getAvailableLanguages(){
        List<Language> languages = new ArrayList<>();
        List<String> languageIds = TranslateLanguage.getAllLanguages();
        for (String languageId: languageIds){
            languages.add(
                    new Language(TranslateLanguage.fromLanguageTag(languageId))
            );
        }
        return languages;
    }

    private void translateEditText()
    {
        final TranslatorOptions translationConfigs = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang.getSelectedItem().toString().substring(0,2))
                .setTargetLanguage(targetLang.getSelectedItem().toString().substring(0,2))
                .build();
        final Translator translator = Translation.getClient(translationConfigs);
        if(editText.getText().length() != 0){
            translator.downloadModelIfNeeded().addOnSuccessListener(new OnSuccessListener<Void>() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccess(Void it) {
                    downloadModels.setText("Download Successful");

                }
            }).addOnFailureListener(new OnFailureListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onFailure(@NonNull Exception it) {
                    downloadModels.setText("Please wait");
                }
            });
            translator.translate(editText.getText().toString()).addOnSuccessListener(new OnSuccessListener<String>() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccess(String s) {
                    targetView.setText(s);
                    downloadModels.setText("Translated");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onFailure(@NonNull Exception e) {
                    downloadModels.setText("Model needed");
                    e.printStackTrace();
                }
            });
        }
    }


    public static void setTarget(String s){
        editText.setText(s);
    }


    //Grammar checker
    private void fetchSuggestion(String input){
        TextServicesManager tsm = (TextServicesManager) getSystemService(TEXT_SERVICES_MANAGER_SERVICE);
        SpellCheckerSession session = tsm.newSpellCheckerSession(null, Locale.ENGLISH, this, true);

        session.getSentenceSuggestions(new TextInfo[]{ new TextInfo(input) }, 5);
    }


    @Override
    public void onGetSuggestions(SuggestionsInfo[] arg0) {

    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] arg0) {
        final StringBuffer sb = new StringBuffer("");

        for (SentenceSuggestionsInfo result : arg0) {
            int len = result.getSuggestionsCount();

            for(int i=0; i < len; i++){
                int m = result.getSuggestionsInfoAt(i).getSuggestionsCount();

                for(int k=0; k < m; k++) {
                    sb.append(result.getSuggestionsInfoAt(i).getSuggestionAt(k))
                            .append("\n");
                }
                sb.append("\n");
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                targetView.setText(sb.toString());
            }
        });
    }


    //Dictionary and Phonetic
    private final OnFetchDataListener listener = new OnFetchDataListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onFetchData(APIresponse apiResponse, String message) {
            progressDialog.dismiss();

            if(apiResponse==null){
                Toast.makeText(MainActivity.this, "No Data Found", Toast.LENGTH_SHORT).show();

                return;
            }
            if (isDicButton){
                showData(apiResponse);
                isDicButton=false;

            }
            if (isVoiceButton){
                playPhonetic(apiResponse);
                isVoiceButton=false;
            }

        }

        @Override
        public void onError(String message) {
            progressDialog.dismiss();
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

    private void showData(APIresponse apiResponse) {
        final StringBuffer sb = new StringBuffer("");
        List<Meanings> meaningsList = apiResponse.getMeanings();
        int meaningSize = meaningsList.size();

        sb.append("Word: ").append(apiResponse.getWord()).append("\n");
        for(int i=0;i<meaningSize;i++){
            sb.append("Parts of Speech: ").append(meaningsList.get(i).getPartOfSpeech())
                    .append("\n").append("\n");
            List<Definitions> defList = meaningsList.get(i).getDefinitions();
            int defSize = defList.size();
            for (int j =0; j<defSize;j++){
                sb.append("Definition: ").append(defList.get(j).getDefinitions())
                        .append("\n");
                if(!defList.get(j).getExample().isEmpty()){
                    sb.append("Example: ").append(defList.get(j).getExample())
                            .append("\n");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        targetView.setText("");
        targetView.append(sb);
    }

    private void playPhonetic(APIresponse apiResponse)
    {
        String targetToPlay = editText.getText().toString();
        List<Phonetics> phonetics = apiResponse.getPhonetics();
        MediaPlayer player = new MediaPlayer();

        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build());
        try {
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            int phoneSize = phonetics.size();
            for (int i =0;i<phoneSize;i++)
            {
                if (!phonetics.get(i).getAudio().equals("")){
                    player.setDataSource(phonetics.get(i).getAudio());
                    targetView.setText(phonetics.get(i).getText());
                    break;
                }
            }
            player.prepareAsync();
            downloadModels.setText("");
        } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(this, "Couldn't Play audio", Toast.LENGTH_SHORT).show();
        }

    }


    //Record audio
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
    }

}