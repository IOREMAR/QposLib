package com.pagatodo.qposlib;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.dspread.xpos.QPOSService;
import com.pagatodo.qposlib.abstracts.AbstractDongle;
import com.pagatodo.qposlib.dongleconnect.AplicacionEmv;
import com.pagatodo.qposlib.dongleconnect.DongleConnect;
import com.pagatodo.qposlib.dongleconnect.DongleListener;
import com.pagatodo.qposlib.dongleconnect.TransactionAmountData;
import com.pagatodo.qposlib.pos.ICCDecodeData;
import com.pagatodo.qposlib.pos.PosResult;
import com.pagatodo.qposlib.pos.QPOSDeviceInfo;
import com.pagatodo.qposlib.pos.dspread.DspreadDevicePOS;
import com.pagatodo.qposlib.pos.sunmi.ByteUtil;
import com.pagatodo.qposlib.pos.sunmi.Constants;
import com.pagatodo.qposlib.pos.sunmi.EmvUtil;
import com.pagatodo.qposlib.pos.sunmi.TLV;
import com.pagatodo.qposlib.pos.sunmi.TLVUtil;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.AidlErrorCodeV2;
import com.sunmi.pay.hardware.aidlv2.bean.EMVCandidateV2;
import com.sunmi.pay.hardware.aidlv2.bean.EMVTransDataV2;
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2;
import com.sunmi.pay.hardware.aidlv2.emv.EMVListenerV2;
import com.sunmi.pay.hardware.aidlv2.emv.EMVOptV2;
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2;
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadOptV2;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;
import com.sunmi.pay.hardware.aidlv2.readcard.ReadCardOptV2;
import com.sunmi.pay.hardware.aidlv2.security.SecurityOptV2;
import com.sunmi.pay.hardware.aidlv2.system.BasicOptV2;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import sunmi.paylib.SunmiPayKernel;

import org.apache.commons.lang3.ArrayUtils;

import static com.pagatodo.qposlib.QPosManager.ADITIONAL_CAPS;
import static com.pagatodo.qposlib.QPosManager.COUNTRY_CODE;
import static com.pagatodo.qposlib.QPosManager.CURRENCY_CODE;
import static com.pagatodo.qposlib.QPosManager.CVM_LIMIT;
import static com.pagatodo.qposlib.QPosManager.TERMINAL_CAPS;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

public class SunmiPosManager extends AbstractDongle {

    private static final String TAG = SunmiPosManager.class.getSimpleName();
    private SunmiPayKernel mSMPayKernel;

    public static BasicOptV2 mBasicOptV2;
    public static ReadCardOptV2 mReadCardOptV2;
    public static PinPadOptV2 mPinPadOptV2;
    public static SecurityOptV2 mSecurityOptV2;
    public static EMVOptV2 mEMVOptV2;
    private static final String[] TAGSEMV = new String[]{"5f2a", "82", "95", "9a", "9c", "9f02", "9f03", "9f10", "9f1a", "9f26", "9f27", "9f33", "9f34", "9f35", "9f36", "9f37", "9f40", "5a", "5f34", "57", "9f07", "5f28", "56", "84", "9f09", "9f41", "9f53", "5f20", "5", "9f1e", "9f12"};
    private Map<String, String> emvTags = new ArrayMap<>();
    private Map<String, String> configMap;
    private int mCardType;  // card type
    private String mCardNo = "";
    private byte[] hexStrPin;
    private String amount;
    private int mAppSelect;
    private boolean fallbackActivado = false;
    private int mPinType;   // 0-online pin, 1-offline pin

    private String track1 = "";
    private String track2 = "";

    private TransactionAmountData transactionAmountData;

    private SunmiPosManager() {
    }

    public SunmiPosManager(DongleConnect listener) {
        super(listener);
        this.setPosSunmi(this);
        connectPayService();
    }

    @Override
    public void setFallBack(boolean isFallback) {
        this.fallbackActivado = isFallback;
    }

    public static boolean isSunmiDevice() {
        Intent intent = new Intent("sunmi.intent.action.PAY_HARDWARE");
        intent.setPackage("com.sunmi.pay.hardware_v3");

        PackageManager pkgManager = PosInstance.getInstance().getAppContext().getPackageManager();
        List<ResolveInfo> infos = pkgManager.queryIntentServices(intent, 0);

        return infos != null && !infos.isEmpty();
    }

    private void connectPayService() {
        mSMPayKernel = SunmiPayKernel.getInstance();
        mSMPayKernel.initPaySDK(PosInstance.getInstance().getAppContext(), new SunmiPayKernel.ConnectCallback() {

            @Override
            public void onConnectPaySDK() {
                try {
                    mEMVOptV2 = mSMPayKernel.mEMVOptV2;
                    mBasicOptV2 = mSMPayKernel.mBasicOptV2;
                    mPinPadOptV2 = mSMPayKernel.mPinPadOptV2;
                    mReadCardOptV2 = mSMPayKernel.mReadCardOptV2;
                    mSecurityOptV2 = mSMPayKernel.mSecurityOptV2;
                    mAppSelect = 0;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnectPaySDK() {
                dongleConnect.ondevicedisconnected();
            }
        });
    }

    public void initData(final Map<String, String> capabilities) {
        // The india country
        configMap = EmvUtil.getConfig(capabilities);
        EmvUtil.initKey(mSecurityOptV2);
        EmvUtil.initAidAndRid(mEMVOptV2);
        EmvUtil.setTerminalParam(configMap, mEMVOptV2);
    }

    public void startProcessEmv(String monto) {
//        AppLogger.LOGGER.fine(TAG, "***************************************************************");
//        AppLogger.LOGGER.fine(TAG, "****************************Start Process**********************");
//        AppLogger.LOGGER.fine(TAG, "***************************************************************");
        amount = monto;
        try {
            // Before check card, initialize emv process(clear all TLV)
            SunmiPosManager.mEMVOptV2.initEmvProcess();
            long parseLong = Long.parseLong(amount);
            if (parseLong > 0) {
                checkCard();
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkCard() {
        try {
            int mCardType = AidlConstantsV2.CardType.MAGNETIC.getValue() | AidlConstantsV2.CardType.IC.getValue() | AidlConstantsV2.CardType.NFC.getValue();
            mReadCardOptV2.checkCard(mCardType, mCheckCardCallback, 60);
        } catch (Exception exe) {
            cancelprocess();

        }
    }

    private void cancelprocess() {
        try {
            mReadCardOptV2.cancelCheckCard();
        } catch (Exception exe) {
        }
    }

    public void transactProcess() {
        try {
            EMVTransDataV2 emvTransData = new EMVTransDataV2();
            emvTransData.amount = amount;
            emvTransData.flowType = 1;
            emvTransData.cardType = mCardType;
            mEMVOptV2.transactProcess(emvTransData, mEMVListener);
        } catch (Exception exe) {
            cancelprocess();
        }
    }

    private Hashtable<String, String> getDataOpTarjeta(final Map<String, TLV> mapTAGS) { //NOSONAR
        Hashtable<String, String> resultData = new Hashtable<>();
        resultData.put("maskedPAN", mapTAGS.containsKey(ICCDecodeData.ENC_PAN.getLabel()) ? mapTAGS.get(ICCDecodeData.ENC_PAN.getLabel()).getValue() : "");
        resultData.put("encTrack1", mapTAGS.containsKey(ICCDecodeData.ENC_TRACK_1.getLabel()) ? mapTAGS.get(ICCDecodeData.ENC_TRACK_1.getLabel()).getValue() : "");
        resultData.put("encTrack2", mapTAGS.containsKey(ICCDecodeData.ENC_TRACK_2.getLabel()) ? mapTAGS.get(ICCDecodeData.ENC_TRACK_2.getLabel()).getValue() : "");
        resultData.put("encTrack3", mapTAGS.containsKey(ICCDecodeData.ENC_TRACK_3.getLabel()) ? mapTAGS.get(ICCDecodeData.ENC_TRACK_3.getLabel()).getValue() : "");
        resultData.put("cardholderName", mapTAGS.containsKey(ICCDecodeData.CARDHOLDER_NAME.getLabel()) ? mapTAGS.get(ICCDecodeData.CARDHOLDER_NAME.getLabel()).getValue() : "");
        resultData.put("iccdata", getHexEmvtags(mapTAGS));
        resultData.put("serviceCode", mapTAGS.containsKey(ICCDecodeData.SERVICE_CODE.getLabel()) ? mapTAGS.get(ICCDecodeData.ENC_PAN.getLabel()).getValue() : "");
        resultData.put("pinBlock", hexStrPin != null ? new String(hexStrPin) : "");
        if (mapTAGS.containsKey("57")) {
            final int index = mapTAGS.get("57").getValue().indexOf("D") + 1;
            final int endIndex = index + 4;
            resultData.put("expiryDate", mapTAGS.get("57").getValue().substring(index, endIndex));
        }
        return resultData;
    }

    private byte[] getTagEncrypt(final byte[] selectTAG, final int keyIndex) throws RemoteException {
        byte[] dataIn = createBytePadding(selectTAG);
        byte[] dataOut = new byte[dataIn.length];
        int result = mSecurityOptV2.dataEncrypt(keyIndex, dataIn, AidlConstantsV2.Security.DATA_MODE_ECB, null, dataOut);
        if (result == 0) {
            return dataOut;
        } else {
            return new byte[0];
        }
    }

    private Hashtable<String, String> getDataOpTarjeta(final Bundle mapTAGS) {//NOSONAR
        mCardType = AidlConstantsV2.CardType.MAGNETIC.getValue();
        track1 = mapTAGS.getString(Constants.TRACK1);
        track2 = mapTAGS.getString(Constants.TRACK2);
        String track3 = mapTAGS.getString(Constants.TRACK3) != null ? mapTAGS.getString(Constants.TRACK3) : "";

        String inicioNombre = track1.substring(18);
        String cardHolderName = inicioNombre.substring(0, inicioNombre.indexOf("^"));

        String serviceCode = "";
        if (track2 != null) {
            int index = track2.indexOf("=");
            if (index != -1) {
                mCardNo = track2.substring(0, index);
                serviceCode = track2.substring(index + 5, index + 8);
            }
        }

        Hashtable<String, String> resultData = new Hashtable<>();

        resultData.put(Constants.maskedPAN, track1.substring(track1.indexOf('B') + 1, track1.indexOf('^')));
        resultData.put(Constants.encTrack1, "%".concat(track1).concat("?"));
        resultData.put(Constants.encTrack2, ";".concat(track2).concat("?"));
        resultData.put(Constants.encTrack3, track3);
        resultData.put(Constants.cardholderName, cardHolderName);
        resultData.put(Constants.serviceCode, serviceCode);
        resultData.put(Constants.pinBlock, hexStrPin != null ? new String(hexStrPin) : "");
        resultData.put(Constants.expiryDate, inicioNombre.substring(inicioNombre.indexOf("^")).substring(1, 5));

        return resultData;
    }


    @Override
    public void openCommunication() {

    }

    @Override
    public void closeCommunication() {
        if (mSMPayKernel != null) {
            mSMPayKernel.destroyPaySDK();
        }
    }

    @Override
    public void resetQPOS() {

    }

    @Override
    public void getSessionKeys(String clavePublicaFile, Context context) {

    }

    @Override
    public void doTransaccion(TransactionAmountData transactionAmountData) {
        //TODO validar operacion EMV, leer banda, leer chip
        limpiarVariables();
        initData(transactionAmountData.getCapacidades());
        this.transactionAmountData = transactionAmountData;
        if (transactionAmountData.getTransactionType().equals(QPOSService.TransactionType.INQUIRY)) {
            checkCard();
        } else {
            startProcessEmv(transactionAmountData.getAmount());
        }
    }

    @Override
    public Map<String, String> getIccTags() {
        return emvTags;
    }

    @Override
    public String getPosInfo() {
        return null;
    }

    @Override
    public void getPin(String maskedPAN) {
        //NONE
    }

    @Override
    public DspreadDevicePOS getDeviePos() {
        return null;
    }

    @Override
    public void deviceCancel() {

    }

    @Override
    public Hashtable<String, String> getQposIdHash() {
        return null;
    }

    @Override
    public int updateFirmware(byte[] dataToUpdate, String file) {
        return 0;
    }

    @Override
    public int getUpdateProgress() {
        return 0;
    }


    public byte[] onPosTagEncrypt(byte[] bytes) {
        try {
            return getTagEncrypt(bytes, 10);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }


    public byte[] onPosPINEncrypt(byte[] bytes) {
        try {
            return getbyteEncrypt(hexStrPin, 11);
        } catch (RemoteException e) {
            return null;
        }
    }


    public byte[] onPosEncryptData(byte[] bytes) {
        try {
            return getbyteEncrypt(createBytePadding(bytes), 10);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public QPOSDeviceInfo getDevicePosInfo() {
        return new QPOSDeviceInfo();
    }

    @Override
    public byte[] onEncryptData(byte[] bytes, EncrypType type) {
        switch (type) {
            case TAGENCRYPT:
                return onPosTagEncrypt(bytes);
            case ICCENCRYPT:
                return onPosEncryptData(bytes);
            case PINENCRYPT:
                return onPosPINEncrypt(bytes);
        }
        return new byte[0];
    }

    private CheckCardCallbackV2 mCheckCardCallback = new CheckCardCallbackV2.Stub() {

        @Override
        public void findMagCard(Bundle bundle) throws RemoteException {
            mCardType = AidlConstantsV2.CardType.MAGNETIC.getValue();
            try {
                Hashtable<String, String> dataCard = getDataOpTarjeta(bundle);
                if (EmvUtil.isChipcard(Objects.requireNonNull(dataCard.get(Constants.serviceCode))) && !fallbackActivado) {//Tarjeta por chip no fallback
                    dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.NO_CHIP.result, "Ingrese la Tarjeta por el Chip o Utilice Otra Tarjeta"));
                } else {
                    if (EmvUtil.requiredNip(Objects.requireNonNull(dataCard.get(Constants.serviceCode))))
                        initPinPad(dataCard);
                    else
                        dongleListener.onResultData(dataCard, DongleListener.DoTradeResult.MCR);
                }
            } catch (Exception e) {
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Error al leer", false));
            }
        }

        @Override
        public void findICCard(String s) throws RemoteException {
            mCardType = AidlConstantsV2.CardType.IC.getValue();
            if (transactionAmountData.getTransactionType().equals(QPOSService.TransactionType.INQUIRY)) {
                Map<String, TLV> mapTAGS = getTlvData();
                TagsTlvToTagsString(mapTAGS);
                dongleListener.onResultData(getDataOpTarjeta(mapTAGS), DongleListener.DoTradeResult.ICC);
            } else {
                transactProcess();
            }
        }

        @Override
        public void findRFCard(String s) throws RemoteException {
            mCardType = AidlConstantsV2.CardType.NFC.getValue();
            if (transactionAmountData.getTransactionType().equals(QPOSService.TransactionType.INQUIRY)) {
                Map<String, TLV> mapTAGS = getTlvData();
                TagsTlvToTagsString(mapTAGS);
                dongleListener.onResultData(getDataOpTarjeta(mapTAGS), DongleListener.DoTradeResult.ICC);
            } else {
                transactProcess();
            }
        }


        /**
         * NO_CHIP_FALLBACK(code)
         */
        @Override
        public void onError(int code, String message) throws RemoteException {
            dongleListener.onRespuestaDongle(new PosResult(code, message));
        }
    };

    private void TagsTlvToTagsString(Map<String, TLV> mapTAGS) {//tagsEMV
        for (Map.Entry<String, TLV> entry : mapTAGS.entrySet()) {
            emvTags.put(entry.getKey(), entry.getValue().getValue());
        }
    }

    private EMVListenerV2 mEMVListener = new EMVListenerV2.Stub() {
        @Override
        public void onWaitAppSelect(List<EMVCandidateV2> list, boolean b) throws RemoteException {
            List<String> candidateNames = getCandidateNames(list);
            final AplicacionEmv aplicacionEmv = new AplicacionEmv() {
                @Override
                public void seleccionAppEmv(final int position) {
                    try {
                        mEMVOptV2.importAppSelect(position);
                    } catch (RemoteException exe) {
                        dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.SELECT_APP_FAIL, "Error al Seleccionar la Aplicación", false));
                    }
                }
            };
            dongleListener.seleccionEmvApp(candidateNames, aplicacionEmv);
        }

        @Override
        public void onAppFinalSelect(String appSelected) throws RemoteException {
            if (appSelected != null && appSelected.length() > 0) {
                boolean isVisa = appSelected.startsWith("A000000003");
                boolean isMaster = appSelected.startsWith("A000000004");
                boolean isUnion = appSelected.startsWith("A000000333");
                if (isVisa) {
                    mAppSelect = 1;
                    // set PayWave tlv data
                    String[] tagsPayWave = {
                            "DF8124", "DF8125", "DF8126"
                    };
                    String[] valuesPayWave = {
                            "999999999999", "999999999999", "000000000000"
                    };
                    mEMVOptV2.setTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_PAYWAVE, tagsPayWave, valuesPayWave);
                } else if (isMaster) {
                    // MasterCard(PayPass)
                    mAppSelect = 2;
                    // set PayPass tlv data
                    String[] tagsPayPass = {
                            "DF8117", "DF8118", "DF8119", "DF811F", "DF811E", "DF812C",
                            "DF8123", "DF8124", "DF8125", "DF8126",
                            "DF811B", "DF811D", "DF8122", "DF8120", "DF8121"
                    };
                    String[] valuesPayPass = {
                            "E0", "F8", "F8", "E8", "00", "00",
                            "999999999999", "999999999999", "999999999999", "000000000000",
                            "30", "02", "0000000000", "000000000000", "000000000000"
                    };
                    mEMVOptV2.setTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_PAYPASS, tagsPayPass, valuesPayPass);
                } else if
                (isUnion) {
                    mAppSelect = 0;
                    // UnionPay

                }
            }
            String[] tags = {
                    "9F33",// Terminal Capabilities
                    "9F40",// Aditional Terminal Capabilities
                    "5F2A", // Moneda
                    "9F1A", // Pais
                    "DF21" // terminal_execute_cvm_limit
            };
            String[] values = {
                    transactionAmountData.getCapacidades().get(TERMINAL_CAPS),
                    transactionAmountData.getCapacidades().get(ADITIONAL_CAPS),
                    transactionAmountData.getCapacidades().get(CURRENCY_CODE),
                    transactionAmountData.getCapacidades().get(COUNTRY_CODE),
                    transactionAmountData.getCapacidades().get(CVM_LIMIT)
            };
            mEMVOptV2.setTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_NORMAL, tags, values);
            mEMVOptV2.importAppFinalSelectStatus(0);
        }

        @Override
        public void onConfirmCardNo(String cardNo) throws RemoteException {
            mCardNo = cardNo;
            mEMVOptV2.importCardNoStatus(0);
        }

        @Override
        public void onRequestShowPinPad(int pinType, int remainTime) throws RemoteException {
            mPinType = pinType;
            initPinPad(null);
        }

        @Override
        public void onRequestSignature() throws RemoteException {
            mEMVOptV2.importSignatureStatus(0);

        }

        @Override
        public void onCertVerify(int i, String s) throws RemoteException {

        }

        @Override
        public void onOnlineProc() throws RemoteException {
            Map<String, TLV> mapTAGS = getTlvData();
            TagsTlvToTagsString(mapTAGS);
            dongleListener.onResultData(getDataOpTarjeta(mapTAGS), DongleListener.DoTradeResult.ICC);
        }

        @Override
        public void onCardDataExchangeComplete() throws RemoteException {
            // NONE
        }

        @Override
        public void onTransResult(int code, String desc) throws RemoteException {//when has finalized process online
            //AppLogger.LOGGER.fine(TAG, "onTransResult code:" + code + " desc:" + desc);
            //AppLogger.LOGGER.fine(TAG, "***************************************************************");
            //AppLogger.LOGGER.fine(TAG, "****************************End Process************************");
            //AppLogger.LOGGER.fine(TAG, "***************************************************************");
            dongleListener.onRespuestaDongle(new PosResult(code, desc));
        }

        @Override
        public void onConfirmationCodeVerified() throws RemoteException {//Only confirmation phone required
            byte[] outData = new byte[512];
            int len = mEMVOptV2.getTlv(AidlConstantsV2.EMV.TLVOpCode.OP_PAYPASS, "DF8129", outData);
            if (len > 0) {
                byte[] data = new byte[len];
                System.arraycopy(outData, 0, data, 0, len);
                String hexStr = ByteUtil.bytes2HexStr(data);
            }
            // card off
            mReadCardOptV2.cardOff(mCardType);
        }
    };

    @Override
    public void cancelOperacion() {
        try {
            mReadCardOptV2.cancelCheckCard();
            dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Operación Cancelada", false));
        } catch (Exception exe) {
            dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Operación Cancelada", false));
        }
    }

    private String getHexEmvtags(final Map<String, TLV> mapTags) {
        final StringBuilder Sbuilder = new StringBuilder();
        for (final String tag : mapTags.keySet()) {
            Sbuilder.append(mapTags.get(tag).recoverToHexStr());
        }
        return Sbuilder.toString();

    }

    private void limpiarVariables() {
        mCardType = 0;  // card type
        mCardNo = "";
        hexStrPin = null;
        amount = "";
        mAppSelect = 0;
        mPinType = 2;   // 0-online pin, 1-offline pin
        track1 = "";
        track2 = "";
    }

    byte[] createBytePadding(final byte[] input) {
        int len = input.length;

        if (input.length % 8 > 0) {
            len += 8 - (input.length % 8);

            final byte[] ret = new byte[len];
            Arrays.fill(ret, (byte) 0x00);

            System.arraycopy(input, 0, ret, 0, input.length);

            return ret;

        } else {
            return input;
        }
    }

    private byte[] getbyteEncrypt(final byte[] data, final int keyIndex) throws RemoteException {

        byte[] dataIn = data;
        byte[] dataOut = new byte[dataIn.length];
        int result = mSecurityOptV2.dataEncrypt(keyIndex, dataIn, AidlConstantsV2.Security.DATA_MODE_ECB, null, dataOut);
        if (result == 0) {
            return dataOut;
        }

        return new byte[0];
    }

    private List<String> getCandidateNames(List<EMVCandidateV2> candiList) {
        List<String> appsName = new ArrayList<>();
        if (candiList == null || candiList.size() == 0)
            return Collections.EMPTY_LIST;

        for (int i = 0; i < candiList.size(); i++) {
            EMVCandidateV2 candi = candiList.get(i);
            String name = candi.appPreName;
            name = TextUtils.isEmpty(name) ? candi.appLabel : name;
            name = TextUtils.isEmpty(name) ? candi.appName : name;
            name = TextUtils.isEmpty(name) ? "" : name;
            appsName.add(name);
        }
        return appsName;
    }

    private Map<String, TLV> getTlvData() {
        try {

            List<String> emvTags = transactionAmountData.getSunmiCapacidades();
            String[] tagList;
            if (!emvTags.isEmpty()) {
                tagList = new String[emvTags.size()];
                tagList = emvTags.toArray(tagList);
            } else {
                tagList = TAGSEMV;
            }

            byte[] outData = new byte[4096];
            Map<String, TLV> map = new LinkedHashMap<>();
            int tlvOpCode;
            if (AidlConstantsV2.CardType.NFC.getValue() == mCardType) {
                if (mAppSelect == 2) {
                    tlvOpCode = AidlConstantsV2.EMV.TLVOpCode.OP_PAYPASS;
                } else if (mAppSelect == 1) {
                    tlvOpCode = AidlConstantsV2.EMV.TLVOpCode.OP_PAYWAVE;
                } else {
                    tlvOpCode = AidlConstantsV2.EMV.TLVOpCode.OP_NORMAL;
                }
            } else {
                tlvOpCode = AidlConstantsV2.EMV.TLVOpCode.OP_NORMAL;
            }
            int len = mEMVOptV2.getTlvList(tlvOpCode, tagList, outData);
            if (len > 0) {
                byte[] bytes = Arrays.copyOf(outData, len);
                String hexStr = ByteUtil.bytes2HexStr(bytes);
                Map<String, TLV> tlvMap = TLVUtil.buildTLVMap(hexStr);
                map.putAll(tlvMap);
            }
            return map;
        } catch (Exception exe) {
            cancelOperacion();
            return Collections.emptyMap();
        }
    }

    @Override
    public void operacionFinalizada(String arpc, int sttus) {
        importOnlineProcessStatus(arpc, sttus);
    }

    private void initPinPad(final Hashtable<String, String> dataCard) {
        try {
            PinPadConfigV2 pinPadConfig = new PinPadConfigV2();
            pinPadConfig.setPinPadType(0);
            pinPadConfig.setPinType(mPinType);
            pinPadConfig.setOrderNumKey(true);
            byte[] panBytes = mCardNo.substring(mCardNo.length() - 13, mCardNo.length() - 1).getBytes("US-ASCII");
            pinPadConfig.setPan(panBytes);
            pinPadConfig.setTimeout(60 * 1000); // input password timeout
            pinPadConfig.setPinKeyIndex(11);    // pik index
            pinPadConfig.setMaxInput(6);
            pinPadConfig.setMinInput(4);
            pinPadConfig.setKeySystem(0);

            pinPadConfig.setAlgorithmType(0);
            mPinPadOptV2.initPinPad(pinPadConfig, new PinPadListenerV2.Stub() {

                @Override
                public void onPinLength(int len) {
                    //Length pin
                }

                @Override
                public void onConfirm(int i, byte[] pinBlock) {

                    try {
                        if (pinBlock != null) {
                            hexStrPin = pinBlock;
                            if (mCardType == AidlConstantsV2.CardType.MAGNETIC.getValue() && dataCard != null) {
                                dataCard.put(Constants.pinBlock, new String(hexStrPin));
                                dongleListener.onResultData(dataCard, DongleListener.DoTradeResult.MCR);
                            } else
                                mEMVOptV2.importPinInputStatus(mPinType, 0);
                        } else {
                            mEMVOptV2.importPinInputStatus(mPinType, 2);
                        }
                    } catch (RemoteException exe) {
                        cancelOperacion();
                    }

                }

                @Override
                public void onCancel() {
                    try {
                        mEMVOptV2.importPinInputStatus(mPinType, 1);
                    } catch (RemoteException exe) {
                        cancelOperacion();
                    }
                }

                @Override
                public void onError(int code) {
                    try {
                        mEMVOptV2.importPinInputStatus(mPinType, 3);
                    } catch (RemoteException exe) {
                        cancelOperacion();
                    }
                }

            });
        } catch (Exception exe) {
            cancelOperacion();
        }
    }

    private void importOnlineProcessStatus(String arpc, final int status) {
        try {
            String[] tags = {
                    "71", "72", "91", "8A", "89"
            };
            String[] values = {
                    "", "", "", arpc, ""
            };
            byte[] out = new byte[1024];
            int len = mEMVOptV2.importOnlineProcStatus(status, tags, values, out);
            if (len < 0) {
                if (len == PosResult.PosTransactionResult.SYNCOPERATION.result)
                    dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.SYNCOPERATION.result, "Tarjeta retirada antes de tiempo."));
            } else {
                byte[] bytes = Arrays.copyOf(out, len);
                String hexStr = ByteUtil.bytes2HexStr(bytes);
            }
        } catch (Exception exe) {
            cancelOperacion();
        }
    }

}
