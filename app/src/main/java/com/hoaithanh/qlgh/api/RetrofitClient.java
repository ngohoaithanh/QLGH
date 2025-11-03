package com.hoaithanh.qlgh.api;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hoaithanh.qlgh.MainApplication; // <-- SỬA LẠI TÊN CLASS APPLICATION CỦA BẠN
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import okhttp3.Interceptor; // <-- IMPORT MỚI
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Response; // <-- IMPORT MỚI
import okhttp3.ResponseBody; // <-- IMPORT MỚI
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
//    private static final String BASE_URL = "http://192.168.1.14/KLTN/api/";
    private static final String BASE_URL = "http://172.16.2.198/KLTN/api/";
    private static volatile ApiService API;
    private static volatile OkHttpClient okHttpClient;

    public static ApiService getApi() {
        if (okHttpClient == null) {
            synchronized (RetrofitClient.class) {
                if (okHttpClient == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    CookieHandler cookieHandler = new CookieManager(
                            null, CookiePolicy.ACCEPT_ALL
                    );

                    // --- TẠO BỘ LỌC ACCOUNT_LOCKED ---
                    Interceptor authInterceptor = new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Response response = chain.proceed(chain.request());

                            // Chỉ kiểm tra các response lỗi (như 401, 403)
                            if (!response.isSuccessful()) {
                                ResponseBody body = response.body();
                                if (body != null) {
                                    // Đọc body một lần
                                    String bodyString = body.string();

                                    // Kiểm tra xem có phải lỗi khóa tài khoản không
                                    if (bodyString.contains("ACCOUNT_LOCKED")) {
                                        // "Phát loa" cho toàn ứng dụng
                                        Intent intent = new Intent("ACTION_ACCOUNT_LOCKED");
                                        LocalBroadcastManager.getInstance(MainApplication.getContext()).sendBroadcast(intent);
                                    }

                                    // Tạo lại body để các hàm khác vẫn có thể đọc được
                                    ResponseBody newBody = ResponseBody.create(body.contentType(), bodyString);
                                    return response.newBuilder().body(newBody).build();
                                }
                            }
                            return response;
                        }
                    };
                    // --- KẾT THÚC BỘ LỌC ---

                    okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(logging)
                            .addInterceptor(authInterceptor) // <-- THÊM BỘ LỌC MỚI VÀO
                            .cookieJar(new JavaNetCookieJar(cookieHandler))
                            .build();
                }
            }
        }

        if (API == null) {
            synchronized (RetrofitClient.class) {
                if (API == null) {
                    // ... (code tạo Gson và Retrofit giữ nguyên)
                    Gson gson = new GsonBuilder().serializeNulls().create();
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
                    API = retrofit.create(ApiService.class);
                }
            }
        }
        return API;
    }
}