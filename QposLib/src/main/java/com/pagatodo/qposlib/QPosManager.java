package com.pagatodo.qposlib;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;

import com.bbpos.bbdevice.BBDeviceController;
import com.dspread.xpos.EmvAppTag;
import com.dspread.xpos.QPOSService;
import com.pagatodo.qposlib.abstracts.AbstractDongle;
import com.pagatodo.qposlib.dongleconnect.AplicacionEmv;
import com.pagatodo.qposlib.dongleconnect.DongleConnect;
import com.pagatodo.qposlib.dongleconnect.DongleListener;
import com.pagatodo.qposlib.dongleconnect.TransactionAmountData;
import com.pagatodo.qposlib.pos.ICCDecodeData;
import com.pagatodo.qposlib.pos.POSConnectionState;
import com.pagatodo.qposlib.pos.PosResult;
import com.pagatodo.qposlib.pos.QPOSDeviceInfo;
import com.pagatodo.qposlib.pos.QposParameters;
import com.pagatodo.qposlib.pos.dspread.DspreadDevicePOS;
import com.pagatodo.qposlib.pos.dspread.POSBluetoothDevice;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import Decoder.BASE64Decoder;

public class QPosManager<T extends DspreadDevicePOS> extends AbstractDongle implements QPOSService.QPOSServiceListener {

    /*Constantes
     */
    private static final String POS_ID = "posId";
    private final String TAG = QPosManager.class.getSimpleName();
    private final POSConnectionState mQStatePOS = new POSConnectionState();
    private final DspreadDevicePOS<Parcelable> mDevicePos;
    public static final String TERMINAL_CAPS = "TERMINAL_CAPS";
    public static final String ADITIONAL_CAPS = "ADITIONAL_CAPS";
    public static final String CURRENCY_CODE = "CURRENCY_CODE";
    public static final String COUNTRY_CODE = "COUNTRY_CODE";
    public static final String CVM_LIMIT = "CVM_LIMIT";
    public static final String REQUIERE_PIN = "INGRESE PIN";
    private static final String[] TAGSEMV = {"4F", "5F20", "9F12", "5A", "9F27", "9F26", "95", "9B", "5F28", "9F07"};

    /*Variables
     */

    private QPOSService mPosService;
    private DspreadDevicePOS mDevice;
    private TransactionAmountData transactionAmountData;
    private QPOSDeviceInfo mQPosDeviceInfo;
    private QposServiceListener mQposServiceCallback;
    private QPOSService.DoTradeResult mCurrentTradeResult;
    private Hashtable<String, String> mDecodeData;
    private ArrayList<String> mListCapabilities;
    private Map<String, String> mEmvTags = new ArrayMap<>();
    private Hashtable<String, String> mQposIdHash;
    private QposParameters qposParameters;
    private boolean isLogEnabled;

    public interface QposServiceListener {
        void onQposIdResult(Hashtable<String, String> hashtable);

        void onDoTradeResult(Hashtable<String, String> hashtable);

        void onValidatorProfile();

        void setPosAmount();

        Context getContext();

        TransactionAmountData getTransactionAmountData();

        void onReturnCustomConfigResult();

        void onRequestOnlineProcess();
    }

    public void setQposDongleListener(DongleListener listener) {
        dongleListener = listener;
    }

    public QPosManager(final T dongleDevice, final DongleConnect listener, final boolean enableLog) {
        super(listener);
        isLogEnabled = enableLog;
        mQStatePOS.updateState(POSConnectionState.STATE_POS.NONE);
        this.mDevicePos = dongleDevice;
        this.setQposDspread(this);
    }

    public QPosManager(final T dongleDevice, final DongleConnect listener) {
        super(listener);
        mQStatePOS.updateState(POSConnectionState.STATE_POS.NONE);
        this.mDevicePos = dongleDevice;
        this.setQposDspread(this);
    }

    @Override
    public Hashtable<String, String> getQposIdHash() {
        logFlow("getQposIdHash() called");
        return mQposIdHash;
    }

    public void setQposServiceListener(QposServiceListener qposServiceCallback) {
        mQposServiceCallback = qposServiceCallback;
    }

    @Override
    public void openCommunication() {
        logFlow("openCommunication() called");

        if (!isQPOSConnected()) {
            this.mDevice = mDevicePos;

            mPosService = QPOSService.getInstance(mDevice.getCommunicationMode());
        }

        if (mPosService == null) {
            return;
        }

        mPosService.setConext(PosInstance.getInstance().getAppContext());

        mPosService.initListener(this);

        mPosService.setShutDownTime(1800);

        switch (mDevice.getCommunicationMode()) {
            case USB_OTG_CDC_ACM:
                final UsbDevice mUsbDevice = (UsbDevice) mDevicePos.getmDevicePos();
                mPosService.openUsb(mUsbDevice);
                break;
            case BLUETOOTH:
            case BLUETOOTH_BLE:
                mPosService.connectBluetoothDevice(true, 25, ((POSBluetoothDevice) mDevice).getAddress());
                break;
            default:
                break;
        }

        mPosService.setPosExistFlag(true);
    }

    public void closeCommunication() {
        logFlow("closeCommunication() called");

        if (mPosService != null) {
            mQStatePOS.updateState(POSConnectionState.STATE_POS.CLOSE);

            switch (mDevice.getCommunicationMode()) {
                case BLUETOOTH:
                case BLUETOOTH_BLE:
                    mPosService.disconnectBT();
                    break;
                case USB:
                case USB_OTG_CDC_ACM:
                    mPosService.closeUsb();
                    break;
                default:
                    mPosService.close();
                    break;
            }
            mPosService.onDestroy();
        }
    }

    @Override
    public void resetQPOS() {
        logFlow("resetQPOS() called");
        mPosService.resetQPOS();
    }

    @Override
    public String getPosInfo() {
        logFlow("getPosInfo() returned: " + null);
        return null;
    }

    @Override
    public void getPin(String maskedPAN) {
        logFlow("getPin() called with: maskedPAN = [" + maskedPAN + "]");
        mPosService.getPin(1, 10, 8, REQUIERE_PIN, maskedPAN, getDateforTRX(), 15);
    }

    @Override
    public void getSessionKeys(final String clavePublicaFile, final Context context) {
        logFlow("getSessionKeys() called with: clavePublicaFile = [" + clavePublicaFile + "], context = [" + context + "]");
        try {
            mPosService.updateRSA(getPublicKey(clavePublicaFile, context), clavePublicaFile);
        } catch (Exception exe) {

            dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Llave No Generada", false));
        }
    }

    @Override
    public void doTransaccion(TransactionAmountData transactionAmountData) {
        logFlow("doTransaccion() called with: transactionAmountData = [" + transactionAmountData + "]");

        if (mPosService.isQposPresent()) {

            mListCapabilities = new ArrayList<>();

            setListCapabillities(transactionAmountData.getCapacidades());

            if (transactionAmountData.getTipoOperacion().equals("D")) {
                mPosService.setQuickEmv(true);
            } else {
                if (transactionAmountData.getAmountIcon().equals("")) {
                    mPosService.setAmountIcon(transactionAmountData.getAmountIcon());
                } else {
                    mPosService.setQuickEmv(true);
                }
            }

            mPosService.setFormatId("0025");

            this.transactionAmountData = transactionAmountData;

            final String lisCap = "Capacidades : ".concat(Arrays.toString(mListCapabilities.toArray()));
            logFlow("doTransaccion: listCap = " + lisCap);

            mPosService.updateEmvAPP(QPOSService.EMVDataOperation.update, mListCapabilities);
        } else {
            onRequestQposDisconnected();
        }
    }

    @Override
    public void doTransaccion(TransactionAmountData transactionAmountData, QposParameters qposParameters) {
        logFlow("doTransaccion() called with: transactionAmountData = [" + transactionAmountData + "], qposParameters = [" + qposParameters + "]");

        if (!mPosService.isQposPresent()) {
            onRequestQposDisconnected();
        } else {
            mListCapabilities = new ArrayList<>();
            this.qposParameters = qposParameters;

            setListCapabillities(transactionAmountData.getCapacidades());

            if (transactionAmountData.getTipoOperacion().equals("D")) {
                mPosService.setQuickEmv(true);
            } else {
                if (transactionAmountData.getAmountIcon().equals("")) {
                    mPosService.setAmountIcon(transactionAmountData.getAmountIcon());
                } else {
                    mPosService.setQuickEmv(true);
                }
            }

            mPosService.setFormatId("0025");
            this.transactionAmountData = transactionAmountData;

            final String lisCap = "Capacidades : ".concat(Arrays.toString(mListCapabilities.toArray()));
            logFlow("doTransaccion: listCap = " + lisCap);

            mPosService.updateEmvAPP(QPOSService.EMVDataOperation.update, mListCapabilities);
        }
    }

    public PublicKey getPublicKey(final String filename, final Context contextApp) throws Exception {//NOSONAR
        logFlow("getPublicKey() called with: filename = [" + filename + "], contextApp = [" + contextApp + "]");

        final InputStream inputStream = contextApp.getAssets().open(filename);
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        final StringBuilder sb = new StringBuilder();

        while ((line = br.readLine()) != null) {
            if (line.contains("BEGIN")) {
                sb.delete(0, sb.length());
            } else {
                if (line.contains("END")) {
                    break;
                }

                sb.append(line);
                sb.append('\r');
            }
        }
        final BASE64Decoder base64Decoder = new BASE64Decoder();
        final byte[] buffer = base64Decoder.decodeBuffer(sb.toString());
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
        final PublicKey publicKey = keyFactory.generatePublic(keySpec);

        logFlow("getPublicKey() returned: " + publicKey);
        return publicKey;
    }

    public boolean isQPOSConnected() {
        return mQStatePOS.isConnected();
    }

    public void setOnRespuestaDongle(PosResult.PosTransactionResult operation, String message, boolean isCorrecta) {
        dongleListener.onRespuestaDongle(new PosResult(operation, message, isCorrecta));
    }

    public void setQuickEmv(boolean set) {
        mPosService.setQuickEmv(set);
    }

    public void setPosServiceAmountIcon(String icon) {
        mPosService.setAmountIcon(icon);
    }

    public void setPosFormatId(String formatId) {
        mPosService.setFormatId(formatId);
    }

    public boolean isTransaccionEqualToD(TransactionAmountData transactionAmountData) {
        return transactionAmountData.getTipoOperacion().equals("D");
    }

//    public void setEmvConfig(byte[] emvCapkCfgBytes, byte[] emvCapAppBytes){
//        final String emvCapkCfg = HexUtil.hexStringFromBytes(emvCapkCfgBytes);
//        final String emvCapApp = HexUtil.hexStringFromBytes(emvCapAppBytes);
//        mPosService.updateEmvConfig(emvCapApp,emvCapkCfg);
//    }

    public void updateEmvApp() {
        if (mListCapabilities != null) {
            mPosService.updateEmvAPP(QPOSService.EMVDataOperation.update, mListCapabilities);
        }
    }

    public int updateFirmware(byte[] dataToUpdate, String file) {
        return mPosService.updatePosFirmware(dataToUpdate, file);
    }

    public int getUpdateProgress() {
        int i = mPosService.getUpdateProgress();
        logFlow("getUpdateProgress() returned: " + i);
        return i;
    }

    public void cancelOperacion() {
        logFlow("cancelOperacion() called");
        mPosService.resetQPOS();
    }

    public void operacionFinalizada(String arpc) {
        logFlow("operacionFinalizada() called with: arpc = [" + arpc + "]");
        mPosService.sendOnlineProcessResult(arpc);
    }

    public Map<String, String> getIccTags() {
        logFlow("getIccTags() returned: " + mEmvTags);
        return mEmvTags;
    }

    @Override
    public DspreadDevicePOS getDeviePos() {
        logFlow("getDeviePos() returned: " + null);
        return null;
    }

    public DspreadDevicePOS getDevicePos() {
        return mDevice;
    }

    public QPOSDeviceInfo getDevicePosInfo() {
        logFlow("getDevicePosInfo() returned: " + mQPosDeviceInfo);
        return mQPosDeviceInfo;
    }

    public void generateSessionKeys() {
        mPosService.generateSessionKeys();
    }

    public void deviceOnTransaction() {
        dongleConnect.deviceOnTransaction();
    }

    public boolean isTradeResultDifferentFromMcrAndIcc() {
        return mCurrentTradeResult != QPOSService.DoTradeResult.MCR && mCurrentTradeResult != QPOSService.DoTradeResult.ICC;
    }

    public boolean isTradeResultMcr() {
        if (mCurrentTradeResult != null) {
            return mCurrentTradeResult == QPOSService.DoTradeResult.MCR;
        }

        return false;
    }

    public void doEmvAppOnPosService() {
        mPosService.doEmvApp(QPOSService.EmvOption.START);
    }

    public void onFailTradeResult(QPOSService.DoTradeResult tradeResult) {
        switch (tradeResult) {
            case NONE:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.UNKNOWN, tradeResult.name(), false));
                break;
            case BAD_SWIPE:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.INVALID_ICC_DATA, "Error en la Lectura de Chip, Operación no Permitida sin Chip", false));
                break;
            case ICC:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.ERROR_DISPOSITIVO, "Ingrese la Tarjeta por el Chip o Utilice Otra Tarjeta", false));
                break;
            case NOT_ICC:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.NO_CHIP, "Error al Leer el Chip", false));
                break;
            default:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.ERROR_DISPOSITIVO, tradeResult.name(), Boolean.FALSE));
                break;
        }
    }

    public void onFailTradeResult() {
        onFailTradeResult(mCurrentTradeResult);
    }

    public void onFailTradeResult(String tradeResult) {
        switch (tradeResult) {
            case "ICC":
                onFailTradeResult(QPOSService.DoTradeResult.ICC);
                break;
            default:
                break;
        }
    }

    public void setPosServiceKeyValue(String keyValue) {
        mPosService.setKeyValue(keyValue);
    }

    public void setBlockAddressOnPosService(String blockAddr) {
        mPosService.setBlockaddr(blockAddr);
    }

    public void doMifareCardOnPosService(String doMifareCardParas, int timeout) {
        mPosService.doMifareCard(doMifareCardParas, timeout);
    }

    public void setPosServiceAmount(String decimalesAmount, String cashbackAmount, TransactionAmountData transaction, boolean isPosDisplayAmount) {
        mPosService.setAmount(setDecimalesAmount(transactionAmountData.getAmount()), setDecimalesAmount(transactionAmountData.getCashbackAmount()), transactionAmountData.getCurrencyCode(), transactionAmountData.getTransactionType(), true);
    }

    public void setListCapabillities(final Map<String, String> capabillities) {

        if (mListCapabilities == null) {
            mListCapabilities = new ArrayList<>();
        }

        mListCapabilities.add(EmvAppTag.ICS + "F4F070FAAFFE8000");
        mListCapabilities.add(EmvAppTag.Terminal_Cterminal_contactless_transaction_limitapabilities + capabillities.get(TERMINAL_CAPS));
        mListCapabilities.add(EmvAppTag.Additional_Terminal_Capabilities + capabillities.get(ADITIONAL_CAPS));
        mListCapabilities.add(EmvAppTag.Transaction_Currency_Code + capabillities.get(CURRENCY_CODE));
        mListCapabilities.add(EmvAppTag.Terminal_Country_Code + capabillities.get(COUNTRY_CODE));
        mListCapabilities.add(EmvAppTag.terminal_execute_cvm_limit + capabillities.get(CVM_LIMIT));

        mListCapabilities.add(EmvAppTag.terminal_contactless_offline_floor_limit + qposParameters.getCtlsTransactionFloorLimitValue());
        mListCapabilities.add(EmvAppTag.terminal_contactless_transaction_limit + qposParameters.getCtlsTransactionLimitValue());
        mListCapabilities.add(EmvAppTag.Contactless_CVM_Required_limit + qposParameters.getCtlsTransactionCvmLimitValue());
    }

    public void getPosServicePin(String maskedPAN, String dateforTRX) {
        mPosService.getPin(1, 10, 8, "INGRESE PIN", maskedPAN, dateforTRX, 15);
    }

    //Montón de métodos heredados de la librería del QPOS
    @Override
    public void onRequestWaitingUser() {
        logFlow("onRequestWaitingUser() called");
    }

    @Override
    public void onQposIdResult(final Hashtable<String, String> ksnDict) {
        logFlow("onQposIdResult() called with: ksnDict = [" + ksnDict + "]");
        mQposIdHash = ksnDict;
        mPosService.getQposInfo();
    }

    @Override
    public void onQposKsnResult(final Hashtable<String, String> ksn) {
        logFlow("onQposKsnResult() called with: ksn = [" + ksn + "]");
    }

    @Override
    public void onQposIsCardExist(final boolean haveCard) {
        logFlow("onQposIsCardExist() called with: haveCard = [" + haveCard + "]");
    }

    @Override
    public void onRequestDeviceScanFinished() {
        logFlow("onRequestDeviceScanFinished() called");
    }

    @Override
    public void onQposInfoResult(final Hashtable<String, String> posInfoData) {
        logFlow("onQposInfoResult() called with: posInfoData = [" + posInfoData + "]");
        mQPosDeviceInfo = new QPOSDeviceInfo();

        mQPosDeviceInfo.setUpdateWorkKeyFlag(posInfoData.get("updateWorkKeyFlag"));
        mQPosDeviceInfo.setIsSupportedTrack2(posInfoData.get("isSupportedTrack2"));
        mQPosDeviceInfo.setIsKeyboard(posInfoData.get("isKeyboard"));
        mQPosDeviceInfo.setBatteryPercentage(posInfoData.get("batteryPercentage"));
        mQPosDeviceInfo.setBootloaderVersion(posInfoData.get("bootloaderVersion"));
        mQPosDeviceInfo.setIsSupportedTrack3(posInfoData.get("isSupportedTrack3"));
        mQPosDeviceInfo.setBatteryLevel(posInfoData.get("batteryLevel"));
        mQPosDeviceInfo.setHardwareVersion(posInfoData.get("hardwareVersion"));
        mQPosDeviceInfo.setIsCharging(posInfoData.get("isCharging"));
        mQPosDeviceInfo.setFirmwareVersion(posInfoData.get("firmwareVersion"));
        mQPosDeviceInfo.setIsSupportedTrack1(posInfoData.get("isSupportedTrack1"));
        mQPosDeviceInfo.setIsUsbConnected(posInfoData.get("isUsbConnected"));

        dongleConnect.onDeviceConnected();
    }

    @Override
    public void onQposGenerateSessionKeysResult(final Hashtable<String, String> rsaData) {
        logFlow("onQposGenerateSessionKeysResult() called with: rsaData = [" + rsaData + "]");
        if (rsaData != null && !rsaData.isEmpty()) {

            PosInstance.getInstance().setSessionKeys(rsaData);

//            final QPOSSessionKeys qposSessionKeys = new QPOSSessionKeys(HexUtil.hex2byte(sessionKeys.get("enDataCardKey"), StandardCharsets.ISO_8859_1),
//                    HexUtil.hex2byte(sessionKeys.get("enPinKcvKey").substring(0, 6), StandardCharsets.ISO_8859_1),
//                    HexUtil.hex2byte(sessionKeys.get("enPinKey"), StandardCharsets.ISO_8859_1),
//                    HexUtil.hex2byte(sessionKeys.get("rsaReginLen"), StandardCharsets.ISO_8859_1),
//                    HexUtil.hex2byte(sessionKeys.get("enKcvDataCardKey").substring(0, 6), StandardCharsets.ISO_8859_1),
//                    HexUtil.hex2byte(sessionKeys.get("rsaReginString"), StandardCharsets.ISO_8859_1)
//            );
//
//            setKeySession(qposSessionKeys);
            dongleConnect.onSessionKeysObtenidas();
        } else {

            dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.FAILKEYS, "Error al generar las llaves", false));
        }
    }

    @Override
    public void onQposDoSetRsaPublicKey(boolean isSetRSA) {
        logFlow("onQposDoSetRsaPublicKey() called with: isSetRSA = [" + isSetRSA + "]");
        if (!isSetRSA) {
            dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Clave RSA No Cargada", false));
        } else {
            mPosService.generateSessionKeys();
        }
    }

    @Override
    public void onSearchMifareCardResult(final Hashtable<String, String> cardData) {
        logFlow("onSearchMifareCardResult() called with: cardData = [" + cardData + "]");
        dongleListener.onSearchMifareCardResult(cardData);
    }

    @Override
    public void onBatchReadMifareCardResult(final String msg, final Hashtable<String, List<String>> cardData) {
        logFlow("onBatchReadMifareCardResult() called with: msg = [" + msg + "], cardData = [" + cardData + "]");
        dongleListener.onBatchReadMifareCardResult(msg, cardData);
    }

    @Override
    public void onBatchWriteMifareCardResult(final String msg, final Hashtable<String, List<String>> cardData) {
        logFlow("onBatchWriteMifareCardResult() called with: msg = [" + msg + "], cardData = [" + cardData + "]");
        final String status = mPosService.getMifareStatusMsg();

        if ("SUCCESS".equals(status)) {//NOSONAR
            dongleListener.onBatchWriteMifareCardResult(msg, cardData);
        } else {
            dongleListener.onErrorWriteMifareCard();
        }
    }

    @Override
    public void onDoTradeResult(final QPOSService.DoTradeResult doTradeResult, final Hashtable<String, String> decodeData) {
        logFlow("onDoTradeResult() called with: doTradeResult = [" + doTradeResult + "], decodeData = [" + decodeData + "]");
        mEmvTags.clear();
        mCurrentTradeResult = doTradeResult;

        if (doTradeResult == QPOSService.DoTradeResult.NFC_ONLINE
                || doTradeResult == QPOSService.DoTradeResult.NFC_OFFLINE) {
            String tlv = mPosService.getNFCBatchData().get("tlv");
            decodeData.put("iccdata", tlv);
            dongleListener.onResultData(decodeData, DongleListener.DoTradeResult.NFC_ONLINE);
        } else if (doTradeResult == QPOSService.DoTradeResult.ICC) {
            mPosService.doEmvApp(QPOSService.EmvOption.START);
        } else if (doTradeResult == QPOSService.DoTradeResult.MCR) {
            dongleListener.onResultData(decodeData, DongleListener.DoTradeResult.MCR);
        } else {
            onFailTradeResult(doTradeResult);
        }
//        mQposServiceCallback.onDoTradeResult(hashtable);
    }

    @Override
    public void onFinishMifareCardResult(final boolean isFailedChip) {
        logFlow("onFinishMifareCardResult() called with: isFailedChip = [" + isFailedChip + "]");
        dongleListener.onFinishMifareCardResult(isFailedChip);
    }

    @Override
    public void onVerifyMifareCardResult(final boolean flag) {
        logFlow("onVerifyMifareCardResult() called with: flag = [" + flag + "]");
        final String status = mPosService.getMifareStatusMsg();

        if ("SUCCESS".equals(status)) {//NOSONAR
            dongleListener.onVerifyMifareCardResult(flag);
        } else {
            dongleListener.onErrorWriteMifareCard();
        }
    }

    @Override
    public void onReadMifareCardResult(final Hashtable<String, String> flag) {
        logFlow("onReadMifareCardResult() called with: flag = [" + flag + "]");
    }

    @Override
    public void onWriteMifareCardResult(final boolean flag) {
        logFlow("onWriteMifareCardResult() called with: flag = [" + flag + "]");
    }

    @Override
    public void onOperateMifareCardResult(final Hashtable<String, String> hashtable) {
        logFlow("onOperateMifareCardResult() called with: hashtable = [" + hashtable + "]");
        final String status = mPosService.getMifareStatusMsg();

        if ("SUCCESS".equals(status)) {
            dongleListener.onOperateMifareCardResult(hashtable);
        } else {
            dongleListener.onErrorWriteMifareCard();
        }
    }

    @Override
    public void getMifareCardVersion(final Hashtable<String, String> hashtable) {
        logFlow("getMifareCardVersion() called with: hashtable = [" + hashtable + "]");
    }

    @Override
    public void getMifareReadData(final Hashtable<String, String> hashtable) {
        logFlow("getMifareReadData() called with: hashtable = [" + hashtable + "]");
    }

    @Override
    public void getMifareFastReadData(final Hashtable<String, String> hashtable) {
        logFlow("getMifareFastReadData() called with: hashtable = [" + hashtable + "]");
    }

    @Override
    public void writeMifareULData(final String flag) {
        logFlow("writeMifareULData() called with: flag = [" + flag + "]");
    }

    @Override
    public void verifyMifareULData(final Hashtable<String, String> hashtable) {
        logFlow("verifyMifareULData() called with: hashtable = [" + hashtable + "]");
    }

    @Override
    public void transferMifareData(final String flag) {
        logFlow("transferMifareData() called with: flag = [" + flag + "]");
    }

    @Override
    public void onRequestSetAmount() {
        logFlow("onRequestSetAmount() called");
        mPosService.setAmount(setDecimalesAmount(transactionAmountData.getAmount()), setDecimalesAmount(transactionAmountData.getCashbackAmount()), transactionAmountData.getCurrencyCode(), transactionAmountData.getTransactionType(), true);
    }

    @Override
    public void onRequestSelectEmvApp(final ArrayList<String> listEMVApps) {
        logFlow("onRequestSelectEmvApp() called with: listEMVApps = [" + listEMVApps + "]");
        if (listEMVApps.size() == 1) {
            mPosService.selectEmvApp(0);
        } else if (listEMVApps.size() > 1) {
            final AplicacionEmv aplicacionEmv = new AplicacionEmv() {
                @Override
                public void seleccionAppEmv(final int position) {
                    mPosService.selectEmvApp(position);
                }
            };
            dongleListener.seleccionEmvApp(listEMVApps, aplicacionEmv);
        }
    }

    @Override
    public void onRequestIsServerConnected() {
        logFlow("onRequestIsServerConnected() called");
    }

    @Override
    public void onRequestFinalConfirm() {
        logFlow("onRequestFinalConfirm() called");
    }

    @Override
    public void deviceCancel() {
        logFlow("deviceCancel() called");
        mPosService.close();
        closeCommunication();
    }

    @Override
    public void onRequestOnlineProcess(String tlvString) {
        logFlow("onRequestOnlineProcess() called with: tlvString = [" + tlvString + "]");
        mEmvTags = reciverEMVTags();
        mDecodeData = mPosService.anlysEmvIccData(tlvString);
        mDecodeData.put(ICCDecodeData.TLV.getLabel(), tlvString);
        //TODO Tiempo De Preguntar Los TAGS Al Dongle
//        CamposEMVData.getInstance().reciverEMVTags();
        final String iccTag = "9F33: ".concat(mPosService.getICCTag(0, 1, "9F33").toString());
        logFlow("onRequestOnlineProcess: iccTag = " + iccTag);
        dongleListener.onResultData(mDecodeData, DongleListener.DoTradeResult.ICC);
    }

    @Override
    public void onRequestTime() {
        logFlow("onRequestTime() called");
        Context context = PosInstance.getInstance().getAppContext();
        mPosService.sendTime(new SimpleDateFormat("yyyyMMddHHmmss", context.getResources().getConfiguration().locale).format(Calendar.getInstance().getTime()));
    }

    @Override
    public void onRequestTransactionResult(final QPOSService.TransactionResult transactionResult) {
        logFlow("onRequestTransactionResult() called with: transactionResult = [" + transactionResult + "]");
        switch (transactionResult) {

            case CANCEL:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Operación Cancelada", false));
                break;
            case DECLINED:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.DECLINADO, "Tarjeta No Permitida", false));
                break;
            case TERMINATED:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.TERMINADO, "Operación Finalizada", false));
                break;
            case APPROVED:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.APROBADO, "Operación Finalizada", true));
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestTransactionLog(final String tlv) {
        logFlow("onRequestTransactionLog() called with: tlv = [" + tlv + "]");
    }

    @Override
    public void onRequestBatchData(final String tlv) {
        logFlow("onRequestBatchData() called with: tlv = [" + tlv + "]");
    }

    @Override
    public void onRequestQposConnected() {
        logFlow("onRequestQposConnected() called");
        mQStatePOS.updateState(POSConnectionState.STATE_POS.CONNECTED);
        mPosService.getQposId();
    }

    @Override
    public void onRequestQposDisconnected() {
        logFlow("onRequestQposDisconnected() called");
        dongleConnect.ondevicedisconnected();
    }

    @Override
    public void onRequestNoQposDetected() {
        logFlow("onRequestNoQposDetected() called");
        dongleConnect.onRequestNoQposDetected();
    }

    @Override
    public void onRequestNoQposDetectedUnbond() {
        logFlow("onRequestNoQposDetectedUnbond() called");
    }

    @Override
    public void onError(QPOSService.Error error) {
        logFlow("onError() called with: error = [" + error + "]");

        this.cancelOperacion();

        if (mDecodeData != null) {
            switch (error) {
                case TIMEOUT:
                    onRequestNoQposDetected();
                    break;
                case CMD_TIMEOUT:
                case CMD_NOT_AVAILABLE:
                    dongleConnect.onRequestNoQposDetected();
                    break;
                case INPUT_INVALID:
                    dongleConnect.onRequestNoQposDetected();
                    break;
                case UNKNOWN:
                    // NONE
                    break;
                case DEVICE_RESET:
                    if (transactionAmountData == null && mDecodeData != null) {
                        dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.ERROR_DISPOSITIVO, error.name(), false));
                    }
                    break;
                default:
                    dongleConnect.onRequestNoQposDetected();
                    break;
            }
        } else if (error == QPOSService.Error.CMD_TIMEOUT) {
            PosResult posResult = new PosResult(PosResult.PosTransactionResult.CMD_TIEMPOFINALIZADO,
                    "No se detectó la tarjeta",
                    false);
            dongleListener.onRespuestaDongle(posResult);
        }
    }

    @Override
    public void onRequestDisplay(final QPOSService.Display displayMsg) {
        logFlow("onRequestDisplay() called with: displayMsg = [" + displayMsg + "]");
    }

    @Override
    public void onReturnReversalData(final String tlv) {
        logFlow("onReturnReversalData() called with: tlv = [" + tlv + "]");
    }

    @Override
    public void onReturnGetPinResult(final Hashtable<String, String> result) {
        logFlow("onReturnGetPinResult() called with: result = [" + result + "]");
    }

    @Override
    public void onReturnPowerOnIccResult(final boolean isSuccess, final String ksn, final String atr, final int atrLen) {
        logFlow("onReturnPowerOnIccResult() called with: isSuccess = [" + isSuccess + "], ksn = [" + ksn + "], atr = [" + atr + "], atrLen = [" + atrLen + "]");
    }

    @Override
    public void onReturnPowerOffIccResult(final boolean isSuccess) {
        logFlow("onReturnPowerOffIccResult() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onReturnApduResult(final boolean isSuccess, final String apdu, final int apduLen) {
        logFlow("onReturnApduResult() called with: isSuccess = [" + isSuccess + "], apdu = [" + apdu + "], apduLen = [" + apduLen + "]");
    }

    @Override
    public void onReturnSetSleepTimeResult(final boolean isSuccess) {
        logFlow("onReturnSetSleepTimeResult() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onGetCardNoResult(final String cardNo) {
        logFlow("onGetCardNoResult() called with: cardNo = [" + cardNo + "]");
    }

    @Override
    public void onRequestSignatureResult(final byte[] paras) {
        logFlow("onRequestSignatureResult() called with: paras = [" + paras + "]");
    }

    @Override
    public void onRequestCalculateMac(final String calMac) {
        logFlow("onRequestCalculateMac() called with: calMac = [" + calMac + "]");
    }

    @Override
    public void onRequestUpdateWorkKeyResult(QPOSService.UpdateInformationResult updateInformationResult) {
        logFlow("onRequestUpdateWorkKeyResult() called with: updateInformationResult = [" + updateInformationResult + "]");
    }

    @Override
    public void onReturnCustomConfigResult(final boolean isSuccess, final String result) {
        logFlow("onReturnCustomConfigResult() called with: isSuccess = [" + isSuccess + "], result = [" + result + "]");
        mQposServiceCallback.onReturnCustomConfigResult();
        mPosService.updateEmvAPP(QPOSService.EMVDataOperation.update, mListCapabilities);
    }

    @Override
    public void onRequestSetPin() {
        logFlow("onRequestSetPin() called");
    }

    @Override
    public void onReturnSetMasterKeyResult(final boolean isSuccess) {
        logFlow("onReturnSetMasterKeyResult() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onRequestUpdateKey(final String result) {
        logFlow("onRequestUpdateKey() called with: result = [" + result + "]");
    }

    @Override
    public void onReturnUpdateIPEKResult(final boolean isSuccess) {
        logFlow("onReturnUpdateIPEKResult() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onReturnRSAResult(final String isSuccess) {
        logFlow("onReturnRSAResult() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onReturnUpdateEMVResult(final boolean isSuccess) {
        logFlow("onReturnUpdateEMVResult() called with: isSuccess = [" + isSuccess + "]");

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException exe) {
            Log.e(TAG, "Error al detener el thread");
            Thread.currentThread().interrupt();
        }

        if (isSuccess) {
            mPosService.setOnlineTime(1000);
            mPosService.setCardTradeMode(qposParameters.getCardTradeMode());
            mPosService.doCheckCard(30, 10);
        }
    }

    @Override
    public void onReturnGetQuickEmvResult(final boolean isSuccess) {
        logFlow("onReturnGetQuickEmvResult() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onReturnGetEMVListResult(final String data) {
        logFlow("onReturnGetEMVListResult() called with: data = [" + data + "]");
    }

    @Override
    public void onReturnUpdateEMVRIDResult(final boolean isSuccess) {
        logFlow("onReturnUpdateEMVRIDResult() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onDeviceFound(final BluetoothDevice bluetoothDevice) {
        logFlow("onDeviceFound() called with: bluetoothDevice = [" + bluetoothDevice + "]");
    }

    @Override
    public void onReturnBatchSendAPDUResult(final LinkedHashMap<Integer, String> batchAPDUResult) {
        logFlow("onReturnBatchSendAPDUResult() called with: batchAPDUResult = [" + batchAPDUResult + "]");
    }

    @Override
    public void onBluetoothBonding() {
        logFlow("onBluetoothBonding() called");
    }

    @Override
    public void onBluetoothBonded() {
        logFlow("onBluetoothBonded() called");
    }

    @Override
    public void onWaitingforData(final String pin) {
        logFlow("onWaitingforData() called with: pin = [" + pin + "]");
    }

    @Override
    public void onBluetoothBondFailed() {
        logFlow("onBluetoothBondFailed() called");
    }

    @Override
    public void onBluetoothBondTimeout() {
        logFlow("onBluetoothBondTimeout() called");
    }

    @Override
    public void onReturniccCashBack(final Hashtable<String, String> result) {
        logFlow("onReturniccCashBack() called with: result = [" + result + "]");
    }

    @Override
    public void onLcdShowCustomDisplay(final boolean isSuccess) {
        logFlow("onLcdShowCustomDisplay() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onUpdatePosFirmwareResult(QPOSService.UpdateInformationResult updateInformationResult) {
        logFlow("onUpdatePosFirmwareResult() called with: updateInformationResult = [" + updateInformationResult + "]");
    }

    @Override
    public void onBluetoothBoardStateResult(final boolean result) {
        logFlow("onBluetoothBoardStateResult() called with: result = [" + result + "]");
    }

    @Override
    public void onReturnDownloadRsaPublicKey(final HashMap<String, String> hashMap) {
        logFlow("onReturnDownloadRsaPublicKey() called with: hashMap = [" + hashMap + "]");
    }

    @Override
    public void onGetPosComm(final int mod, final String amount, final String posid) {
        logFlow("onGetPosComm() called with: mod = [" + mod + "], amount = [" + amount + "], posid = [" + posid + "]");
    }

    @Override
    public void onUpdateMasterKeyResult(final boolean result, final Hashtable<String, String> resultTable) {
        logFlow("onUpdateMasterKeyResult() called with: result = [" + result + "], resultTable = [" + resultTable + "]");
    }

    @Override
    public void onPinKey_TDES_Result(final String result) {
        logFlow("onPinKey_TDES_Result() called with: result = [" + result + "]");
    }

    @Override
    public void onEmvICCExceptionData(final String tlv) {
        logFlow("onEmvICCExceptionData() called with: tlv = [" + tlv + "]");
    }

    @Override
    public void onSetParamsResult(final boolean b, final Hashtable<String, Object> resultTable) {
        logFlow("onSetParamsResult() called with: b = [" + b + "], resultTable = [" + resultTable + "]");
    }

    @Override
    public void onGetInputAmountResult(final boolean b, final String amount) {
        logFlow("onGetInputAmountResult() called with: b = [" + b + "], amount = [" + amount + "]");
    }

    @Override
    public void onReturnNFCApduResult(final boolean result, final String apdu, final int apduLen) {
        logFlow("onReturnNFCApduResult() called with: result = [" + result + "], apdu = [" + apdu + "], apduLen = [" + apduLen + "]");
    }

    @Override
    public void onReturnPowerOnNFCResult(final boolean result, final String ksn, final String atr, final int atrLen) {
        logFlow("onReturnPowerOnNFCResult() called with: result = [" + result + "], ksn = [" + ksn + "], atr = [" + atr + "], atrLen = [" + atrLen + "]");
    }

    @Override
    public void onReturnPowerOffNFCResult(final boolean result) {
        logFlow("onReturnPowerOffNFCResult() called with: result = [" + result + "]");
    }

    @Override
    public void onCbcMacResult(final String result) {
        logFlow("onCbcMacResult() called with: result = [" + result + "]");
    }

    @Override
    public void onReadBusinessCardResult(final boolean b, final String result) {
        logFlow("onReadBusinessCardResult() called with: b = [" + b + "], result = [" + result + "]");
    }

    @Override
    public void onWriteBusinessCardResult(boolean b) {
        logFlow("onWriteBusinessCardResult() called with: b = [" + b + "]");
    }

    @Override
    public void onConfirmAmountResult(boolean b) {
        logFlow("onConfirmAmountResult() called with: b = [" + b + "]");
    }

    @Override
    public void onSetManagementKey(boolean b) {
        logFlow("onSetManagementKey() called with: b = [" + b + "]");
    }

    @Override
    public void onSetSleepModeTime(boolean b) {
        logFlow("onSetSleepModeTime() called with: b = [" + b + "]");
    }

    @Override
    public void onGetSleepModeTime(final String b) {
        logFlow("onGetSleepModeTime() called with: b = [" + b + "]");
    }

    @Override
    public void onGetShutDownTime(final String b) {
        logFlow("onGetShutDownTime() called with: b = [" + b + "]");
    }

    @Override
    public void onEncryptData(final String b) {
        logFlow("onEncryptData() called with: b = [" + b + "]");
    }

    @Override
    public void onAddKey(boolean b) {
        logFlow("onAddKey() called with: b = [" + b + "]");
    }

    @Override
    public void onSetBuzzerResult(boolean b) {
        logFlow("onSetBuzzerResult() called with: b = [" + b + "]");
    }

    @Override
    public void onSetBuzzerTimeResult(boolean b) {
        logFlow("onSetBuzzerTimeResult() called with: b = [" + b + "]");
    }

    @Override
    public void onSetBuzzerStatusResult(boolean b) {
        logFlow("onSetBuzzerStatusResult() called with: b = [" + b + "]");
    }

    @Override
    public void onGetBuzzerStatusResult(final String b) {
        logFlow("onGetBuzzerStatusResult() called with: b = [" + b + "]");
    }

    @Override
    public void onQposDoTradeLog(final boolean isSuccess) {
        logFlow("onQposDoTradeLog() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onQposDoGetTradeLogNum(final String data) {
        logFlow("onQposDoGetTradeLogNum() called with: data = [" + data + "]");
    }

    @Override
    public void onQposDoGetTradeLog(final String data, final String orderId) {
        logFlow("onQposDoGetTradeLog() called with: data = [" + data + "], orderId = [" + orderId + "]");
    }

    @Override
    public void onRequestDevice() {
        logFlow("onRequestDevice() called");
    }

    @Override
    public void onGetKeyCheckValue(final List<String> checkValues) {
        logFlow("onGetKeyCheckValue() called with: checkValues = [" + checkValues + "]");
    }

    @Override
    public void onGetDevicePubKey(final String clearKeys) {
        logFlow("onGetDevicePubKey() called with: clearKeys = [" + clearKeys + "]");
    }

    @Override
    public void onSetPosBlePinCode(boolean b) {
        logFlow("onSetPosBlePinCode() called with: b = [" + b + "]");
    }

    @Override
    public void onTradeCancelled() {
        logFlow("onTradeCancelled() called");
    }

    @Override
    public void onReturnSetAESResult(final boolean isSuccess, final String result) {
        logFlow("onReturnSetAESResult() called with: isSuccess = [" + isSuccess + "], result = [" + result + "]");
    }

    @Override
    public void onReturnAESTransmissonKeyResult(final boolean isSuccess, final String result) {
        logFlow("onReturnAESTransmissonKeyResult() called with: isSuccess = [" + isSuccess + "], result = [" + result + "]");
    }

    @Override
    public void onReturnSignature(final boolean b, final String signaturedData) {
        logFlow("onReturnSignature() called with: b = [" + b + "], signaturedData = [" + signaturedData + "]");
    }

    @Override
    public void onReturnConverEncryptedBlockFormat(final String result) {
        logFlow("onReturnConverEncryptedBlockFormat() called with: result = [" + result + "]");
    }

    @Override
    public void onQposIsCardExistInOnlineProcess(final boolean haveCard) {
        logFlow("onQposIsCardExistInOnlineProcess() called with: haveCard = [" + haveCard + "]");
    }

    private String setDecimalesAmount(final String monto) {
        String amount = monto;
        //TODO Seleccionar el monto del pais - difiere del que existen el la BD ?
        if (transactionAmountData.getDecimales() == 0 && !"".equals(monto)) {
            amount = amount.concat("00");
        }
        return amount;
    }

    public String getDateforTRX() {
        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        final Date date = new Date();
        return dateFormat.format(date);
    }

    public Map<String, String> reciverEMVTags() {

        final StringBuilder sBuilder = new StringBuilder();
        StringBuilder tlvresponcebuilder;

        for (final String tag : TAGSEMV) {
            sBuilder.append(tag);
        }

        final Integer emvLength = Integer.valueOf(TAGSEMV.length);
        Map<String, String> tags = mPosService.getICCTag(QPOSService.EncryptType.PLAINTEXT, 0, emvLength, sBuilder.toString());
        if (tags.containsKey("tlv")) {
            String iccTlv = tags.get("tlv");
            tags.putAll(BBDeviceController.decodeTlv(iccTlv));
        }

        return tags;
    }

    private void logFlow(String whatToLog) {
        if (isLogEnabled) {
            Log.d(TAG, whatToLog);
        }
    }

    public void setLogEnabled(boolean logEnabled) {
        isLogEnabled = logEnabled;
    }
}
