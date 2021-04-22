package com.pagatodo.qposlib;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import com.bbpos.bbdevice.BBDeviceController;
import com.dspread.xpos.QPOSService;
import com.pagatodo.qposlib.abstracts.AbstractDongle;
import com.pagatodo.qposlib.dongleconnect.DongleConnect;
import com.pagatodo.qposlib.dongleconnect.DongleListener;
import com.pagatodo.qposlib.dongleconnect.TransactionAmountData;
import com.pagatodo.qposlib.emv.EmvTags;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
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

import Decoder.BASE64Decoder;

public class QPosManager<T extends DspreadDevicePOS> extends AbstractDongle implements QPOSService.QPOSServiceListener {

    private final String TAG = QPosManager.class.getSimpleName();
    private final POSConnectionState mQStatePOS = new POSConnectionState();
    private final DspreadDevicePOS<Parcelable> mDevicePos;
    public static final String REQUIERE_PIN = "INGRESE PIN";

    private Consumer<Boolean> onReturnCustomConfigConsumer;
    private Consumer<Boolean> onAidConfigOverrideConsumer;
    private UpdateThread updateThread;

    private QPOSService mPosService;
    private DspreadDevicePOS mDevice;
    private TransactionAmountData transactionAmountData;
    private QPOSDeviceInfo mQPosDeviceInfo;
    private QPOSService.DoTradeResult mCurrentTradeResult;
    private Hashtable<String, String> mDecodeData;
    private Map<String, String> mEmvTags = new ArrayMap<>();
    private Hashtable<String, String> mQposIdHash;
    private QposParameters qposParameters;
    private String[] aidTlvList;

    private int aidListCount;
    private boolean isUpdatingAid;
    private boolean isUpdatingFirmware;
    private final boolean isLogEnabled;

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

    @Override
    public Hashtable<String, String> getQposIdHash() {
        logFlow("getQposIdHash() returned: " + mQposIdHash);
        return mQposIdHash;
    }

    @Override
    public void setReaderEmvConfig(String emvCfgAppHex, String emvCfgCapkHex, Consumer<Boolean> onReturnCustomConfigConsumer) {
        logFlow("setReaderEmvConfig() called with: emvCfgAppHex = [" + emvCfgAppHex + "]");
        logFlow("setReaderEmvConfig() called with: emvCfgCapkHex = [" + emvCfgCapkHex + "]");
        this.onReturnCustomConfigConsumer = onReturnCustomConfigConsumer;
        mPosService.updateEmvConfig(emvCfgAppHex, emvCfgCapkHex);
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
        logFlow("closeCommunication() called: isUpdatingFirmware = [" + isUpdatingFirmware + ']');

        // TODO: This method is also called when updating the firmware
        if (isUpdatingFirmware) {
        } else if (mPosService != null) {
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
    public void getPin(int maxLen, final String maskedPAN) {
        logFlow("getPin() called with: maxLen = [" + maxLen + "], maskedPAN = [" + maskedPAN + "]");
        mPosService.getPin(1, 10, maxLen, REQUIERE_PIN, maskedPAN, getDateforTRX(), 15);
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
    public void doTransaccion(TransactionAmountData transactionAmountData, QposParameters qposParameters) {
        logFlow("doTransaccion() called with: transactionAmountData = [" + transactionAmountData + "], qposParameters = [" + qposParameters + "]");

        if (!mPosService.isQposPresent()) {
            onRequestQposDisconnected();
        } else {
            this.transactionAmountData = transactionAmountData;
            this.qposParameters = qposParameters;

            mPosService.setQuickEmv(transactionAmountData.getTipoOperacion().equalsIgnoreCase("X")
                    || transactionAmountData.getTipoOperacion().equalsIgnoreCase("Z"));

            mPosService.setFormatId("0025");
            mPosService.setOnlineTime(1000);
            mPosService.setCardTradeMode(qposParameters.getCardTradeMode());
            mPosService.setAmountIcon(transactionAmountData.getAmountIcon());
            mPosService.setAmountPoint(qposParameters.getExponent() > 0);

            if (dongleListener.checkDoTrade()) {
                mPosService.doTrade(10, 30);
            } else {
                mPosService.doCheckCard(30, 10);
            }
        }
    }

    public void doTransaccionNextOperation(TransactionAmountData transactionAmountData, QposParameters qposParameters) {
        logFlow("doTransaccion() called with: transactionAmountData = [" + transactionAmountData + "]");

        if (mPosService.isQposPresent()) {
            this.transactionAmountData = transactionAmountData;

            mPosService.setQuickEmv(transactionAmountData.getTipoOperacion().equalsIgnoreCase("X")
                    || transactionAmountData.getTipoOperacion().equalsIgnoreCase("Z"));

            mPosService.setFormatId("0025");
            mPosService.doEmvApp(QPOSService.EmvOption.START);
        } else {
            onRequestQposDisconnected();
        }
    }

    public PublicKey getPublicKey(final String filename, final Context contextApp) throws Exception {//NOSONAR
        logFlow("getPublicKey() called with: filename = [" + filename + "], contextApp = [" + contextApp + "]");

        final InputStream inputStream = contextApp.getAssets().open(filename);
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder sb = new StringBuilder();
        String line;

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

    @Override
    public void setEmvAidUpdate(ArrayList<String> aidConfigList, Consumer<Boolean> onEmvAidConfigUpdateConsumer) {
        logFlow("setEmvAppUpdate() called with: aidConfigList = [" + aidConfigList + "]");
        this.onAidConfigOverrideConsumer = onEmvAidConfigUpdateConsumer;
        mPosService.updateEmvAPP(QPOSService.EMVDataOperation.update, aidConfigList);
    }

    @Override
    public void setAidTlvUpdate(@NonNull String[] aidTlvList, Consumer<Boolean> onAidTlvUpdateConsumer) {
        logFlow("setAidTlvUpdate() called with: aidTlvList = [" + Arrays.toString(aidTlvList) + "]");
        this.onAidConfigOverrideConsumer = onAidTlvUpdateConsumer;
        this.aidTlvList = aidTlvList;

        isUpdatingAid = true;
        aidListCount = 0;
        updateEmvAid();
    }

    private void updateEmvAid() {
        if (aidListCount < aidTlvList.length) {
//            Log.d(TAG, "updateEmvAid: tlv = " + aidTlvList[aidListCount]);
            mPosService.updateEmvAPPByTlv(QPOSService.EMVDataOperation.update, aidTlvList[aidListCount]);
//            Log.d(TAG, "updateEmvAid: count = " + aidListCount);
            aidListCount++;
        } else {
            isUpdatingAid = false;
            onAidConfigOverrideConsumer.accept(true);
        }
    }

    public int updateFirmware(@NonNull Context context, byte[] dataToUpdate, String file) {
        int result = mPosService.updatePosFirmware(dataToUpdate, file);
        logFlow("updateFirmware: " + result);

        if (result != 0) {
            firmwareUpdate.onPosFirmwareUpdateResult(false, "Device is not charging");
        } else {
            updateThread = new UpdateThread(context);
            updateThread.setContinueFlag(true);
            updateThread.start();
//            isUpdatingFirmware = true;
        }

        return result;
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
        logFlow("getEmvTags() returned: " + mEmvTags);
        return mEmvTags;
    }

    @Override
    public DspreadDevicePOS getDeviePos() {
        logFlow("getDeviePos() returned: " + mDevice);
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
            case TRY_ANOTHER_INTERFACE:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.TRY_ANOTHER_INTERFACE, "Pase Por Contacto o Utilice Otra Tarjeta", false));
                break;
            default:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.ERROR_DISPOSITIVO, tradeResult.name(), Boolean.FALSE));
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

    //Montón de métodos heredados de la librería del QPOS

    @Override
    public void onQposTestSelfCommandResult(final boolean isSuccess, final String datas) {
        logFlow("onQposTestSelfCommandResult() called with: isSuccess = [" + isSuccess + "], datas = [" + datas + "]");
    }

    @Override
    public void onQposTestCommandResult(boolean isSuccess, String dataResult) {
        logFlow("onQposTestCommandResult() called with: isSuccess = [" + isSuccess + "], dataResult = [" + dataResult + "]");
    }

    @Override
    public void onQposRequestPinResult(List<String> dataResult, int offlineTime) {
        logFlow("onQposRequestPinResult() called with: dataResult = [" + dataResult + "], offlineTime = [" + offlineTime + "]");
    }

    @Override
    public void onReturnD20SleepTimeResult(boolean isSuccess) {
        logFlow("onReturnD20SleepTimeResult() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onQposRequestPinStartResult(List<String> dataResult) {
        logFlow("onQposRequestPinStartResult() called with: dataResult = [" + dataResult + "]");
    }

    @Override
    public void onQposPinMapSyncResult(boolean isSuccess, boolean isNeedPin) {
        logFlow("onQposPinMapSyncResult() called with: isSuccess = [" + isSuccess + "], isNeedPin = [" + isNeedPin + "]");

    }

    @Override
    public void onRequestWaitingUser() {
        logFlow("onRequestWaitingUser() called");
    }

    @Override
    public void onReturnRsaResult(String data) {
        logFlow("onReturnRsaResult() called with: data = [" + data + "]");
    }

    @Override
    public void onQposInitModeResult(boolean isSuccess) {
        logFlow("onQposInitModeResult() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onD20StatusResult(String data) {
        logFlow("onD20StatusResult() called with: data = [" + data + "]");
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
    public void onQposCertificateInfoResult(List<String> deviceInfoData) {
        logFlow("onQposCertificateInfoResult() called with: deviceInfoData = [" + deviceInfoData + "]");
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

        mCurrentTradeResult = doTradeResult;
        mDecodeData = decodeData;
        mEmvTags.clear();

        if (doTradeResult == QPOSService.DoTradeResult.NFC_ONLINE
                || doTradeResult == QPOSService.DoTradeResult.NFC_OFFLINE) {
            String tlv = mPosService.getNFCBatchData().get("tlv");
            decodeData.put("iccdata", tlv);
            mEmvTags = reciverEMVTags(DongleListener.DoTradeResult.NFC_ONLINE);
            dongleListener.onResultData(decodeData, DongleListener.DoTradeResult.NFC_ONLINE);
        } else if (doTradeResult == QPOSService.DoTradeResult.ICC) {
            mPosService.doEmvApp(dongleListener.isPinMandatory()
                    ? QPOSService.EmvOption.START_WITH_FORCE_PIN
                    : QPOSService.EmvOption.START);
        } else if (doTradeResult == QPOSService.DoTradeResult.MCR) {
            dongleListener.onResultData(decodeData, DongleListener.DoTradeResult.MCR);
        } else if (doTradeResult == QPOSService.DoTradeResult.NFC_DECLINED) {
            dongleListener.onResultData(decodeData, DongleListener.DoTradeResult.NFC_DECLINED);
        } else if (doTradeResult == QPOSService.DoTradeResult.PLS_SEE_PHONE) {
            dongleListener.onResultData(decodeData, DongleListener.DoTradeResult.SEE_PHONE);
        } else {
            onFailTradeResult(doTradeResult);
        }
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
        String amount = transactionAmountData.getAmount();
        String cashback = transactionAmountData.getCashbackAmount();
        String currencyCode = transactionAmountData.getCurrencyCode();
        QPOSService.TransactionType transactionType = transactionAmountData.getTransactionType();

        logFlow("onRequestSetAmount(): amount = [" + amount + "], cashback = [" + cashback + "], currencyCode = [" + currencyCode + "], transactionType = [" + transactionType + "]");
        mPosService.setAmount(setDecimalesAmount(amount), setDecimalesAmount(cashback), currencyCode, transactionType, true);
    }

    @Override
    public void onRequestSelectEmvApp(final ArrayList<String> listEMVApps) {
        logFlow("onRequestSelectEmvApp() called with: listEMVApps = [" + listEMVApps + "]");

        if (listEMVApps.size() == 1) {
            mPosService.selectEmvApp(0);
        } else if (listEMVApps.size() > 1) {
            final Consumer<Integer> aplicacionEmv = new Consumer<Integer>() {
                @Override
                public void accept(Integer position) {
                    logFlow("onRequestSelectEmvApp: position = [" + position + "]");

                    if (position < 0 || position > listEMVApps.size()) {
                        mPosService.cancelSelectEmvApp();
                    } else {
                        mPosService.selectEmvApp(position);
                    }
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

        mDecodeData = mPosService.anlysEmvIccData(tlvString);
        mDecodeData.put(ICCDecodeData.TLV.getLabel(), tlvString);
        mEmvTags = reciverEMVTags(DongleListener.DoTradeResult.ICC);
        dongleListener.onResultData(mDecodeData, DongleListener.DoTradeResult.ICC);
    }

    @Override
    public void onRequestTime() {
        Locale locale = PosInstance.getInstance().getAppContext().getResources().getConfiguration().locale;
        String formattedTime = new SimpleDateFormat("yyyyMMddHHmmss", locale)
                .format(Calendar.getInstance().getTime());
        logFlow("onRequestTime: " + formattedTime);
        mPosService.sendTime(formattedTime);
    }

    @Override
    public void onRequestTransactionResult(final QPOSService.TransactionResult transactionResult) {
        logFlow("onRequestTransactionResult() called with: transactionResult = [" + transactionResult + "]");

        switch (transactionResult) {
            case APPROVED:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.APROBADO, "Operación Finalizada", true));
                break;
            case CANCEL:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Operación Cancelada", false));
                break;
            case DECLINED:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.DECLINADO, "Tarjeta Declinada", false));
                break;
            case TERMINATED:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.TERMINADO, "Operación Finalizada", false));
                break;
            case NFC_TERMINATED:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.NFC_TERMINATED, "Error al Procesar la Tarjeta", false));
                break;
            case SELECT_APP_FAIL:
                dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CARD_BLOCKED_OR_NO_EMV_APPS, "Error al Leer la Tarjeta", false));
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
        logFlow("onRequestQposDisconnected() called: isUpdatingFirmware = [" + isUpdatingFirmware + ']');

        // TODO: This method is also called just before updating the firmware
        if (!isUpdatingFirmware) {
            dongleConnect.ondevicedisconnected();
        }
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

        if (isUpdatingFirmware) {
            updateThread.setContinueFlag(false);
            firmwareUpdate.onPosFirmwareUpdateResult(false, error.name());
        } else if (mDecodeData != null) {
            switch (error) {
                case TIMEOUT:
                    onRequestNoQposDetected();
                    break;
                case UNKNOWN:
                    dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.UNKNOWN, error.name(), false));
                    break;
                case DEVICE_RESET:
                    if (transactionAmountData == null) {
                        dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.ERROR_DISPOSITIVO, error.name(), false));
                    }
                    break;
                case CMD_TIMEOUT:
                case CMD_NOT_AVAILABLE:
                case INPUT_INVALID:
                default:
                    dongleConnect.onRequestNoQposDetected();
                    break;
            }
        } else if (error == QPOSService.Error.CMD_TIMEOUT) {
            PosResult posResult = new PosResult(PosResult.PosTransactionResult.CMD_TIEMPOFINALIZADO,
                    "No se detectó la tarjeta",
                    false);
            dongleListener.onRespuestaDongle(posResult);
        } else if (error == QPOSService.Error.INPUT_INVALID) {
            PosResult posResult = new PosResult(PosResult.PosTransactionResult.INPUT_INVALID,
                    "Monto Inválido",
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
        dongleListener.onSyncRequested(tlv);
    }

    @Override
    public void onReturnGetPinInputResult(int result) {
        logFlow("onReturnGetPinInputResult() called with: result = [" + result + "]");
    }

    @Override
    public void onReturnGetKeyBoardInputResult(String result) {
        logFlow("onReturnGetKeyBoardInputResult() called with: result = [" + result + "]");
    }

    @Override
    public void onReturnGetPinResult(final Hashtable<String, String> result) {
        logFlow("onReturnGetPinResult() called with: result = [" + result + "]");

        if (result != null) {
            if (mDecodeData == null) {
                mDecodeData = result;
            } else {
                mDecodeData.putAll(result);
            }
        }

        dongleListener.onPinResult(mDecodeData);
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
    public void onRequestSendTR31KeyResult(boolean result) {
        logFlow("onRequestSendTR31KeyResult() called with: result = [" + result + "]");
    }

    @Override
    public void onReturnCustomConfigResult(final boolean isSuccess, final String result) {
        logFlow("onReturnCustomConfigResult() called with: isSuccess = [" + isSuccess + "], result = [" + result + "]");
        onReturnCustomConfigConsumer.accept(isSuccess);
        onReturnCustomConfigConsumer = null;
    }

    @Override
    public void onRetuenGetTR31Token(String datas) {
        logFlow("onRetuenGetTR31Token() called with: datas = [" + datas + "]");
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
        if (isUpdatingAid) {
            updateEmvAid();
        } else {
            onAidConfigOverrideConsumer.accept(isSuccess);
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
    public void onReturnGetCustomEMVListResult(Map<String, String> data) {
        logFlow("onReturnGetCustomEMVListResult() called with: data = [" + data + "]");
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
    public void onUpdatePosFirmwareResult(QPOSService.UpdateInformationResult result) {
        logFlow("onUpdatePosFirmwareResult() called with: result = [" + result + "]");
        updateThread.setContinueFlag(false);
//        isUpdatingFirmware = false;

        if (result != QPOSService.UpdateInformationResult.UPDATE_SUCCESS) {
            firmwareUpdate.onPosFirmwareUpdateResult(false, result.name());
        } else {
            firmwareUpdate.onPosFirmwareUpdateResult(true, null);
        }
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
    public void onSetVendorIDResult(boolean b, Hashtable<String, Object> resultTable) {
        logFlow("onSetVendorIDResult() called with: b = [" + b + "], resultTable = [" + resultTable + "]");
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
        dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Operación Cancelada", false));
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

    @Override
    public void onReturnSetConnectedShutDownTimeResult(boolean isSuccess) {
        logFlow("onReturnSetConnectedShutDownTimeResult() called with: isSuccess = [" + isSuccess + "]");
    }

    @Override
    public void onReturnGetConnectedShutDownTimeResult(String b) {
        logFlow("onReturnGetConnectedShutDownTimeResult() called with: b = [" + b + "]");
    }

    @Override
    public void onRequestNFCBatchData(QPOSService.TransactionResult transactionResult, String tlv) {
        logFlow("onRequestNFCBatchData() called with: transactionResult = [" + transactionResult + "], tlv = [" + tlv + "]");
    }

    private String setDecimalesAmount(final String monto) {
//        String amount = monto;
        //TODO Seleccionar el monto del pais - difiere del que existen el la BD ?
//        if (transactionAmountData.getDecimales() == 0 && !"".equals(monto)) {
//            amount = amount.concat("00");
//        }

        final String amount;
        if (monto.isEmpty()) {
            amount = monto;
        } else {
            final BigDecimal nPow = BigDecimal.valueOf(Math.pow(10, qposParameters.getExponent()));
            amount = new BigDecimal(monto).multiply(nPow)
                    .setScale(0, RoundingMode.HALF_UP)
                    .toPlainString();
        }

        logFlow("setDecimalesAmount() returned: " + amount);
        return amount;
    }

    public String getDateforTRX() {
        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        final String formattedDate = dateFormat.format(new Date());
        logFlow("getDateforTRX() returned: " + formattedDate);
        return formattedDate;
    }

    public Map<String, String> reciverEMVTags(DongleListener.DoTradeResult tradeResult) {
        Pair<String, Integer> pair = EmvTags.getAsString(
                EmvTags.APPLICATION_IDENTIFIER,
                EmvTags.APPLICATION_DEDICATED_FILE_NAME,
                EmvTags.APPLICATION_PREFERRED_NAME,
                EmvTags.APPLICATION_PRIORITY_INDICATOR,
                EmvTags.TRACK2_EQUIVALENT_DATA,
                EmvTags.APPLICATION_INTERCHANGE_PROFILE,
                EmvTags.APPLICATION_USAGE_CONTROL,
                EmvTags.APPLICATION_CRYPTOGRAM,
                EmvTags.CRYPTOGRAM_INFORMATION_DATA,
                EmvTags.CARD_APPLICATION_VERSION,
                EmvTags.CARDHOLDER_VERIFICATION_METHOD_LIST,
                EmvTags.CARDHOLDER_VERIFICATION_RESULTS,
                EmvTags.CARDHOLDER_MOBILE_VERIFICATION_RESULTS,
                EmvTags.ISSUER_APPLICATION_DATA,
                EmvTags.ISSUER_ACTION_CODE_DEFAULT,
                EmvTags.ISSUER_ACTION_CODE_DENIAL,
                EmvTags.ISSUER_ACTION_CODE_ONLINE,
                EmvTags.TERMINAL_VERIFICATION_RESULTS,
                EmvTags.TRANSACTION_STATUS_INDICATOR,
                EmvTags.ISSUER_COUNTRY_CODE,
                EmvTags.TERMINAL_CAPABILITIES,
                EmvTags.MERCHANT_NAME_AND_LOCATION,
                EmvTags.AMOUNT_AUTHORIZED,
                EmvTags.AMOUNT_OTHER,
                EmvTags.TRANSACTION_CURRENCY_CODE,
                EmvTags.TRANSACTION_CURRENCY_EXPONENT,
                EmvTags.TERMINAL_TRANSACTION_QUALIFIERS
        );

        Map<String, String> tags = mPosService.getICCTag(QPOSService.EncryptType.PLAINTEXT,
                tradeResult == DongleListener.DoTradeResult.ICC ? 0 : 1, pair.second, pair.first);
        logFlow("reciverEMVTags: " + tags);

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

    private class UpdateThread extends Thread {
        private boolean continueFlag = true;
        private Handler handler;

        private UpdateThread(@NonNull Context context) {
            handler = new Handler(context.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    QPosManager.this.firmwareUpdate.onPosFirmwareUpdateProgress(msg.what);
                }
            };
        }

        @Override
        public void run() {
            super.run();

            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }

                if (qposDspread == null) {
                    continueFlag = false;
                } else {
                    int percentage = mPosService.getUpdateProgress();
                    logFlow("updateProgress: " + percentage);

                    if (percentage < 100) {
                        handler.sendEmptyMessage(percentage);
                    } else {
                        continueFlag = false;
                    }
                }
            } while (continueFlag);
            handler = null;
        }

        public void setContinueFlag(boolean continueFlag) {
            this.continueFlag = continueFlag;
            isUpdatingFirmware = continueFlag;
        }
    }
}