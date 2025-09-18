package com.asafmeizner.wifiaware

import com.getcapacitor.Logger

class WifiAware {
    fun echo(value: String): String {
        Logger.info("Echo", value)
        return value
    }
}
