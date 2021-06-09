package com.example.TERA.ui.history;

class HistoryModel {

    double lastLatitude;
    double lastLongitude;
    String lastDateTime;


    public HistoryModel() {

    }


    public double getLastLatitude() {
        return lastLatitude;
    }

    public void setLastLatitude(double lastLatitude) {
        this.lastLatitude = lastLatitude;
    }

    public double getLastLongitude() {
        return lastLongitude;
    }

    public void setLastLongitude(double lastLongitude) {
        this.lastLongitude = lastLongitude;
    }

    public String getLastDateTime() {
        return lastDateTime;
    }

    public void setLastDateTime(String lastDateTime) {
        this.lastDateTime = lastDateTime;
    }
}
