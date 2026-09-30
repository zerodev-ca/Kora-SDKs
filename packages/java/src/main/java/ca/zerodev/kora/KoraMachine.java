package ca.zerodev.kora;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;

public class KoraMachine {
    public String mac() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }
                byte[] address = ni.getHardwareAddress();
                if (address != null && address.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < address.length; i++) {
                        sb.append(String.format("%02X%s", address[i], (i < address.length - 1) ? ":" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return "00:00:00:00:00:00";
    }

    public String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "localhost";
        }
    }

    public String hwid() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(System.getProperty("os.name", "").getBytes(StandardCharsets.UTF_8));
            digest.update(System.getProperty("os.arch", "").getBytes(StandardCharsets.UTF_8));
            digest.update(this.hostname().getBytes(StandardCharsets.UTF_8));
            digest.update(this.mac().getBytes(StandardCharsets.UTF_8));
            digest.update(String.valueOf(Runtime.getRuntime().availableProcessors()).getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ignored) {
            return "generic-hwid";
        }
    }
}
