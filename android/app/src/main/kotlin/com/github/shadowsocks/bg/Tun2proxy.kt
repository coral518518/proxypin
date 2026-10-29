package com.github.shadowsocks.bg

import android.util.Log

/**
 * JNI wrapper for tun2proxy (Reqable / tun2proxy Rust native library).
 */
object Tun2proxy {
    private const val TAG = "Tun2proxy"

    init {
        try {
            System.loadLibrary("tun2proxy")
            Log.i(TAG, "libtun2proxy.so loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load libtun2proxy.so", e)
        }
    }

    /**
     * Run tun2proxy with CLI arguments.
     * This is a blocking call that runs until [stop] is invoked.
     *
     * @param cliArgs CLI arguments, e.g. "tun2proxy --tun-fd 10 --proxy socks5://127.0.0.1:9099 --dns virtual"
     * @param tunMtu The MTU of the TUN device, e.g. 1500.toChar()
     * @return Exit code
     */
    @JvmStatic
    external fun run(cliArgs: String, tunMtu: Char): Int

    /**
     * Stop tun2proxy loop gracefully.
     *
     * @return 0 on success, negative on error
     */
    @JvmStatic
    external fun stop(): Int
}
