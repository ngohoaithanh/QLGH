package com.hoaithanh.qlgh.widget;

public class StatusUtil {

    public static boolean isCompleted(String status){
        if (status == null) return false;
        switch (status.toLowerCase()){
            case "delivered":
            case "delivery_failed":
            case "cancelled":
                return true;
            default:
                return false;
        }
    }
    public static boolean isOngoing(String status){
        if (status == null) return false;
        switch (status.toLowerCase()){
            case "pending":
            case "accepted":
            case "picked_up":
            case "in_transit":
                return true;
            default:
                return false;
        }
    }


    public static String pretty(String status){
        if (status == null) return "--";
        switch (status.toLowerCase()){
            case "pending": return "Chờ nhận";
            case "accepted": return "Đã nhận";
            case "picked_up": return "Đã lấy hàng";
            case "in_transit": return "Đang giao";
            case "delivered": return "Hoàn thành";
            case "delivery_failed": return "Giao thất bại";
            case "cancelled": return "Đã hủy";
            default: return status;
        }
    }
}
