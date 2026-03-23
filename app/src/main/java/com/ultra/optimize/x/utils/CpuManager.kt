package com.ultra.optimize.x.utils

import java.io.RandomAccessFile

object CpuManager {

    fun getCpuUsage(): Int {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            var load = reader.readLine()
            var toks = load.split(" +".toRegex())
            val idle1 = toks[4].toLong()
            val cpu1 = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() + toks[6].toLong() + toks[7].toLong() + toks[8].toLong()
            
            Thread.sleep(360)
            
            reader.seek(0)
            load = reader.readLine()
            reader.close()
            toks = load.split(" +".toRegex())
            val idle2 = toks[4].toLong()
            val cpu2 = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() + toks[6].toLong() + toks[7].toLong() + toks[8].toLong()
            
            ((cpu2 - cpu1).toDouble() / ((cpu2 + idle2) - (cpu1 + idle1)).toDouble() * 100).toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun setGovernor(governor: String) {
        val cores = Runtime.getRuntime().availableProcessors()
        for (i in 0 until cores) {
            RootManager.runCommand("echo $governor > /sys/devices/system/cpu/cpu$i/cpufreq/scaling_governor")
        }
    }

    fun getAvailableGovernors(): List<String> {
        val output = RootManager.runCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors")
        return output.trim().split(" ")
    }
}
