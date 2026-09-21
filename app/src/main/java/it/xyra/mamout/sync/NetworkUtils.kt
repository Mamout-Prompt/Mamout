package it.xyra.mamout.sync

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    /**
     * Retrieves the local IP address of the device on the Wi-Fi or LAN network.
     *
     * @return The IPv4 address as a string, or null if not found.
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}
