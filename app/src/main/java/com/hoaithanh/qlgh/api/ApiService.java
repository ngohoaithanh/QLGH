package com.hoaithanh.qlgh.api;

import com.hoaithanh.qlgh.activity.LoginActivity;
import com.hoaithanh.qlgh.model.ApiResponse;
import com.hoaithanh.qlgh.model.ApiResult;
import com.hoaithanh.qlgh.model.ApiResultNearby;
import com.hoaithanh.qlgh.model.ApiResultNearbyOrders;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.model.Notification;
import com.hoaithanh.qlgh.model.PricingResponse;
import com.hoaithanh.qlgh.model.ShipperBalanceResponse;
import com.hoaithanh.qlgh.model.ShipperLocation;
import com.hoaithanh.qlgh.model.SimpleResult;
import com.hoaithanh.qlgh.model.Transaction;
import com.hoaithanh.qlgh.model.UnreadCountResponse;
import com.hoaithanh.qlgh.model.UserCheckResponse;
import com.hoaithanh.qlgh.model.Vehicle;

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

    @FormUrlEncoded
    @POST("order/update_order_status.php")
    Call<SimpleResult> updateOrderStatusWithPhoto(
            @Field("order_id") int orderId,
            @Field("new_status") String newStatus,
            @Field("photo_url") String photoUrl
    );

    @FormUrlEncoded
    @POST("order/update_order_status.php")
    Call<SimpleResult> updateOrderStatusWithReason(
            @Field("order_id") int orderId,
            @Field("new_status") String newStatus,
            @Field("reason") String reason // Tham số mới
    );

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
            @Field("fee_payer") String feePayer,
            @Field("Shippingfee") int shippingFee,
            @Field("distance") double distance
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

    // API Lấy Số dư Hiện tại
    @GET("shipper/get_shipper_balance.php") // <-- Đảm bảo đường dẫn đúng
    Call<ShipperBalanceResponse> getShipperBalance(@Query("shipper_id") int shipperId);

    // API Lấy Lịch sử Giao dịch
    @GET("shipper/get_shipper_transactions.php") // <-- Đảm bảo đường dẫn đúng
    Call<List<Transaction>> getShipperTransactions(
            @Query("shipper_id") int shipperId,
            @Query("start_date") String startDate, // Định dạng "YYYY-MM-DD"
            @Query("end_date") String endDate     // Định dạng "YYYY-MM-DD"
    );

    // check sdt truoc khi dang ky tai khoan
    @GET("user/check_user_exists.php")
    Call<UserCheckResponse> checkUserExists(@Query("phone_number") String phoneNumber);

    // register_user
    @FormUrlEncoded
    @POST("user/register_user.php")
    Call<SimpleResult> registerUser(
            @Field("phone_number") String phoneNumber,
            @Field("full_name") String fullName,
            @Field("password") String password
    );

    // update profile
    @FormUrlEncoded
    @POST("user/update_profile.php")
    Call<SimpleResult> updateProfile(
            @Field("user_id") int userId,
            @Field("full_name") String fullName,
            @Field("email") String email,
            @Field("password") String password,
            @Field("old_password") String oldPassword
    );

    @FormUrlEncoded
    @POST("user/update_profile.php")
    Call<SimpleResult> updateAvatar(
            @Field("user_id") int userId,
            @Field("avatar_url") String avatarUrl
    );

    @GET("shipper/get_vehicle_info.php")
    Call<Vehicle> getVehicleInfo(@Query("shipper_id") int shipperId);

    @FormUrlEncoded
    @POST("order/shipper_cancel_order.php")
    Call<SimpleResult> shipperCancelOrder(
            @Field("order_id") int orderId,
            @Field("reason") String reason
    );

    @GET("order/get_active_pricing.php")
    Call<PricingResponse> getActivePricing();

    @GET("notification/get_notifications.php")
    Call<List<Notification>> getNotifications(
            @Query("user_id") int userId,
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("notification/get_unread_count.php")
    Call<UnreadCountResponse> getUnreadCount();

    @POST("notification/mark_read.php")
    Call<SimpleResult> markNotificationsAsRead();
}
