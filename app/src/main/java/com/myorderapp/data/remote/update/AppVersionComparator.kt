package com.myorderapp.data.remote.update

object AppVersionComparator {
    fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = versionParts(remote)
        val currentParts = versionParts(current)
        val length = maxOf(remoteParts.size, currentParts.size)
        for (index in 0 until length) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (remotePart != currentPart) return remotePart > currentPart
        }
        return false
    }

    private fun versionParts(version: String): List<Int> = version
        .removePrefix("v")
        .split(".")
        .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
