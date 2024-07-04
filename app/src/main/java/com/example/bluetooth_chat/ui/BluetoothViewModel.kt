package com.example.bluetooth_chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth_chat.domain.chat.BluetoothController
import com.example.bluetooth_chat.domain.chat.BluetoothDeviceDomain
import com.example.bluetooth_chat.domain.chat.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val _viewState = MutableStateFlow(BluetoothViewState())
    val viewState = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _viewState
    ) { scannedDevices, pairedDevices, viewState ->
        viewState.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _viewState.value)

    private var deviceConnectionJob: Job? = null

    init {
        bluetoothController.isConnected.onEach { isConnected ->
            _viewState.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.error.onEach { error ->
            _viewState.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        _viewState.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .connectToDevice(device)
            .listen()
    }

    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _viewState.update {
            it.copy(
                isConnecting = false,
                isConnected = false
            )
        }
    }

    fun waitForIncomingConnections() {
        _viewState.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .startBluetoothServer()
            .listen()
    }

    fun startScan() = bluetoothController.startDiscovery()

    fun stopScan() = bluetoothController.stopDiscovery()

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                ConnectionResult.ConnectionEstablished -> {
                    _viewState.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false,
                            errorMessage = null
                        )
                    }
                }

                is ConnectionResult.Error -> {
                    _viewState.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }.catch { throwable ->
            bluetoothController.closeConnection()
            _viewState.update {
                it.copy(
                    isConnected = false,
                    isConnecting = false,
                    errorMessage = throwable.message
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}