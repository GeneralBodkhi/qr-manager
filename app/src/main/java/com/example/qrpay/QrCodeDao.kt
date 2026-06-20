package com.example.qrpay

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QrCodeDao {

    @Query("SELECT * FROM qr_codes")
    fun getAllCodes(): List<QrCodeEntity>

    @Insert
    fun insertCode(qrCode: QrCodeEntity)

    @Delete
    fun deleteCode(qrCode: QrCodeEntity)
}