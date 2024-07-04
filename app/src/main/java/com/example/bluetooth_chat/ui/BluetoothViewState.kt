package com.example.bluetooth_chat.ui

import com.example.bluetooth_chat.domain.chat.BluetoothDevice

data class BluetoothViewState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String ?= null
)
