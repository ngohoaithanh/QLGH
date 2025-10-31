package com.hoaithanh.qlgh.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hoaithanh.qlgh.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.1.12/KLTN/api/";
//    private static final String BASE_URL = "http://10.67.116.119/KLTN/api/";
//    private static final String BASE_URL = "http://10.72.49.119/KLTN/api/";
//    private static final String BASE_URL = "http://10.0.2.2/CNMoi/api/order/";

    private static volatile ApiService API;

    public static ApiService getApi() {
        if (API == null) {
            synchronized (RetrofitClient.class) {
                if (API == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(logging)
                            .build();

                    Gson gson = new GsonBuilder()
                            .serializeNulls() // cho phép parse field null
                            .create();

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL) // phải kết thúc bằng /
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();

                    API = retrofit.create(ApiService.class);
                }
            }
        }
        return API;
    }
}