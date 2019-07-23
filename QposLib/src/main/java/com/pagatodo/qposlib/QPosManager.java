package com.pagatodo.qposlib;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;

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
    private final POSConnectionState mQStatePOS = new POSConnectionState();
    private final DspreadDevicePOS<Parcelable> mDevicePos;
    private final String TAG = QPosManager.class.getSimpleName();
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

    public QPosManager(final T dongleDevice, final DongleConnect listener) {
        super(listener);
        mQStatePOS.updateState(POSConnectionState.STATE_POS.NONE);
        this.mDevicePos = dongleDevice;
        this.setQposDspread(this);
    }

    @Override
    public Hashtable<String, String> getQposIdHash() {
        return mQposIdHash;
    }

    public void setQposServiceListener(QposServiceListener qposServiceCallback) {
        mQposServiceCallback = qposServiceCallback;
    }

    @Override
    public void openCommunication() {
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
        mPosService.resetQPOS();
    }

    @Override
    public String getPosInfo() {
        return null;
    }

    @Override
    public void getPin(String maskedPAN) {
        mPosService.getPin(1, 10, 8, REQUIERE_PIN, maskedPAN, getDateforTRX(), 15);
    }

    @Override
    public void getSessionKeys(final String clavePublicaFile, final Context context) {
        try {
            mPosService.updateRSA(getPublicKey(clavePublicaFile, context), clavePublicaFile);
        } catch (Exception exe) {

            dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Llave No Generada", false));
        }
    }

    @Override
    public void doTransaccion(TransactionAmountData transactionAmountData) {

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
            Log.i(TAG, lisCap);

            mPosService.updateEmvAPP(QPOSService.EMVDataOperation.update, mListCapabilities);
        } else {
            onRequestQposDisconnected();
        }
    }

    public PublicKey getPublicKey(final String filename, final Context contextApp) throws Exception {//NOSONAR
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
        return keyFactory.generatePublic(keySpec);
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

    public void logCapabilities() {
        final String lisCap = "Capacidades : ".concat(Arrays.toString(mListCapabilities.toArray()));
        Log.i(TAG, lisCap);
    }

    public int getUpdateProgress() {
        return mPosService.getUpdateProgress();
    }

    public void cancelOperacion() {
        mPosService.resetQPOS();
    }

    public void operacionFinalizada(String arpc) {
        mPosService.sendOnlineProcessResult(arpc);
    }

    public Map<String, String> getIccTags() {
        return mEmvTags;
    }

    @Override
    public DspreadDevicePOS getDeviePos() {
        return null;
    }

    public DspreadDevicePOS getDevicePos() {
        return mDevice;
    }

    public QPOSDeviceInfo getDevicePosInfo() {
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
    }

    public void getPosServicePin(String maskedPAN, String dateforTRX) {
        mPosService.getPin(1, 10, 8, "INGRESE PIN", maskedPAN, dateforTRX, 15);
    }

    //Montón de métodos heredados de la librería del QPOS
    @Override
    public void onRequestWaitingUser() {

    }

    @Override
    public void onQposIdResult(Hashtable<String, String> hashtable) {
        mQposIdHash = hashtable;
        mPosService.getQposInfo();
        mPosService.generateSessionKeys();
    }

    @Override
    public void onQposKsnResult(Hashtable<String, String> hashtable) {

    }

    @Override
    public void onQposIsCardExist(boolean b) {

    }

    @Override
    public void onRequestDeviceScanFinished() {

    }

    @Override
    public void onQposInfoResult(Hashtable<String, String> posInfoData) {
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
//        mPosService.generateSessionKeys();
    }

    @Override
    public void onQposGenerateSessionKeysResult(Hashtable<String, String> sessionKeys) {
        if (sessionKeys != null && !sessionKeys.isEmpty()) {

            PosInstance.getInstance().setSessionKeys(sessionKeys);

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
        if (!isSetRSA) {
            dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.CANCELADO, "Clave RSA No Cargada", false));
        } else {
            mPosService.getQposId();
        }
    }

    @Override
    public void onSearchMifareCardResult(Hashtable<String, String> hashtable) {
        Log.i(TAG, "onSearchMifareCardResult " + mPosService.getMifareStatusMsg());
        dongleListener.onSearchMifareCardResult(hashtable);
    }

    @Override
    public void onBatchReadMifareCardResult(String s, Hashtable<String, List<String>> hashtable) {
        Log.i(TAG, "onBatchReadMifareCardResult " + mPosService.getMifareStatusMsg());
        dongleListener.onBatchReadMifareCardResult(s, hashtable);
    }

    @Override
    public void onBatchWriteMifareCardResult(String s, Hashtable<String, List<String>> hashtable) {
        final String status = mPosService.getMifareStatusMsg();
        Log.i(TAG, "onBatchWriteMifareCardResult " + status);

        if ("SUCCESS".equals(status)) {//NOSONAR
            dongleListener.onBatchWriteMifareCardResult(s, hashtable);
        } else {
            dongleListener.onErrorWriteMifareCard();
        }
    }

    @Override
    public void onDoTradeResult(QPOSService.DoTradeResult doTradeResult, Hashtable<String, String> hashtable) {
        mEmvTags.clear();
        mCurrentTradeResult = doTradeResult;
        Log.i(TAG, mCurrentTradeResult.name());
        Log.i(TAG, doTradeResult.name());
        Log.i(TAG, doTradeResult.name());

        if (doTradeResult != QPOSService.DoTradeResult.MCR && doTradeResult != QPOSService.DoTradeResult.ICC) {
            onFailTradeResult(doTradeResult);
        } else {
            mEmvTags = reciverEMVTags();
            if (doTradeResult == QPOSService.DoTradeResult.MCR) {
                dongleListener.onResultData(hashtable, DongleListener.DoTradeResult.MCR);
            } else {
                Log.i(TAG, " ");
                mPosService.doEmvApp(QPOSService.EmvOption.START);
            }
        }
//        mQposServiceCallback.onDoTradeResult(hashtable);
    }

    @Override
    public void onFinishMifareCardResult(boolean isFailedChip) {
        Log.i(TAG, "onFinishMifareCardResult " + mPosService.getMifareStatusMsg());
        dongleListener.onFinishMifareCardResult(isFailedChip);
    }

    @Override
    public void onVerifyMifareCardResult(boolean isFailedChip) {
        final String status = mPosService.getMifareStatusMsg();
        Log.i(TAG, " onVerifyMifareCardResult " + mPosService.getMifareStatusMsg());

        if ("SUCCESS".equals(status)) {//NOSONAR
            dongleListener.onVerifyMifareCardResult(isFailedChip);
        } else {
            dongleListener.onErrorWriteMifareCard();
        }
    }

    @Override
    public void onReadMifareCardResult(Hashtable<String, String> hashtable) {
        Log.i(TAG, "onReadMifareCardResult " + mPosService.getMifareStatusMsg());
    }

    @Override
    public void onWriteMifareCardResult(boolean b) {
        Log.i(TAG, "onWriteMifareCardResult " + mPosService.getMifareStatusMsg());
    }

    @Override
    public void onOperateMifareCardResult(Hashtable<String, String> hashtable) {
        final String status = mPosService.getMifareStatusMsg();
        Log.i(TAG, "onOperateMifareCardResult " + mPosService.getMifareStatusMsg());

        if ("SUCCESS".equals(status)) {
            dongleListener.onOperateMifareCardResult(hashtable);
        } else {
            dongleListener.onErrorWriteMifareCard();
        }
    }

    @Override
    public void getMifareCardVersion(Hashtable<String, String> hashtable) {
        Log.i(TAG, "getMifareCardVersion " + mPosService.getMifareStatusMsg());
    }

    @Override
    public void getMifareReadData(Hashtable<String, String> hashtable) {
        Log.i(TAG, "getMifareReadData " + mPosService.getMifareStatusMsg());
    }

    @Override
    public void getMifareFastReadData(Hashtable<String, String> hashtable) {
        Log.i(TAG, "getMifareFastReadData " + mPosService.getMifareStatusMsg());
    }

    @Override
    public void writeMifareULData(String s) {
        Log.i(TAG, "writeMifareULData " + mPosService.getMifareStatusMsg());
    }

    @Override
    public void verifyMifareULData(Hashtable<String, String> hashtable) {
        Log.i(TAG, "verifyMifareULData " + mPosService.getMifareStatusMsg());
    }

    @Override
    public void transferMifareData(String s) {
        Log.i(TAG, "transferMifareData " + mPosService.getMifareStatusMsg());
    }

    @Override
    public void onRequestSetAmount() {
        mPosService.setAmount(setDecimalesAmount(transactionAmountData.getAmount()), setDecimalesAmount(transactionAmountData.getCashbackAmount()), transactionAmountData.getCurrencyCode(), transactionAmountData.getTransactionType(), true);
    }

    @Override
    public void onRequestSelectEmvApp(ArrayList<String> listEMVApps) {
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

    }

    @Override
    public void onRequestFinalConfirm() {

    }

    @Override
    public void deviceCancel() {
        mPosService.close();
        closeCommunication();
    }

    @Override
    public void onRequestOnlineProcess(String tlvString) {
        Log.i(TAG, tlvString);
        mDecodeData = mPosService.anlysEmvIccData(tlvString);
        mDecodeData.put(ICCDecodeData.TLV.getLabel(), tlvString);
        //TODO Tiempo De Preguntar Los TAGS Al Dongle
//        CamposEMVData.getInstance().reciverEMVTags();
        final String iccTag = "9F33: ".concat(mPosService.getICCTag(0, 1, "9F33").toString());
        Log.v(TAG, iccTag);
        dongleListener.onResultData(mDecodeData, DongleListener.DoTradeResult.ICC);
    }

    @Override
    public void onRequestTime() {
        Log.i(TAG, "Ingresand Tiempo al PosInterface");
        Context context = PosInstance.getInstance().getAppContext();
        mPosService.sendTime(new SimpleDateFormat("yyyyMMddHHmmss", context.getResources().getConfiguration().locale).format(Calendar.getInstance().getTime()));
    }

    @Override
    public void onRequestTransactionResult(QPOSService.TransactionResult transactionResult) {
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
    public void onRequestTransactionLog(String s) {

    }

    @Override
    public void onRequestBatchData(String s) {

    }

    @Override
    public void onRequestQposConnected() {
        mQStatePOS.updateState(POSConnectionState.STATE_POS.CONNECTED);
        mPosService.getQposId();
    }

    @Override
    public void onRequestQposDisconnected() {
        Log.i(TAG, "Disconnected False");
        dongleConnect.ondevicedisconnected();
    }

    @Override
    public void onRequestNoQposDetected() {
        dongleConnect.onRequestNoQposDetected();
        Log.i(TAG, "Disconnected False2");
    }

    @Override
    public void onRequestNoQposDetectedUnbond() {
        Log.i(TAG, "Disconnected False3");
    }

    @Override
    public void onError(QPOSService.Error error) {
        this.cancelOperacion();
        if (mDecodeData != null) {
            mPosService.resetQPOS();

            switch (error) {
                case TIMEOUT:
                    onRequestNoQposDetected();
                    break;
                case CMD_TIMEOUT:
                case CMD_NOT_AVAILABLE:
                    dongleConnect.onRequestNoQposDetected();
//                    .onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.TIMEOUT, error.name(), false));
                    break;
                case INPUT_INVALID:
                    dongleConnect.onRequestNoQposDetected();
//                    dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.INPUT_INVALID, error.name(), false));
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
//                    dongleListener.onRespuestaDongle(new PosResult(PosResult.PosTransactionResult.ERROR_DISPOSITIVO, error.name(), false));
                    break;
            }
        }
    }

    @Override
    public void onRequestDisplay(QPOSService.Display display) {

    }

    @Override
    public void onReturnReversalData(String s) {

    }

    @Override
    public void onReturnGetPinResult(Hashtable<String, String> hashtable) {

    }

    @Override
    public void onReturnPowerOnIccResult(boolean b, String s, String s1, int i) {

    }

    @Override
    public void onReturnPowerOffIccResult(boolean b) {

    }

    @Override
    public void onReturnApduResult(boolean b, String s, int i) {

    }

    @Override
    public void onReturnSetSleepTimeResult(boolean b) {

    }

    @Override
    public void onGetCardNoResult(String s) {

    }

    @Override
    public void onRequestSignatureResult(byte[] bytes) {

    }

    @Override
    public void onRequestCalculateMac(String s) {

    }

    @Override
    public void onRequestUpdateWorkKeyResult(QPOSService.UpdateInformationResult updateInformationResult) {

    }

    @Override
    public void onReturnCustomConfigResult(boolean isReturnedConfig, String sfinal) {
        mQposServiceCallback.onReturnCustomConfigResult();
        mPosService.updateEmvAPP(QPOSService.EMVDataOperation.update, mListCapabilities);
    }

    @Override
    public void onRequestSetPin() {

    }

    @Override
    public void onReturnSetMasterKeyResult(boolean b) {

    }

    @Override
    public void onRequestUpdateKey(String s) {

    }

    @Override
    public void onReturnUpdateIPEKResult(boolean b) {

    }

    @Override
    public void onReturnRSAResult(String s) {

    }

    @Override
    public void onReturnUpdateEMVResult(boolean isActualizado) {
        Log.i(TAG, "Injectando Capacidades");

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException exe) {
            Log.e(TAG, "Error al detener el thread");
            Thread.currentThread().interrupt();
        }

        if (isActualizado) {

            mPosService.setCardTradeMode(QPOSService.CardTradeMode.SWIPE_INSERT_CARD);
            Log.i(TAG, "Iniciando Operación");

            mPosService.setOnlineTime(1000);

            mPosService.doCheckCard(30, 10);
        }
    }

    @Override
    public void onReturnGetQuickEmvResult(boolean b) {

    }

    @Override
    public void onReturnGetEMVListResult(String s) {

    }

    @Override
    public void onReturnUpdateEMVRIDResult(boolean b) {

    }

    @Override
    public void onDeviceFound(BluetoothDevice bluetoothDevice) {

    }

    @Override
    public void onReturnBatchSendAPDUResult(LinkedHashMap<Integer, String> linkedHashMap) {

    }

    @Override
    public void onBluetoothBonding() {

    }

    @Override
    public void onBluetoothBonded() {

    }

    @Override
    public void onWaitingforData(String s) {

    }

    @Override
    public void onBluetoothBondFailed() {

    }

    @Override
    public void onBluetoothBondTimeout() {

    }

    @Override
    public void onReturniccCashBack(Hashtable<String, String> hashtable) {

    }

    @Override
    public void onLcdShowCustomDisplay(boolean b) {

    }

    @Override
    public void onUpdatePosFirmwareResult(QPOSService.UpdateInformationResult updateInformationResult) {

    }

    @Override
    public void onBluetoothBoardStateResult(boolean b) {

    }

    @Override
    public void onReturnDownloadRsaPublicKey(HashMap<String, String> hashMap) {

    }

    @Override
    public void onGetPosComm(int i, String s, String s1) {

    }

    @Override
    public void onUpdateMasterKeyResult(boolean b, Hashtable<String, String> hashtable) {

    }

    @Override
    public void onPinKey_TDES_Result(String s) {

    }

    @Override
    public void onEmvICCExceptionData(String s) {

    }

    @Override
    public void onSetParamsResult(boolean b, Hashtable<String, Object> hashtable) {

    }

    @Override
    public void onGetInputAmountResult(boolean b, String s) {

    }

    @Override
    public void onReturnNFCApduResult(boolean b, String s, int i) {

    }

    @Override
    public void onReturnPowerOnNFCResult(boolean b, String s, String s1, int i) {

    }

    @Override
    public void onReturnPowerOffNFCResult(boolean b) {

    }

    @Override
    public void onCbcMacResult(String s) {

    }

    @Override
    public void onReadBusinessCardResult(boolean b, String s) {

    }

    @Override
    public void onWriteBusinessCardResult(boolean b) {

    }

    @Override
    public void onConfirmAmountResult(boolean b) {

    }

    @Override
    public void onSetManagementKey(boolean b) {

    }

    @Override
    public void onSetSleepModeTime(boolean b) {

    }

    @Override
    public void onGetSleepModeTime(String s) {

    }

    @Override
    public void onGetShutDownTime(String s) {

    }

    @Override
    public void onEncryptData(String s) {

    }

    @Override
    public void onAddKey(boolean b) {

    }

    @Override
    public void onSetBuzzerResult(boolean b) {

    }

    @Override
    public void onSetBuzzerTimeResult(boolean b) {

    }

    @Override
    public void onSetBuzzerStatusResult(boolean b) {

    }

    @Override
    public void onGetBuzzerStatusResult(String s) {

    }

    @Override
    public void onQposDoTradeLog(boolean b) {

    }

    @Override
    public void onQposDoGetTradeLogNum(String s) {

    }

    @Override
    public void onQposDoGetTradeLog(String s, String s1) {

    }

    @Override
    public void onRequestDevice() {

    }

    @Override
    public void onGetKeyCheckValue(List<String> list) {

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
        //TODO Usar Libreria De Qpos
        return mPosService.getICCTag(QPOSService.EncryptType.PLAINTEXT, 0, emvLength, sBuilder.toString());
    }
}
