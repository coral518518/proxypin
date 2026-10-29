package com.network.proxy.vpn

import android.os.ParcelFileDescriptor
import android.util.Log
import com.github.shadowsocks.bg.Tun2proxy

/**
 * VPN 线程，负责调用 tun2proxy 原生内核接管 TUN 虚拟网卡流量并转发到本地代理
 */
class ProxyVpnThread(
    private val vpnInterface: ParcelFileDescriptor,
    private val proxyHost: String,
    private val proxyPort: Int,
    private val proxyPassDomains: ArrayList<String>? = null,
) : Thread("Tun2proxy thread") {

    companion object {
        const val TAG = "ProxyVpnThread"
        const val VIRTUAL_DNS_POOL = "198.18.0.0/15"
        const val TUN_MTU: Char = 1500.toChar()
    }

    @Volatile
    private var running = false

    override fun run() {
        Log.i(TAG, "Starting tun2proxy thread with proxyHost=$proxyHost, proxyPort=$proxyPort")
        running = true

        val fd = vpnInterface.fd
        // tun2proxy 参数说明：
        // 1. argv[0]: 程序名称 (tun2proxy)
        // 2. --tun-fd: 虚拟网卡文件描述符
        // 3. --proxy: 上游代理地址 (使用 SOCKS5 协议连接本地 127.0.0.1:proxyPort)
        // 4. --dns: virtual (虚拟 DNS 分配 198.18.x.x 虚拟 IP，零延迟响应，保持真实域名映射)
        // 5. --virtual-dns-pool: 虚拟 IP 池 (198.18.0.0/15)
        // 6. --verbosity: 日志级别 (info)
        // 7. --ipv6-enabled: 支持 IPv6 避免双栈网络下因无 IPv6 路由导致断网或延迟
        val args = StringBuilder("tun2proxy")
        args.append(" --tun-fd ").append(fd)
        args.append(" --proxy socks5://").append(proxyHost).append(":").append(proxyPort)
        args.append(" --dns virtual")
        args.append(" --virtual-dns-pool ").append(VIRTUAL_DNS_POOL)
        args.append(" --ipv6-enabled")
        args.append(" --verbosity info")

        // 直连绕过名单（针对 IP 或 CIDR 网段）
        proxyPassDomains?.forEach { domain ->
            val trimmed = domain.trim()
            if (trimmed.isNotEmpty()) {
                if (trimmed.contains("/") || isIpv4Address(trimmed)) {
                    args.append(" --bypass ").append(trimmed)
                }
            }
        }

        val cliArgs = args.toString()
        Log.i(TAG, "Invoking Tun2proxy.run with: $cliArgs")

        try {
            val result = Tun2proxy.run(cliArgs, TUN_MTU)
            Log.i(TAG, "Tun2proxy.run returned with code: $result")
        } catch (e: Throwable) {
            Log.e(TAG, "Tun2proxy runtime exception", e)
        } finally {
            running = false
        }
    }

    private fun isIpv4Address(text: String): Boolean {
        val parts = text.split(".")
        if (parts.size != 4) return false
        return parts.all { it.toIntOrNull()?.let { num -> num in 0..255 } == true }
    }

    @Synchronized
    fun stopThread() {
        if (running) {
            Log.i(TAG, "Stopping tun2proxy...")
            try {
                Tun2proxy.stop()
            } catch (e: Throwable) {
                Log.e(TAG, "Error calling Tun2proxy.stop", e)
            }
            running = false
            interrupt()
        }
    }
}
