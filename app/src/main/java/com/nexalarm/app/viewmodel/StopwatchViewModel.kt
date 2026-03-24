package com.nexalarm.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StopwatchViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("stopwatch_state", Context.MODE_PRIVATE)

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _laps = MutableStateFlow<List<Long>>(emptyList())
    val laps: StateFlow<List<Long>> = _laps

    private var tickJob: Job? = null
    private var startTime = 0L
    private var accumulatedTime = 0L

    init {
        val savedAccumulated = prefs.getLong("accumulatedTime", 0L)
        val startedAt = prefs.getLong("startedAt", 0L)
        val wasRunning = prefs.getBoolean("isRunning", false)
        val lapsStr = prefs.getString("laps", "") ?: ""

        accumulatedTime = if (wasRunning && startedAt > 0L) {
            savedAccumulated + (System.currentTimeMillis() - startedAt)
        } else {
            savedAccumulated
        }
        _elapsedMs.value = accumulatedTime

        if (lapsStr.isNotBlank()) {
            _laps.value = lapsStr.split(",").mapNotNull { it.toLongOrNull() }
        }

        if (wasRunning) startTicking()
    }

    fun toggle() {
        if (_isRunning.value) pause() else start()
    }

    private fun start() {
        _isRunning.value = true
        startTime = System.currentTimeMillis()
        prefs.edit()
            .putBoolean("isRunning", true)
            .putLong("accumulatedTime", accumulatedTime)
            .putLong("startedAt", startTime)
            .apply()
        startTicking()
    }

    private fun startTicking() {
        _isRunning.value = true
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
        _elapsedMs.value = accumulatedTime
        tickJob?.cancel()
        prefs.edit()
            .putBoolean("isRunning", false)
            .putLong("accumulatedTime", accumulatedTime)
            .putLong("startedAt", 0L)
            .apply()
    }

    fun lap() {
        if (!_isRunning.value) return
        val lapTime = accumulatedTime + (System.currentTimeMillis() - startTime) - _laps.value.sum()
        _laps.value = listOf(lapTime) + _laps.value
        prefs.edit().putString("laps", _laps.value.joinToString(",")).apply()
    }

    fun reset() {
        _isRunning.value = false
        tickJob?.cancel()
        accumulatedTime = 0L
        startTime = 0L
        _elapsedMs.value = 0L
        _laps.value = emptyList()
        prefs.edit()
            .putBoolean("isRunning", false)
            .putLong("accumulatedTime", 0L)
            .putLong("startedAt", 0L)
            .putString("laps", "")
            .apply()
    }
}
