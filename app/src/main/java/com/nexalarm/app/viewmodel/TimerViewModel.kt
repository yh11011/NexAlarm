package com.nexalarm.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {

    private val _totalSeconds = MutableStateFlow(300)
    val totalSeconds: StateFlow<Int> = _totalSeconds

    private val _remainingSeconds = MutableStateFlow(300)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    private var tickJob: Job? = null

    fun setDuration(seconds: Int) {
        reset()
        _totalSeconds.value = seconds
        _remainingSeconds.value = seconds
    }

    fun toggle() {
        if (_isRunning.value) {
            pause()
        } else {
            start()
        }
    }

    private fun start() {
        if (_remainingSeconds.value <= 0) return
        _isRunning.value = true
        _isFinished.value = false
        tickJob = viewModelScope.launch {
            while (_isRunning.value && _remainingSeconds.value > 0) {
                delay(1000)
                if (_isRunning.value) {
                    _remainingSeconds.value = _remainingSeconds.value - 1
                    if (_remainingSeconds.value <= 0) {
                        _isRunning.value = false
                        _isFinished.value = true
                    }
                }
            }
        }
    }

    private fun pause() {
        _isRunning.value = false
        tickJob?.cancel()
    }

    fun reset() {
        _isRunning.value = false
        _isFinished.value = false
        tickJob?.cancel()
        _remainingSeconds.value = _totalSeconds.value
    }

    fun addOneMinute() {
        _totalSeconds.value += 60
        _remainingSeconds.value += 60
    }
}

