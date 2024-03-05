package me.snowlight.paymentpg.config.extension

import org.springframework.http.server.reactive.ServerHttpRequest
import java.util.HashMap


private val mapReqIdToTxId = HashMap<String, String>()

var ServerHttpRequest.txid: String?
    get() {
        return mapReqIdToTxId[this.id]
    }
    set(value) {
        if (value == null) {
            mapReqIdToTxId.remove(id)
        } else {
            mapReqIdToTxId[id] = value
        }
    }