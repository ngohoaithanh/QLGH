package com.hoaithanh.qlgh.api;

import com.hoaithanh.qlgh.activity.LoginActivity;
import com.hoaithanh.qlgh.model.ApiResponse;
import com.hoaithanh.qlgh.model.ApiResult;
import com.hoaithanh.qlgh.model.ApiResultNearby;
import com.hoaithanh.qlgh.model.ApiResultNearbyOrders;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.model.ShipperLocation;
import com.hoaithanh.qlgh.model.SimpleResult;

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

//    @GET("order/orders/{id}")
//    Call<DonDatHang> getDonDatHangById(@Path("id") String id);

    @GET("order/get_order_by_id.php")
    Call<DonDatHang> getOrderById(@Query("id") int orderId);

    // Cập nhật trạng thái đơn
//    @FormUrlEncoded
//    @POST("order/update_status.php")
//    Call<ApiService.SimpleResult> updateOrderStatus(
//            @Field("OrderID") int orderId,
//            @Field("Status") String newStatus
//    );

    @FormUrlEncoded
    @POST("order/update_order_status.php") // Đường dẫn tới file PHP của bạn
    Call<SimpleResult> updateOrderStatus(
            @Field("order_id") int orderId,
            @Field("new_status") String newStatus
    );

    // Response đơn giản
//    class SimpleResult {
//        public boolean success;
//        public String message;
//    }

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
            @Field("Note") String note,
            @Field("fee_payer") String feePayer
    );
    @FormUrlEncoded
    @POST("user/login_user.php") // đổi đường dẫn phù hợp backend của bạn
    Call<LoginActivity.LoginResponse> loginByPhone(
            @Field("phonenumber") String phone,        // tên field đúng theo API PHP
            @Field("password") String password   // gửi password thô
    );

    //api for shipper location

    // Cập nhật vị trí shipper (online/offline/busy)
    @FormUrlEncoded
    @POST("shipper/update_location.php")
    Call<ApiResult> updateShipperLocation(
            @Field("shipper_id") int shipperId,
            @Field("lat") double lat,
            @Field("lng") double lng,
            @Field("status") String status
    );

    // Hàm mới (có thêm reason)
    @FormUrlEncoded
    @POST("order/update_order_status.php")
    Call<SimpleResult> updateOrderStatusWithReason(
            @Field("order_id") int orderId,
            @Field("new_status") String newStatus,
            @Field("reason") String reason // Tham số mới
    );

    // Lấy đơn gần (đã lọc online+fresh+fairness server-side)
    @GET("order/get_nearby_orders.php")
    Call<ApiResultNearbyOrders> getNearbyOrders(
            @Query("shipper_id") int shipperId,
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("radius") int radiusMeters,
            @Query("limit") int limit
    );

    // Nhận đơn (race-safe)
    @FormUrlEncoded
    @POST("order/accept_order.php")
    Call<ApiResult> acceptOrder(
            @Field("order_id") int orderId,
            @Field("shipper_id") int shipperId
    );
//    @FormUrlEncoded
//    @POST("shipper/update_shipper_location.php")
//    Call<ApiResult> updateShipperLocation(
//            @Field("shipper_id") int shipperId,
//            @Field("lat") double lat,
//            @Field("lng") double lng,
//            @Field("status") String status
//    );

    @GET("shipper/get_shipper_location.php")
    Call<ShipperLocation> getShipperLocation(@Query("shipper_id") int shipperId);

    @GET("shipper/get_nearby_shippers.php")
    Call<ApiResultNearby> getNearbyShippers(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("radius") int radius,
            @Query("limit") int limit
    );

//    @GET("order/get_nearby_orders.php")
//    Call<ApiResultNearbyOrders> getNearbyOrders(
//            @Query("lat") double lat,
//            @Query("lng") double lng,
//            @Query("radius") int radius,
//            @Query("limit") int limit
//    );

    @FormUrlEncoded
    @POST("shipper/submit_shipper_rating.php") // <-- Đảm bảo đường dẫn này đúng
    Call<SimpleResult> submitShipperRating(
            @Field("shipper_id") int shipperId,
            @Field("order_id") int orderId, // <-- THÊM DÒNG NÀY
            @Field("rating") float rating
    );

    @FormUrlEncoded
    @POST("order/cancel_order.php") // <-- Đảm bảo đường dẫn này đúng
    Call<SimpleResult> cancelOrder(@Field("order_id") int orderId);

}
