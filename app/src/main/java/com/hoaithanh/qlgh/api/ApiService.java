package com.hoaithanh.qlgh.api;

import com.hoaithanh.qlgh.activity.LoginActivity;
import com.hoaithanh.qlgh.model.ApiResult;
import com.hoaithanh.qlgh.model.DonDatHang;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface ApiService {
    @GET("order/order.php")
    Call<List<DonDatHang>> getDonDatHang();

    @GET("order/get_order_by_customer_id.php")
    Call<List<DonDatHang>> getOrdersByCustomerId(@Query("CustomerID") int customerId);

    @GET("order/orders/{id}")
    Call<DonDatHang> getDonDatHangById(@Path("id") String id);


    // Tạo đơn hàng mới
    @FormUrlEncoded
    @POST("order/add_order.php") // đổi đúng tên file PHP mà bạn đang dùng
    Call<ApiResult> createOrder(
            @Field("CustomerName") String customerName,
            @Field("PhoneNumber") String phoneNumber,
            @Field("Pick_up_address") String pickUpAddress,
            @Field("Delivery_address") String deliveryAddress,
            @Field("Recipient") String recipient,
            @Field("RecipientPhone") String recipientPhone,
            @Field("Status") String status,
            @Field("COD_amount") double codAmount,
            @Field("Weight") double weight,
            @Field("Note") String note
    );
    @FormUrlEncoded
    @POST("user/login_user.php") // đổi đường dẫn phù hợp backend của bạn
    Call<LoginActivity.LoginResponse> loginByPhone(
            @Field("phonenumber") String phone,        // tên field đúng theo API PHP
            @Field("password") String password   // gửi password thô
    );

}