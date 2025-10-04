package com.hoaithanh.qlgh.model;

import java.util.List;

public class ApiResultNearbyOrders {
    public boolean success;
    public int count;
    public String info; // server có thể trả: offline_or_stale / low_rating / max_active_reached / cooldown_XXs
    public List<DonDatHang> orders;
}
