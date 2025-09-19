package com.hoaithanh.qlgh.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GoongRetrofitClient {
    private static volatile Retrofit INSTANCE;

    public static Retrofit getInstance() {
        if (INSTANCE == null) {
            synchronized (GoongRetrofitClient.class) {
                if (INSTANCE == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(logging)
                            .build();

                    INSTANCE = new Retrofit.Builder()
                            .baseUrl("https://rsapi.goong.io/") // <- nhớ dấu /
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(client)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
