package com.hoaithanh.qlgh.api;

import com.hoaithanh.qlgh.model.ApiResult;
import com.hoaithanh.qlgh.model.DonDatHang;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;


public interface ApiService {
    @GET("order.php")
    Call<List<DonDatHang>> getDonDatHang();

    @GET("orders/{id}")
    Call<DonDatHang> getDonDatHangById(@Path("id") String id);


    // Tạo đơn hàng mới
    @FormUrlEncoded
    @POST("add_order.php") // đổi đúng tên file PHP mà bạn đang dùng
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
}