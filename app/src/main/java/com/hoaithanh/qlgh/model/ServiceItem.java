package com.hoaithanh.qlgh.model;

public class ServiceItem {
    private String name;
    private String description;
    private int iconResource;

    public ServiceItem(String name, String description, int iconResource) {
        this.name = name;
        this.description = description;
        this.iconResource = iconResource;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getIconResource() {
        return iconResource;
    }
}
