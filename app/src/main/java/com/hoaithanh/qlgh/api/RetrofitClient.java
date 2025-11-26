package com.hoaithanh.qlgh.api;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // Import mới
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hoaithanh.qlgh.MainApplication; // Import class Application
import com.hoaithanh.qlgh.session.SessionManager;

import java.io.IOException; // Import mới
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import okhttp3.Interceptor; // Import mới
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response; // Import mới
import okhttp3.ResponseBody; // Import mới
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.1.14/KLTN/api/";
//    private static final String BASE_URL = "https://dalvin.online/api/";

    private static volatile ApiService API;
    private static volatile OkHttpClient okHttpClient;

    public static ApiService getApi() {
        if (okHttpClient == null) {
            synchronized (RetrofitClient.class) {
                if (okHttpClient == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    // --- TẠO INTERCEPTOR ĐỂ GỬI "THẺ" (SESSION ID) ---
                    Interceptor sessionInterceptor = new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            // Lấy session ID đã lưu
                            SessionManager session = new SessionManager(MainApplication.getContext());
                            String sessionId = session.getSessionId();

                            // Gắn "thẻ" vào request
                            Request.Builder requestBuilder = chain.request().newBuilder();
                            if (sessionId != null) {
                                requestBuilder.addHeader("Cookie", "PHPSESSID=" + sessionId);
                            }

                            Response response = chain.proceed(requestBuilder.build());

                            // XỬ LÝ LỖI KHÓA TÀI KHOẢN (authInterceptor)
                            if (!response.isSuccessful()) {
                                ResponseBody body = response.peekBody(Long.MAX_VALUE);
                                if (body != null) {
                                    String bodyString = body.string();
                                    if (bodyString != null && bodyString.contains("ACCOUNT_LOCKED")) {
                                        // Gửi broadcast
                                    }
                                }
                            }
                            return response;
                        }
                    };
                    // --- KẾT THÚC INTERCEPTOR ---

                    okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(logging)
                            .addInterceptor(sessionInterceptor) // <-- SỬ DỤNG INTERCEPTOR MỚI
                            // KHÔNG CẦN DÙNG CookieJar nữa
                            .build();
                }
            }
        }

        // Khởi tạo Retrofit (sử dụng OkHttpClient ở trên) MỘT LẦN
        if (API == null) {
            synchronized (RetrofitClient.class) {
                if (API == null) {
                    Gson gson = new GsonBuilder()
                            .serializeNulls()
                            .create();

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(okHttpClient) // <-- SỬ DỤNG CLIENT ĐÚNG
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();

                    API = retrofit.create(ApiService.class);
                }
            }
        }
        return API;
    }
}