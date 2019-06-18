package com.pagatodo.qposlib;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.widget.Toast;

import com.pagatodo.qposlib.abstracts.AbstractDongle;
import com.pagatodo.qposlib.dongleconnect.AplicacionEmv;
import com.pagatodo.qposlib.dongleconnect.DongleConnect;
import com.pagatodo.qposlib.dongleconnect.DongleListener;
import com.pagatodo.qposlib.dongleconnect.TransactionAmountData;
import com.pagatodo.qposlib.pos.PosResult;
import com.pagatodo.qposlib.pos.QPOSDeviceInfo;
import com.pagatodo.qposlib.pos.dspread.DspreadDevicePOS;
import com.pagatodo.qposlib.pos.sunmi.ByteUtil;
import com.pagatodo.qposlib.pos.sunmi.EmvUtil;
import com.pagatodo.qposlib.pos.sunmi.TLV;
import com.pagatodo.qposlib.pos.sunmi.TLVUtil;
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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sunmi.paylib.SunmiPayKernel;

import static com.pagatodo.qposlib.QPosManager.ADITIONAL_CAPS;
import static com.pagatodo.qposlib.QPosManager.COUNTRY_CODE;
import static com.pagatodo.qposlib.QPosManager.CURRENCY_CODE;
import static com.pagatodo.qposlib.QPosManager.CVM_LIMIT;
import static com.pagatodo.qposlib.QPosManager.TERMINAL_CAPS;

public class SunmiPosManager extends AbstractDongle {

    private static final String TAG = SunmiPosManager.class.getSimpleName();
    private SunmiPayKernel mSMPayKernel;

    public static BasicOptV2 mBasicOptV2;
    public static ReadCardOptV2 mReadCardOptV2;
    public static PinPadOptV2 mPinPadOptV2;
    public static SecurityOptV2 mSecurityOptV2;
    public static EMVOptV2 mEMVOptV2;

    private Hashtable<String, String> sessionKeys;
    private Map<String, String> configMap;
    private int mCardType;  // card type
    private String mCardNo = "";
    private byte [] hexStrPin ;
    private String amount;
    private int mAppSelect ;
    private int mPinType;   // 0-online pin, 1-offline pin

    private String track1 = "";
    private String track2 = "";

    private TransactionAmountData transactionAmountData;

    private SunmiPosManager() {
    }

    public SunmiPosManager(DongleConnect listener, Hashtable<String, String> hashtable) {
        super(listener);
        this.setPosSunmi(this);
        this.sessionKeys = hashtable;
        connectPayService();
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
//                AppLogger.LOGGER.fine(TAG, "onConnectPaySDK");
                try {
                    mEMVOptV2 = mSMPayKernel.mEMVOptV2;
                    mBasicOptV2 = mSMPayKernel.mBasicOptV2;
                    mPinPadOptV2 = mSMPayKernel.mPinPadOptV2;
                    mReadCardOptV2 = mSMPayKernel.mReadCardOptV2;
                    mSecurityOptV2 = mSMPayKernel.mSecurityOptV2;
                    mAppSelect = 0;
                    getSessionKeys("PCIEM.PEM", null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnectPaySDK() {
//                AppLogger.LOGGER.throwing(TAG, 1, new Throwable(), "onDisconnectPaySDK");
                dongleConnect.ondevicedisconnected();
//                dongleListener.ondevicedisconnected();
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
            int mCardType = AidlConstantsV2.CardType.MAGNETIC.getValue() | AidlConstantsV2.CardType.IC.getValue();
            mReadCardOptV2.checkCard(mCardType, mCheckCardCallback, 60);
        } catch (Exception exe) {
//            AppLogger.LOGGER.throwing(TAG,1,exe,exe.getMessage());
            cancelprocess();

        }
    }

    private void cancelprocess() {
        try {
            mReadCardOptV2.cancelCheckCard();
        } catch (Exception exe) {
//            AppLogger.LOGGER.throwing(TAG,1,exe,exe.getMessage());
        }
    }

    public void transactProcess() {
//        AppLogger.LOGGER.fine(TAG, "transactProcess");
        try {
            EMVTransDataV2 emvTransData = new EMVTransDataV2();
            emvTransData.amount = amount;
            emvTransData.flowType = 1;
            emvTransData.cardType = mCardType;
            mEMVOptV2.transactProcess(emvTransData, mEMVListener);
        } catch (Exception exe) {
//            AppLogger.LOGGER.throwing(TAG,1,exe,exe.getMessage());
            cancelprocess();
        }
    }

    private Hashtable<String, String> getDataOpTarjeta ( final Map<String, TLV> mapTAGS ) { //NOSONAR
        Hashtable<String, String> resultData = new Hashtable<>();
        try {
            resultData.put("encPAN", mapTAGS.containsKey("5A") ? mapTAGS.get("5A").getValue() : "");
            resultData.put("encTrack1", mapTAGS.containsKey("9F1F") ? mapTAGS.get("9F1F").getValue() : "");
            resultData.put("encTrack2", mapTAGS.containsKey("57") ? mapTAGS.get("57").getValue() : "");
            resultData.put("encTrack3", mapTAGS.containsKey("58") ?mapTAGS.get("58").getValue() : "");
            resultData.put("iccdata", getHexEmvtags(mapTAGS));
            resultData.put("pinBlock", hexStrPin !=null ? new String(getbyteEncrypt(hexStrPin, 11)) : "");
            if (mapTAGS.containsKey("57")) {
                final int index =  mapTAGS.get("57").getValue().indexOf("D") + 1;
                final int endIndex = index + 4;
                resultData.put("expiryDate",  mapTAGS.get("57").getValue().substring(index, endIndex));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return resultData;
    }

    @Override
    public void openCommunication() {

    }

    //--------------------------Pos Interface------------------------------------------------------------------
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
        PosInstance.getInstance().setSessionKeys(sessionKeys);
        dongleConnect.onSessionKeysObtenidas();
    }

    @Override
    public void doTransaccion(TransactionAmountData transactionAmountData) {
        //TODO validar operacion EMV, leer banda, leer chip
        initData(transactionAmountData.getCapacidades());
        this.transactionAmountData = transactionAmountData;
        startProcessEmv(transactionAmountData.getAmount());
    }

    @Override
    public Map<String, String> getIccTags() {
        return null;
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
    public int updateFirmware(byte[] dataToUpdate, String file) {
        return 0;
    }

    @Override
    public int getUpdateProgress() {
        return 0;
    }

    @Override
    public QPOSDeviceInfo getDevicePosInfo() {
        return new QPOSDeviceInfo();
    }

    //--------------------------------------Callback Sunmi-------------------------------------------------

    private CheckCardCallbackV2 mCheckCardCallback = new CheckCardCallbackV2.Stub() {

        @Override
        public void findMagCard(Bundle bundle) throws RemoteException {
            mCardType = AidlConstantsV2.CardType.MAGNETIC.getValue();
            track1 = bundle.getString("TRACK1");
            track2 = bundle.getString("TRACK2");
            String track3 = bundle.getString("TRACK3");
            String value = "track1:" + track1 + "\ntrack2:" + track2 + "\ntrack3:" + track3;
            String serviceCode = "";
            if (track2 != null) {
                int index = track2.indexOf("=");
                if (index != -1) {
                    mCardNo = track2.substring(0, index);
                    serviceCode = track2.substring(index+5, index+8);
                }
            }
//            if (ValidatePerfilEMV.validateNIPifnecessary(serviceCode)){
//                initPinPad();
//            }else {
//
//            }
//            AppLogger.LOGGER.fine(TAG, "Lectura Banda "+value);
//            dongleListener.onSessionKeysObtenidas();
            dongleConnect.onSessionKeysObtenidas();
        }

        @Override
        public void findICCard(String s) throws RemoteException {
            //Todo respuesta chip
            mCardType = AidlConstantsV2.CardType.IC.getValue();
//            AppLogger.LOGGER.fine(TAG, "Lectura Chip");
            transactProcess();
        }

        @Override
        public void findRFCard(String s) throws RemoteException {

        }

        @Override
        public void onError(int code, String message) throws RemoteException {

        }
    };

    private EMVListenerV2 mEMVListener = new EMVListenerV2.Stub()  {
        @Override
        public void onWaitAppSelect(List<EMVCandidateV2> list, boolean b) throws RemoteException {
            List<String> candidateNames = getCandidateNames(list);
            final AplicacionEmv aplicacionEmv = new AplicacionEmv() {
                @Override
                public void seleccionAppEmv(final int position) {
                    try {
                        mEMVOptV2.importAppSelect(position);
                    }
                    catch ( RemoteException exe ){
//                        AppLogger.LOGGER.throwing(TAG,1,exe,exe.getMessage());
                        dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.SELECT_APP_FAIL, "Error al Seleccionar la Aplicación", false));


                    }
                }
            };
            dongleListener.seleccionEmvApp(candidateNames, aplicacionEmv);
        }

        @Override
        public void onAppFinalSelect(String appSelected) throws RemoteException {
//            AppLogger.LOGGER.fine(TAG,"onAppFinalSelect tag9F06value:" + appSelected);
            if (appSelected != null && appSelected.length() > 0) {
                boolean isVisa = appSelected.startsWith("A000000003");
                boolean isMaster = appSelected.startsWith("A000000004");
                boolean isUnion = appSelected.startsWith("A000000333");
                if ( isVisa ) {
                    // VISA(PayWave)
                    mAppSelect = 1;
                    // set PayWave tlv data
                    String[] tagsPayWave = {
                            "DF8124", "DF8125", "DF8126"
                    };
                    String[] valuesPayWave = {
                            "999999999999", "999999999999", "000000000000"
                    };
                    mEMVOptV2.setTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_PAYWAVE, tagsPayWave, valuesPayWave);
                } else if ( isMaster ) {
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
                ( isUnion ) {
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
//            AppLogger.LOGGER.info(TAG,cardNo);
            mCardNo = cardNo;
            mEMVOptV2.importCardNoStatus(0);
        }

        @Override
        public void onRequestShowPinPad(int pinType, int remainTime) throws RemoteException {
//            AppLogger.LOGGER.info(TAG,"Type PIN : " + pinType );
            mPinType = pinType;
            initPinPad();
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
          Map<String ,TLV> mapTAGS =   getTlvData();
          dongleListener.onResultData(getDataOpTarjeta(mapTAGS), DongleListener.DoTradeResult.ICC);
        }


        @Override
        public void onCardDataExchangeComplete() throws RemoteException {
            // NONE
        }

        @Override
        public void onTransResult(int code, String desc) throws RemoteException {
//            AppLogger.LOGGER.fine(TAG, "onTransResult code:" + code + " desc:" + desc);
//            AppLogger.LOGGER.fine(TAG, "***************************************************************");
//            AppLogger.LOGGER.fine(TAG, "****************************End Process************************");
//            AppLogger.LOGGER.fine(TAG, "***************************************************************");
            dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.APROBADO, "Operación Finalizada", true));
        }

        @Override
        public void onConfirmationCodeVerified() throws RemoteException {
//            AppLogger.LOGGER.fine(TAG, "onConfirmationCodeVerified");
            byte[] outData = new byte[512];
            int len = mEMVOptV2.getTlv(AidlConstantsV2.EMV.TLVOpCode.OP_PAYPASS, "DF8129", outData);
            if (len > 0) {
                byte[] data = new byte[len];
                System.arraycopy(outData, 0, data, 0, len);
                String hexStr = ByteUtil.bytes2HexStr(data);
//                AppLogger.LOGGER.fine(TAG, "DF8129: " + hexStr);
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
//            AppLogger.LOGGER.throwing(TAG,1,exe,exe.getMessage());
        }
    }

    private String getHexEmvtags (final Map<String,TLV> mapTags ){
        final StringBuilder Sbuilder =  new StringBuilder();
        for (final String tag : mapTags.keySet() ){
            Sbuilder.append(mapTags.get(tag).recoverToHexStr());
        }
        return Sbuilder.toString();

    }

    private byte [] getbyteEncrypt( final byte [] data ,final int keyIndex) throws RemoteException {

        byte[] dataIn = data;
        byte[] dataOut = new byte[dataIn.length];
        int result =  mSecurityOptV2.dataEncrypt(keyIndex, dataIn, AidlConstantsV2.Security.DATA_MODE_ECB, null, dataOut);
        if (result == 0) {
            return dataOut;
        }

        return new byte[0];
    }

    private List <String>   getCandidateNames(List<EMVCandidateV2> candiList) {
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
//            AppLogger.LOGGER.fine(TAG, name);
        }
        return  appsName;
    }

    private Map<String,TLV> getTlvData() {
        try {
            List<String> emvTags = new ArrayList(transactionAmountData.getCapacidades().values());

            String[] tagList = new String[emvTags.size()];
            tagList = emvTags.toArray(tagList);

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
//            AppLogger.LOGGER.throwing(TAG,1,exe,exe.getMessage());
            cancelOperacion();
            return Collections.emptyMap();
        }
    }

    @Override
    public void operacionFinalizada(String ARPC) {
        importOnlineProcessStatus(0 ,ARPC);

    }

    private void initPinPad() {
//        AppLogger.LOGGER.fine(TAG, "initPinPad");
        try {
            PinPadConfigV2 pinPadConfig = new PinPadConfigV2();
            pinPadConfig.setPinPadType(0);
            pinPadConfig.setPinType(mPinType);
            pinPadConfig.setOrderNumKey(true);
//            byte[] panBytes = mCardNo.getBytes(StandardCharsets.US_ASCII);
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
//                    AppLogger.LOGGER.info(TAG, "onPinLength: " + len);
                }

                @Override
                public void onConfirm(int i, byte[] pinBlock) {

                    try {
                        if ( pinBlock != null ) {
                            hexStrPin = pinBlock;
//                            AppLogger.LOGGER.info(TAG, "onConfirm pin block:" + hexStrPin);
                            mEMVOptV2.importPinInputStatus(mPinType, 0);
                        } else {
                            mEMVOptV2.importPinInputStatus(mPinType, 2);
                        }
                    }
                    catch ( RemoteException exe ){
//                        AppLogger.LOGGER.throwing(TAG,1,exe ,exe.getMessage());
                        cancelOperacion();
                    }

                }

                @Override
                public void onCancel() {
                    try {
//                        AppLogger.LOGGER.throwing(TAG, 1, new Throwable("onCancel"), "onPin Canceled");
                        mEMVOptV2.importPinInputStatus(mPinType, 1);
                        dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Error al Ingresar el PIN", false));
                    }
                    catch ( RemoteException exe ){
//                        AppLogger.LOGGER.throwing(TAG,1,exe ,exe.getMessage());
                        cancelOperacion();
                    }
                }

                @Override
                public void onError(int code) {
                    try {
                        String msg = AidlErrorCodeV2.valueOf(code).getMsg();
//                        AppLogger.LOGGER.throwing(TAG, 1, new Throwable("Erro al Ingresar el PIN "), msg);

                        mEMVOptV2.importPinInputStatus(mPinType, 3);
                    }
                    catch ( RemoteException exe ){
//                        AppLogger.LOGGER.throwing(TAG,1,exe ,exe.getMessage());
                        cancelOperacion();
                    }
                }

            });
        } catch (Exception exe) {
//            AppLogger.LOGGER.throwing(TAG,1,exe,exe.getMessage());
            cancelOperacion();
        }
    }

    private void importOnlineProcessStatus(final int status ,final String ARPC) {
//        AppLogger.LOGGER.fine(TAG, "importOnlineProcessStatus status:" + status    + "ARPC: " + ARPC );
        try {
            String[] tags = {
                    "71", "72", "91", "8A", "89"
            };
            String[] values = {
                    "", "", "", "", ""
            };
            byte[] out = new byte[1024];
            int len = mEMVOptV2.importOnlineProcStatus(status, tags, values, out);
            if (len < 0) {
//                AppLogger.LOGGER.throwing(TAG ,1, new Throwable("importOnlineProcessStatus error,code:" ) , "" + len);
            } else {
                byte[] bytes = Arrays.copyOf(out, len);
                String hexStr = ByteUtil.bytes2HexStr(bytes);
//                AppLogger.LOGGER.fine(TAG, "importOnlineProcessStatus outData:" + hexStr);
            }
        } catch (Exception exe) {
//            AppLogger.LOGGER.throwing(TAG,1,exe,exe.getMessage());
            cancelOperacion();
        }
    }

}
