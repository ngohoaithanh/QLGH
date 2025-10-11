package com.hoaithanh.qlgh.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Entity(tableName = "don_dat_hang")
public class DonDatHang implements Serializable {
    public double distance = -1;
    @SerializedName("ID")
    @NonNull
    @PrimaryKey
    private String ID;

    @SerializedName("CustomerID")
    private String CustomerID;

    @SerializedName("ShipperID")
    private String ShipperID; // có thể null

    @SerializedName("Pick_up_address")
    private String Pick_up_address;

    @SerializedName("Pick_up_lat")
    private String Pick_up_lat;

    @SerializedName("Pick_up_lng")
    private String Pick_up_lng;

    @SerializedName("Recipient")
    private String Recipient;

    @SerializedName("RecipientPhone")
    private String RecipientPhone;

    @SerializedName("Delivery_address")
    private String Delivery_address;

    @SerializedName("Delivery_lat")
    private String Delivery_lat;

    @SerializedName("Delivery_lng")
    private String Delivery_lng;

    @SerializedName("Status")
    private String Status;

    @SerializedName("COD_amount")
    private String COD_amount;

    @SerializedName("Shippingfee")
    private String Shippingfee;

    @SerializedName("CODFee")
    private String CODFee;

    @SerializedName("Weight")
    private String Weight;

    @SerializedName("Created_at")
    private String Created_at;

    @SerializedName("Accepted_at")
    private String Accepted_at;

    @SerializedName("Note")
    private String Note;

    @SerializedName("UserName")
    private String UserName;

    @SerializedName("CustomerEmail")
    private String CustomerEmail;

    @SerializedName("ShipperName")
    private String ShipperName;

    @SerializedName("ShipperEmail")
    private String ShipperEmail;

    @SerializedName("PhoneNumberCus")
    private String PhoneNumberCus;

    @SerializedName("hint_feasible") public Boolean hint_feasible;
    @SerializedName("hint_reason")   public String  hint_reason;


    // Getters and Setters
    public String getID() { return ID; }
    public void setID(String ID) { this.ID = ID; }

    public String getCustomerID() { return CustomerID; }
    public void setCustomerID(String customerID) { CustomerID = customerID; }

    public String getShipperID() { return ShipperID; }
    public void setShipperID(String shipperID) { ShipperID = shipperID; }

    public String getPick_up_address() { return Pick_up_address; }

    public String getPick_up_lat() {
        return Pick_up_lat;
    }

    public void setPick_up_lat(String pick_up_lat) {
        Pick_up_lat = pick_up_lat;
    }

    public String getPick_up_lng() {
        return Pick_up_lng;
    }

    public void setPick_up_lng(String pick_up_lng) {
        Pick_up_lng = pick_up_lng;
    }

    public String getDelivery_lat() {
        return Delivery_lat;
    }

    public void setDelivery_lat(String delivery_lat) {
        Delivery_lat = delivery_lat;
    }

    public String getDelivery_lng() {
        return Delivery_lng;
    }

    public void setDelivery_lng(String delivery_lng) {
        Delivery_lng = delivery_lng;
    }

    public void setPick_up_address(String pick_up_address) { Pick_up_address = pick_up_address; }

    public String getRecipient() { return Recipient; }
    public void setRecipient(String recipient) { Recipient = recipient; }

    public String getRecipientPhone() { return RecipientPhone; }
    public void setRecipientPhone(String recipientPhone) { RecipientPhone = recipientPhone; }

    public String getDelivery_address() { return Delivery_address; }
    public void setDelivery_address(String delivery_address) { Delivery_address = delivery_address; }

    public String getStatus() { return Status; }
    public void setStatus(String status) { Status = status; }

    public String getCOD_amount() { return COD_amount; }
    public void setCOD_amount(String COD_amount) { this.COD_amount = COD_amount; }

    public String getShippingfee() { return Shippingfee == null ? "0" : Shippingfee; }
    public void setShippingfee(String shippingfee) { Shippingfee = shippingfee; }

    public String getCODFee() {
        return CODFee == null ? "0" : CODFee;
    }

    public void setCODFee(String CODFee) {
        this.CODFee = CODFee;
    }

    public String getWeight() { return Weight; }
    public void setWeight(String weight) { Weight = weight; }

    public String getCreated_at() { return Created_at; }
    public void setCreated_at(String created_at) { Created_at = created_at; }

    public String getAccepted_at() { return Accepted_at; }
    public void setAccepted_at(String accepted_at) { Accepted_at = accepted_at; }

    public String getNote() { return Note; }
    public void setNote(String note) { Note = note; }

    public String getUserName() { return UserName; }
    public void setUserName(String userName) { UserName = userName; }

    public String getCustomerEmail() { return CustomerEmail; }
    public void setCustomerEmail(String customerEmail) { CustomerEmail = customerEmail; }

    public String getShipperName() { return ShipperName; }
    public void setShipperName(String shipperName) { ShipperName = shipperName; }

    public String getShipperEmail() { return ShipperEmail; }
    public void setShipperEmail(String shipperEmail) { ShipperEmail = shipperEmail; }

    public String getPhoneNumberCus() { return PhoneNumberCus; }
    public void setPhoneNumberCus(String phoneNumberCus) { PhoneNumberCus = phoneNumberCus; }

    // ==== Tiện ích chuyển đổi an toàn ====
    public double getWeightAsDouble() {
        try { return Double.parseDouble(Weight); } catch (Exception e) { return 0.0; }
    }
    public double getCodAmountAsDouble() {
        try { return Double.parseDouble(COD_amount); } catch (Exception e) { return 0.0; }
    }
    public double getShippingFeeAsDouble() {
        try { return Double.parseDouble(Shippingfee); } catch (Exception e) { return 0.0; }
    }

    // ==== Công thức tính phí (y hệt API) ====
    public static int calculateShippingFee(double weight) {
        if (weight < 1.0) {
            return 15000;
        } else if (weight <= 2.0) {
            return 18000;
        } else {
            double extraWeight = weight - 2.0;
            int extraFee = (int) (Math.ceil(extraWeight * 2) * 2500);
            return 18000 + extraFee;
        }
    }
}