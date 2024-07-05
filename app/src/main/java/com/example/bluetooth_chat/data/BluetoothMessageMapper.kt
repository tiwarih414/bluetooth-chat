package com.example.bluetooth_chat.data

import com.example.bluetooth_chat.domain.chat.BluetoothMessage

fun String.toBluetoothMessage(isFromLocalUser: Boolean): BluetoothMessage {
    val name = substringBeforeLast("#")
    val message = substringAfter("#")
    return BluetoothMessage(
        senderName = name,
        message = message,
        isFromLocalUser = isFromLocalUser
    )
}
fun BluetoothMessage.toByteArray(): ByteArray{
    return "$senderName#$message".encodeToByteArray()
}