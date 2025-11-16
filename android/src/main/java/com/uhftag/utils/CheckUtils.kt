package com.uhftag.utils

import com.rscja.deviceapi.entity.UHFTAGInfo

object CheckUtils {
    
    /**
     * Get the insert index for a new tag in a sorted list
     * Uses binary search for efficient insertion
     * 
     * @param listData The existing sorted list of tags
     * @param newInfo The new tag to insert
     * @param exists Output parameter indicating if tag already exists
     * @return The index where the tag should be inserted
     */
    fun getInsertIndex(
        listData: List<UHFTAGInfo>,
        newInfo: UHFTAGInfo,
        exists: BooleanArray
    ): Int {
        if (listData.isEmpty()) {
            exists[0] = false
            return 0
        }

        var startIndex = 0
        var endIndex = listData.size - 1
        var judgeIndex: Int
        var compareResult: Int

        while (true) {
            judgeIndex = (startIndex + endIndex) / 2
            compareResult = compareBytes(newInfo.epcBytes, listData[judgeIndex].epcBytes)

            when {
                compareResult > 0 -> {
                    if (judgeIndex == endIndex) {
                        exists[0] = false
                        return judgeIndex + 1
                    }
                    startIndex = judgeIndex + 1
                }
                compareResult < 0 -> {
                    if (judgeIndex == startIndex) {
                        exists[0] = false
                        return judgeIndex
                    }
                    endIndex = judgeIndex - 1
                }
                else -> {
                    exists[0] = true
                    return judgeIndex
                }
            }
        }
    }

    /**
     * Compare two byte arrays
     * 
     * @return 1 or 2 if b1 > b2
     *         -1 or -2 if b1 < b2
     *         0 if b1 == b2
     */
    private fun compareBytes(b1: ByteArray, b2: ByteArray): Int {
        val minLength = minOf(b1.size, b2.size)

        for (i in 0 until minLength) {
            val value1 = b1[i].toInt() and 0xFF
            val value2 = b2[i].toInt() and 0xFF

            when {
                value1 > value2 -> return 1
                value1 < value2 -> return -1
            }
        }

        return when {
            b1.size > b2.size -> 2
            b1.size < b2.size -> -2
            else -> 0
        }
    }
}
