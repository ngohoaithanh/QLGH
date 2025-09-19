package com.hoaithanh.qlgh.model;

public class ShipperLocation {
    private int shipper_id;
    private double lat, lng;
    private String status, updated_at;

    public int getShipper_id(){ return shipper_id; }
    public double getLat(){ return lat; }
    public double getLng(){ return lng; }
    public String getStatus(){ return status; }
    public String getUpdated_at(){ return updated_at; }
}
