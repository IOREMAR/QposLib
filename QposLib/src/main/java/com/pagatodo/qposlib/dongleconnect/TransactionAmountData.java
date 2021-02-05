package com.pagatodo.qposlib.dongleconnect;

import android.util.ArrayMap;

import androidx.annotation.NonNull;

import com.dspread.xpos.QPOSService;

import java.util.Map;

public class TransactionAmountData {

    private String nameProduct;
    private String pathIcon;
    private String amount;
    private String cashbackAmount;
    private String currencyCode;
    private String transactionType;

    private String tipoOperacion;
    private Integer decimales;
    private String importeOperacion;
    private String importeCashback;
    private String importePropina;
    private String importeImpuestos;
    private String comision;
    private String codigoPostal;
    private String AmountIcon;

    public String getAmountIcon() {
        return AmountIcon;
    }

    public void setAmountIcon(String amountIcon) {
        AmountIcon = amountIcon;
    }

    public Map<String, String> getCapacidades() {
        return capacidades;
    }

    public void setCapacidades(Map<String, String> capacidades) {
        this.capacidades = capacidades;
    }

    private Map<String, String> capacidades = new ArrayMap<>();

    public String getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(final String tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public TransactionAmountData() {

        //Empty constructor
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(final String amount) {

        this.amount = amount;
    }

    public String getCashbackAmount() {
        return cashbackAmount;
    }

    public void setCashbackAmount(final String cashbackAmount) {
        this.cashbackAmount = cashbackAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(final String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public QPOSService.TransactionType getTransactionType() {
        if (transactionType.equalsIgnoreCase("CASHBACK")) {
            return QPOSService.TransactionType.CASHBACK;
        } else if (transactionType.equalsIgnoreCase("REFUND")) {
            return QPOSService.TransactionType.REFUND;
        } else if (transactionType.equalsIgnoreCase("INQUIRY")) {
            return QPOSService.TransactionType.INQUIRY;
        } else {
            return QPOSService.TransactionType.GOODS;
        }
    }

    public void setTransactionType(final String transactionType) {
        this.transactionType = transactionType;
    }

    public String getNameProduct() {
        return nameProduct;
    }

    public void setNameProduct(final String nameProduct) {
        this.nameProduct = nameProduct;
    }

    public String getPathIcon() {
        return pathIcon;
    }

    public void setPathIcon(final String pathIcon) {
        this.pathIcon = pathIcon;
    }


    public String getImporteOperacion() {
        return importeOperacion;
    }

    public void setImporteOperacion(final String importeOperacion) {
        this.importeOperacion = importeOperacion;
    }

    public String getImporteCashback() {
        return importeCashback;
    }

    public void setImporteCashback(final String importeCashback) {
        this.importeCashback = importeCashback;
    }

    public String getImportePropina() {
        return importePropina;
    }

    public void setImportePropina(final String importePropina) {
        this.importePropina = importePropina;
    }

    public String getImporteImpuestos() {
        return importeImpuestos;
    }

    public void setImporteImpuestos(final String importeImpuestos) {
        this.importeImpuestos = importeImpuestos;
    }

    public String getComision() {
        return comision;
    }

    public void setComision(final String comision) {
        this.comision = comision;
    }

    public Integer getDecimales() {
        return decimales;
    }

    public void setDecimales(final Integer decimales) {
        this.decimales = decimales;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(final String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    @NonNull
    @Override
    public String toString() {
        return "{\"nameProduct\" : " + (nameProduct == null ? null : "\"" + nameProduct + "\"")
                + ",\"pathIcon\" : " + (pathIcon == null ? null : "\"" + pathIcon + "\"")
                + ",\"amount\" : " + (amount == null ? null : "\"" + amount + "\"")
                + ",\"cashbackAmount\" : " + (cashbackAmount == null ? null : "\"" + cashbackAmount + "\"")
                + ",\"currencyCode\" : " + (currencyCode == null ? null : "\"" + currencyCode + "\"")
                + ",\"transactionType\" : " + (transactionType == null ? null : "\"" + transactionType + "\"")
                + ",\"tipoOperacion\" : " + (tipoOperacion == null ? null : "\"" + tipoOperacion + "\"")
                + ",\"decimales\" : " + decimales
                + ",\"importeOperacion\" : " + (importeOperacion == null ? null : "\"" + importeOperacion + "\"")
                + ",\"importeCashback\" : " + (importeCashback == null ? null : "\"" + importeCashback + "\"")
                + ",\"importePropina\" : " + (importePropina == null ? null : "\"" + importePropina + "\"")
                + ",\"importeImpuestos\" : " + (importeImpuestos == null ? null : "\"" + importeImpuestos + "\"")
                + ",\"comision\" : " + (comision == null ? null : "\"" + comision + "\"")
                + ",\"codigoPostal\" : " + (codigoPostal == null ? null : "\"" + codigoPostal + "\"")
                + ",\"AmountIcon\" : " + (AmountIcon == null ? null : "\"" + AmountIcon + "\"")
                + "}";
    }
}