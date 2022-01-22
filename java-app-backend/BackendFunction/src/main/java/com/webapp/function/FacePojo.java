package com.webapp.function;

public class FacePojo {
    private final String imageUrl;
    private final String fullName;

    public FacePojo(String imageUrl, String fullName) {
        this.imageUrl = imageUrl;
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
