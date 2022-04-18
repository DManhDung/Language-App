package com.example.translateapp;

import com.example.translateapp.Model.APIresponse;

public interface OnFetchDataListener {
    void onFetchData(APIresponse apiResponse, String message);
    void onError(String message);
}
