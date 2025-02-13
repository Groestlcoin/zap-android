package zapsolutions.zap.util;

import androidx.annotation.NonNull;

public class InvoiceUtil {

    public static String URI_PREFIX_LIGHTNING = "lightning:";
    public static String URI_PREFIX_BITCOIN = "groestlcoin:";
    public static String INVOICE_PREFIX_LIGHTNING_TESTNET = "lntgrs";
    public static String INVOICE_PREFIX_LIGHTNING_MAINNET = "lngrs";
    private static int INVOICE_LIGHTNING_MIN_LENGTH = 4;

    public static String generateLightningUri(@NonNull String data) {
        if (isLightningUri(data)) {
            return data;
        }

        return URI_PREFIX_LIGHTNING + data;
    }

    public static String generateBitcoinUri(@NonNull String data) {
        if (isBitcoinUri(data)) {
            return data;
        }

        return URI_PREFIX_BITCOIN + data;
    }

    public static boolean isLightningUri(@NonNull String data) {
        return hasPrefix(URI_PREFIX_LIGHTNING, data);
    }

    public static boolean isBitcoinUri(@NonNull String data) {
        return hasPrefix(URI_PREFIX_BITCOIN, data);
    }

    public static boolean isLightningInvoice(@NonNull String data) {
        if (data.isEmpty() || data.length() < INVOICE_LIGHTNING_MIN_LENGTH) {
            return false;
        }

        return hasPrefix(INVOICE_PREFIX_LIGHTNING_MAINNET, data) || hasPrefix(INVOICE_PREFIX_LIGHTNING_TESTNET, data);
    }

    private static boolean hasPrefix(@NonNull String prefix, @NonNull String data) {
        if (data.isEmpty() || data.length() < prefix.length()) {
            return false;
        }

        return data.substring(0, prefix.length()).equalsIgnoreCase(prefix);
    }
}
