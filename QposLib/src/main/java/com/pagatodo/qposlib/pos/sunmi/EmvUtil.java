package com.pagatodo.qposlib.pos.sunmi;

import com.pagatodo.qposlib.PosInstance;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.bean.AidV2;
import com.sunmi.pay.hardware.aidlv2.bean.CapkV2;
import com.sunmi.pay.hardware.aidlv2.bean.EmvTermParamV2;
import com.sunmi.pay.hardware.aidlv2.emv.EMVOptV2;
import com.sunmi.pay.hardware.aidlv2.security.SecurityOptV2;

import java.util.HashMap;
import java.util.Map;

public final class EmvUtil {

    private static final String TAG = EmvUtil.class.getSimpleName();

//    private static AbstractDongle abstractDongle = MposApplication.getInstance().getPreferedDongle();

    private EmvUtil() {
    }

    /**
     * Get configuration by country
     */
    public static Map<String, String> getConfig(Map<String, String> capabilities) {
        Map<String, String> map = new HashMap<>();
//        map.put("countryCode", capabilities.get(COUNTRY_CODE));//country code(国家代码)
//        map.put("capability", capabilities.get(TERMINAL_CAPS));//capability(终端性能)
//        map.put("5F2A", capabilities.get(CURRENCY_CODE));//transaction currency code(交易货币代码)
//        map.put("DF21", capabilities.get(CVM_LIMIT));//transaction currency code exponent(交易货币代码指数)
//        map.put("9F40", capabilities.get(ADITIONAL_CAPS));
        return map;
    }

    /**
     * Initialize keys
     */
    public static void initKey(SecurityOptV2 mSecurityOptV2) {
        try {

            int result = mSecurityOptV2.savePlaintextKey(AidlConstantsV2.Security.KEY_TYPE_TDK,
                    PosInstance.getInstance().getSessionKeys().get("plainDataKey").getBytes(),  PosInstance.getInstance().getSessionKeys().get("plainDataKcvKey").getBytes(),
                    AidlConstantsV2.Security.KEY_ALG_TYPE_3DES, 10);
//            AppLogger.LOGGER.fine(TAG, "save KEK result:" + result);
            if (result != 0) {
//                AppLogger.LOGGER.fine(TAG, "save KEK fail");
                return;
            }

            result = mSecurityOptV2.savePlaintextKey(AidlConstantsV2.Security.KEY_TYPE_PIK, PosInstance.getInstance().getSessionKeys().get("plainPinkey").getBytes(), PosInstance.getInstance().getSessionKeys().get("plainPinKcvkey").getBytes(),
                    AidlConstantsV2.Security.KEY_ALG_TYPE_3DES, 11);
//            AppLogger.LOGGER.fine(TAG, "save KEK result:" + result);
            if (result != 0) {
//                AppLogger.LOGGER.fine(TAG, "save KEK fail");
                return;
            }

//            AppLogger.LOGGER.fine(TAG, "init  key success");
        } catch (Exception e) {
            e.printStackTrace();
//            AppLogger.LOGGER.fine(TAG, "init key fail");
        }
    }

    /**
     * Initialize AIDs and Capks
     */
    public static void initAidAndRid(EMVOptV2 emvOptV2) {
        try {
            //Normal Capks
            CapkV2 capkV2 = EmvUtil.hexStr2Rid("9F0605A0000003339F220104DF05083230323531323331DF060101DF070101DF0281F8BC853E6B5365E89E7EE9317C94B02D0ABB0DBD91C05A224A2554AA29ED9FCB9D86EB9CCBB322A57811F86188AAC7351C72BD9EF196C5A01ACEF7A4EB0D2AD63D9E6AC2E7836547CB1595C68BCBAFD0F6728760F3A7CA7B97301B7E0220184EFC4F653008D93CE098C0D93B45201096D1ADFF4CF1F9FC02AF759DA27CD6DFD6D789B099F16F378B6100334E63F3D35F3251A5EC78693731F5233519CDB380F5AB8C0F02728E91D469ABD0EAE0D93B1CC66CE127B29C7D77441A49D09FCA5D6D9762FC74C31BB506C8BAE3C79AD6C2578775B95956B5370D1D0519E37906B384736233251E8F09AD79DFBE2C6ABFADAC8E4D8624318C27DAF1DF040103DF0314F527081CF371DD7E1FD4FA414A665036E0F5E6E5");
            emvOptV2.addCapk(capkV2);
            capkV2 = EmvUtil.hexStr2Rid("9F0605A0000003339F220103DF05083230323431323331DF060101DF070101DF0281B0B0627DEE87864F9C18C13B9A1F025448BF13C58380C91F4CEBA9F9BCB214FF8414E9B59D6ABA10F941C7331768F47B2127907D857FA39AAF8CE02045DD01619D689EE731C551159BE7EB2D51A372FF56B556E5CB2FDE36E23073A44CA215D6C26CA68847B388E39520E0026E62294B557D6470440CA0AEFC9438C923AEC9B2098D6D3A1AF5E8B1DE36F4B53040109D89B77CAFAF70C26C601ABDF59EEC0FDC8A99089140CD2E817E335175B03B7AA33DDF040103DF031487F0CD7C0E86F38F89A66F8C47071A8B88586F26");
            emvOptV2.addCapk(capkV2);
            capkV2 = EmvUtil.hexStr2Rid("9F0605A0000003339F220102DF05083230323131323331DF060101DF070101DF028190A3767ABD1B6AA69D7F3FBF28C092DE9ED1E658BA5F0909AF7A1CCD907373B7210FDEB16287BA8E78E1529F443976FD27F991EC67D95E5F4E96B127CAB2396A94D6E45CDA44CA4C4867570D6B07542F8D4BF9FF97975DB9891515E66F525D2B3CBEB6D662BFB6C3F338E93B02142BFC44173A3764C56AADD202075B26DC2F9F7D7AE74BD7D00FD05EE430032663D27A57DF040103DF031403BB335A8549A03B87AB089D006F60852E4B8060");
            emvOptV2.addCapk(capkV2);
            capkV2 = EmvUtil.hexStr2Rid("9F0605A0000003339F220101DF05083230313431323331DF060101DF070101DF028180BBE9066D2517511D239C7BFA77884144AE20C7372F515147E8CE6537C54C0A6A4D45F8CA4D290870CDA59F1344EF71D17D3F35D92F3F06778D0D511EC2A7DC4FFEADF4FB1253CE37A7B2B5A3741227BEF72524DA7A2B7B1CB426BEE27BC513B0CB11AB99BC1BC61DF5AC6CC4D831D0848788CD74F6D543AD37C5A2B4C5D5A93BDF040103DF0314E881E390675D44C2DD81234DCE29C3F5AB2297A0");
            emvOptV2.addCapk(capkV2);
        } catch (Exception e) {
            e.printStackTrace();
//            AppLogger.LOGGER.fine(TAG, "initAIDAndRid fail");
        }
    }

    /**
     * Set EMV terminal parameters
     */
    public static void setTerminalParam(Map<String, String> map, EMVOptV2 emvOptV2) {
        try {
            EmvTermParamV2 emvTermParam = new EmvTermParamV2();
            emvTermParam.countryCode = map.get("countryCode");
            emvTermParam.capability = map.get("capability");
            int result = emvOptV2.setTerminalParam(emvTermParam);
//            AppLogger.LOGGER.fine(TAG, "setTerminalParam result:" + result);
        } catch (Exception e) {
            e.printStackTrace();
//            AppLogger.LOGGER.fine(TAG, "setTerminalParam fail");
        }
    }

    /**
     * Convert hex string AID to AidV2
     */
    public static AidV2 hexStr2Aid(String hexStr) {
        AidV2 aidV2 = new AidV2();
        Map<String, TLV> map = TLVUtil.buildTLVMap(hexStr);
        TLV tlv = map.get("DF21");
        if (tlv != null) {
            aidV2.cvmLmt = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF20");
        if (tlv != null) {
            aidV2.termClssLmt = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF19");
        if (tlv != null) {
            aidV2.termClssOfflineFloorLmt = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("9F7B");
        if (tlv != null) {
            aidV2.termOfflineFloorLmt = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("9F06");
        if (tlv != null) {
            aidV2.aid = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF01");
        if (tlv != null) {
            aidV2.selFlag = ByteUtil.hexStr2Byte(tlv.getValue());
        }
        tlv = map.get("DF17");
        if (tlv != null) {
            aidV2.targetPer = ByteUtil.hexStr2Byte(tlv.getValue());
        }
        tlv = map.get("DF16");
        if (tlv != null) {
            aidV2.maxTargetPer = ByteUtil.hexStr2Byte(tlv.getValue());
        }
        tlv = map.get("9F1B");
        if (tlv != null) {
            aidV2.floorLimit = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF15");
        if (tlv != null) {
            aidV2.threshold = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF13");
        if (tlv != null) {
            aidV2.TACDenial = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF12");
        if (tlv != null) {
            aidV2.TACOnline = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF11");
        if (tlv != null) {
            aidV2.TACDefault = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("9F01");
        if (tlv != null) {
            aidV2.AcquierId = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF14");
        if (tlv != null) {
            aidV2.dDOL = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("9F09");
        if (tlv != null) {
            aidV2.version = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("9F4E");
        if (tlv != null) {
            aidV2.merchName = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("9F15");
        if (tlv != null) {
            aidV2.merchCateCode = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("9F16");
        if (tlv != null) {
            aidV2.merchId = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("9F3C");
        if (tlv != null) {
            aidV2.referCurrCode = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("9F3D");
        if (tlv != null) {
            aidV2.referCurrExp = ByteUtil.hexStr2Byte(tlv.getValue());
        }
        return aidV2;
    }

    /**
     * Convert hex string Capk to CapkV2
     */
    public static CapkV2 hexStr2Rid(String hexStr) {
        CapkV2 capkV2 = new CapkV2();
        Map<String, TLV> map = TLVUtil.buildTLVMap(hexStr);
        TLV tlv = map.get("9F06");
        if (tlv != null) {
            capkV2.rid = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("9F22");
        if (tlv != null) {
            capkV2.index = ByteUtil.hexStr2Byte(tlv.getValue());
        }
        tlv = map.get("DF06");
        if (tlv != null) {
            capkV2.hashInd = ByteUtil.hexStr2Byte(tlv.getValue());
        }
        tlv = map.get("DF07");
        if (tlv != null) {
            capkV2.arithInd = ByteUtil.hexStr2Byte(tlv.getValue());
        }
        tlv = map.get("DF02");
        if (tlv != null) {
            capkV2.modul = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF04");
        if (tlv != null) {
            capkV2.exponent = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF05");
        if (tlv != null) {
            capkV2.expDate = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        tlv = map.get("DF03");
        if (tlv != null) {
            capkV2.checkSum = ByteUtil.hexStr2Bytes(tlv.getValue());
        }
        return capkV2;
    }

}
