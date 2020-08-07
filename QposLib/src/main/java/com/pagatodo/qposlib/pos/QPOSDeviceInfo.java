package com.pagatodo.qposlib.pos;

import androidx.annotation.NonNull;

public class QPOSDeviceInfo {

    private String isSupportedTrack2;
    private String isKeyboard;
    private String batteryPercentage;
    private String updateWorkKeyFlag;
    private String bootloaderVersion;
    private String isSupportedTrack3;
    private String batteryLevel;
    private String hardwareVersion;
    private String isCharging;
    private String firmwareVersion;
    private String isSupportedTrack1;
    private String isUsbConnected;

    public QPOSDeviceInfo() {
        //none
    }

    public String getIsSupportedTrack2() {
        return isSupportedTrack2;
    }

    public void setIsSupportedTrack2(final String isSupportedTrack2) {
        this.isSupportedTrack2 = isSupportedTrack2;
    }

    public String getIsKeyboard() {
        return isKeyboard;
    }

    public void setIsKeyboard(final String isKeyboard) {
        this.isKeyboard = isKeyboard;
    }

    public String getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(final String batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public String getUpdateWorkKeyFlag() {
        return updateWorkKeyFlag;
    }

    public void setUpdateWorkKeyFlag(final String updateWorkKeyFlag) {
        this.updateWorkKeyFlag = updateWorkKeyFlag;
    }

    public String getBootloaderVersion() {
        return bootloaderVersion;
    }

    public void setBootloaderVersion(final String bootloaderVersion) {
        this.bootloaderVersion = bootloaderVersion;
    }

    public String getIsSupportedTrack3() {
        return isSupportedTrack3;
    }

    public void setIsSupportedTrack3(final String isSupportedTrack3) {
        this.isSupportedTrack3 = isSupportedTrack3;
    }

    public String getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(final String batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(final String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public String getIsCharging() {
        return isCharging;
    }

    public void setIsCharging(final String isCharging) {
        this.isCharging = isCharging;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(final String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getIsSupportedTrack1() {
        return isSupportedTrack1;
    }

    public void setIsSupportedTrack1(final String isSupportedTrack1) {
        this.isSupportedTrack1 = isSupportedTrack1;
    }

    public String getIsUsbConnected() {
        return isUsbConnected;
    }

    public void setIsUsbConnected(final String isUsbConnected) {
        this.isUsbConnected = isUsbConnected;
    }

    @NonNull
    @Override
    public String toString() {
        return "{\"isSupportedTrack2\" : " + (isSupportedTrack2 == null ? null : "\"" + isSupportedTrack2 + "\"")
                + ",\"isKeyboard\" : " + (isKeyboard == null ? null : "\"" + isKeyboard + "\"")
                + ",\"batteryPercentage\" : " + (batteryPercentage == null ? null : "\"" + batteryPercentage + "\"")
                + ",\"updateWorkKeyFlag\" : " + (updateWorkKeyFlag == null ? null : "\"" + updateWorkKeyFlag + "\"")
                + ",\"bootloaderVersion\" : " + (bootloaderVersion == null ? null : "\"" + bootloaderVersion + "\"")
                + ",\"isSupportedTrack3\" : " + (isSupportedTrack3 == null ? null : "\"" + isSupportedTrack3 + "\"")
                + ",\"batteryLevel\" : " + (batteryLevel == null ? null : "\"" + batteryLevel + "\"")
                + ",\"hardwareVersion\" : " + (hardwareVersion == null ? null : "\"" + hardwareVersion + "\"")
                + ",\"isCharging\" : " + (isCharging == null ? null : "\"" + isCharging + "\"")
                + ",\"firmwareVersion\" : " + (firmwareVersion == null ? null : "\"" + firmwareVersion + "\"")
                + ",\"isSupportedTrack1\" : " + (isSupportedTrack1 == null ? null : "\"" + isSupportedTrack1 + "\"")
                + ",\"isUsbConnected\" : " + (isUsbConnected == null ? null : "\"" + isUsbConnected + "\"")
                + "}";
    }
}