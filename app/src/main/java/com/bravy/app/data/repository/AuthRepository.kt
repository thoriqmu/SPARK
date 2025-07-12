package com.bravy.app.data.repository

import android.net.Uri
import com.bravy.app.data.model.RedeemCode
import com.bravy.app.data.model.User
import java.io.File

interface AuthRepository {
    suspend fun validateRedeemCode(code: String): Result<RedeemCode>
    suspend fun markRedeemCodeAsUsed(code: String): Result<Unit>
    suspend fun registerUser(user: User): Result<Unit>
    suspend fun createUserWithEmail(email: String, password: String): Result<String>
    suspend fun loginUser(email: String, password: String): Result<String>
    suspend fun getUser(uid: String): Result<User>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun uploadProfilePicture(uid: String, imageFile: File): Result<String>
}