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

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("timer_state", Context.MODE_PRIVATE)

    // 從持久化狀態還原（若上次正在執行，則計算經過時間）
    private val _totalSeconds = MutableStateFlow(prefs.getInt("totalSec", 300))
    val totalSeconds: StateFlow<Int> = _totalSeconds

    private val _remainingSeconds = MutableStateFlow(run {
        val savedRemaining = prefs.getInt("remainingSec", 300)
        val startedAt = prefs.getLong("startedAt", 0L)
        val wasRunning = prefs.getBoolean("isRunning", false)
        if (wasRunning && startedAt > 0L) {
            val elapsed = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
            (savedRemaining - elapsed).coerceAtLeast(0)
        } else {
            savedRemaining
        }
    })
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    private var tickJob: Job? = null

    init {
        val wasRunning = prefs.getBoolean("isRunning", false)
        if (wasRunning && _remainingSeconds.value > 0) {
            startTicking()
        } else if (_remainingSeconds.value <= 0 && wasRunning) {
            _isFinished.value = true
            prefs.edit().putBoolean("isRunning", false).apply()
        }
    }

    fun setDuration(seconds: Int) {
        tickJob?.cancel()
        _isRunning.value = false
        _isFinished.value = false
        _totalSeconds.value = seconds
        _remainingSeconds.value = seconds
        prefs.edit()
            .putInt("totalSec", seconds)
            .putInt("remainingSec", seconds)
            .putBoolean("isRunning", false)
            .putLong("startedAt", 0L)
            .apply()
    }

    fun toggle() {
        if (_isRunning.value) pause() else start()
    }

    private fun start() {
        if (_remainingSeconds.value <= 0) return
        _isFinished.value = false
        prefs.edit()
            .putBoolean("isRunning", true)
            .putInt("remainingSec", _remainingSeconds.value)
            .putLong("startedAt", System.currentTimeMillis())
            .apply()
        startTicking()
    }

    private fun startTicking() {
        _isRunning.value = true
        tickJob = viewModelScope.launch {
            while (_isRunning.value && _remainingSeconds.value > 0) {
                delay(1000)
                if (_isRunning.value) {
                    _remainingSeconds.value = _remainingSeconds.value - 1
                    if (_remainingSeconds.value <= 0) {
                        _isRunning.value = false
                        _isFinished.value = true
                        prefs.edit().putBoolean("isRunning", false).putInt("remainingSec", 0).apply()
                    }
                }
            }
        }
    }

    private fun pause() {
        _isRunning.value = false
        tickJob?.cancel()
        prefs.edit()
            .putBoolean("isRunning", false)
            .putInt("remainingSec", _remainingSeconds.value)
            .putLong("startedAt", 0L)
            .apply()
    }

    fun reset() {
        _isRunning.value = false
        _isFinished.value = false
        tickJob?.cancel()
        _remainingSeconds.value = _totalSeconds.value
        prefs.edit()
            .putBoolean("isRunning", false)
            .putInt("remainingSec", _totalSeconds.value)
            .putLong("startedAt", 0L)
            .apply()
    }

    fun addOneMinute() {
        _totalSeconds.value += 60
        _remainingSeconds.value += 60
        if (_isRunning.value) {
            // 更新 startedAt 使下次重啟時計算正確
            prefs.edit()
                .putInt("remainingSec", _remainingSeconds.value)
                .putLong("startedAt", System.currentTimeMillis())
                .apply()
        } else {
            prefs.edit()
                .putInt("totalSec", _totalSeconds.value)
                .putInt("remainingSec", _remainingSeconds.value)
                .apply()
        }
    }
}
