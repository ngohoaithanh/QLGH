package com.hoaithanh.qlgh.model;

public class CustomerInfo {
    private String UserName;
    private String UserNameId;
    private String ShipportEmail;
    private String PhoneNumberCut;

    // Getters and Setters
    public String getUserName() { return UserName; }
    public void setUserName(String userName) { UserName = userName; }

    public String getUserNameId() { return UserNameId; }
    public void setUserNameId(String userNameId) { UserNameId = userNameId; }

    public String getShipportEmail() { return ShipportEmail; }
    public void setShipportEmail(String shipportEmail) { ShipportEmail = shipportEmail; }

    public String getPhoneNumberCut() { return PhoneNumberCut; }
    public void setPhoneNumberCut(String phoneNumberCut) { PhoneNumberCut = phoneNumberCut; }
}
