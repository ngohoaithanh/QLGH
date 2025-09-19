package com.hoaithanh.qlgh.api;

import com.hoaithanh.qlgh.activity.LoginActivity;
import com.hoaithanh.qlgh.model.ApiResult;
import com.hoaithanh.qlgh.model.ApiResultNearby;
import com.hoaithanh.qlgh.model.ApiResultNearbyOrders;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.model.ShipperLocation;

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

    @GET("order/order_for_shipper.php")
    Call<List<DonDatHang>> getOrdersByShipper(@Query("shipperID") int shipperId);

    @GET("order/orders/{id}")
    Call<DonDatHang> getDonDatHangById(@Path("id") String id);

    @GET("order/get_order_by_id.php")
    Call<DonDatHang> getOrderById(@Query("id") int orderId);

    // Cập nhật trạng thái đơn
    @FormUrlEncoded
    @POST("order/update_status.php")
    Call<ApiService.SimpleResult> updateOrderStatus(
            @Field("OrderID") int orderId,
            @Field("Status") String newStatus
    );

    // Response đơn giản
    class SimpleResult {
        public boolean success;
        public String message;
    }

    // Tạo đơn hàng mới
    @FormUrlEncoded
    @POST("order/add_order.php") // đổi đúng tên file PHP mà bạn đang dùng
    Call<ApiResult> createOrder(
            @Field("CustomerName") String customerName,
            @Field("PhoneNumber") String phoneNumber,
            @Field("Pick_up_address") String pickUpAddress,
            @Field("Pick_up_lat") Double pickUpLat,
            @Field("Pick_up_lng") Double pickUpLng,
            @Field("Delivery_address") String deliveryAddress,
            @Field("Delivery_lat") Double deliveryLat,
            @Field("Delivery_lng") Double deliveryLng,
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

    //api for shipper location
    @FormUrlEncoded
    @POST("shipper/update_shipper_location.php")
    Call<ApiResult> updateShipperLocation(
            @Field("shipper_id") int shipperId,
            @Field("lat") double lat,
            @Field("lng") double lng,
            @Field("status") String status
    );

    @GET("shipper/get_shipper_location.php")
    Call<ShipperLocation> getShipperLocation(@Query("shipper_id") int shipperId);

    @GET("shipper/get_nearby_shippers.php")
    Call<ApiResultNearby> getNearbyShippers(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("radius") int radius,
            @Query("limit") int limit
    );

    @GET("order/get_nearby_orders.php")
    Call<ApiResultNearbyOrders> getNearbyOrders(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("radius") int radius,
            @Query("limit") int limit
    );

}
