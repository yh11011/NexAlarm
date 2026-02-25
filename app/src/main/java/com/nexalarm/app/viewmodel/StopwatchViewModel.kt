package com.nexalarm.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StopwatchViewModel : ViewModel() {

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _laps = MutableStateFlow<List<Long>>(emptyList())
    val laps: StateFlow<List<Long>> = _laps

    private var tickJob: Job? = null
    private var startTime = 0L
    private var accumulatedTime = 0L
    private var lapStartTime = 0L

    fun toggle() {
        if (_isRunning.value) {
            pause()
        } else {
            start()
        }
    }

    private fun start() {
        _isRunning.value = true
        startTime = System.currentTimeMillis()
        if (lapStartTime == 0L) lapStartTime = startTime
        tickJob = viewModelScope.launch {
            while (_isRunning.value) {
                _elapsedMs.value = accumulatedTime + (System.currentTimeMillis() - startTime)
                delay(33)
            }
        }
    }

    private fun pause() {
        _isRunning.value = false
        accumulatedTime += System.currentTimeMillis() - startTime
        tickJob?.cancel()
    }

    fun lap() {
        if (!_isRunning.value) return
        val now = System.currentTimeMillis()
        val lapTime = accumulatedTime + (now - startTime) - _laps.value.sum()
        _laps.value = listOf(lapTime) + _laps.value
    }

    fun reset() {
        _isRunning.value = false
        tickJob?.cancel()
        accumulatedTime = 0L
        _elapsedMs.value = 0L
        _laps.value = emptyList()
        lapStartTime = 0L
    }
}

