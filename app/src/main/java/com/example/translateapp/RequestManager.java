package com.example.translateapp;

import android.content.Context;

import com.example.translateapp.Model.APIresponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class RequestManager {
    Context context;

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.dictionaryapi.dev/api/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    public RequestManager(Context context) {
        this.context = context;
    }

    public void getWordMeaning(OnFetchDataListener listener, String word){
        CallDictionary callDictionary = retrofit.create(CallDictionary.class);
        Call<List<APIresponse>> call = callDictionary.callMeanings(word);

        try{
            call.enqueue(new Callback<List<APIresponse>>() {
                @Override
                public void onResponse(Call<List<APIresponse>> call, Response<List<APIresponse>> response) {
                    if (!response.isSuccessful()){
                        return;
                    }
                    listener.onFetchData(response.body().get(0),response.message());
                }

                @Override
                public void onFailure(Call<List<APIresponse>> call, Throwable t) {
                    listener.onError("Request Failed");

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface CallDictionary{
        @GET("entries/en/{word}")
        Call<List<APIresponse>> callMeanings(
                @Path("word") String word
        );
    }
}
