package com.spark.edtech.data.repository

import android.util.Log
import com.spark.edtech.data.model.RedeemCode
import com.spark.edtech.data.model.User
import com.spark.edtech.data.source.FirebaseDataSource
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : AuthRepository {
    private val TAG = "AuthRepositoryImpl"

    override suspend fun validateRedeemCode(code: String): Result<RedeemCode> {
        return try {
            val redeemCode = dataSource.validateRedeemCode(code)
            Log.d(TAG, "Validating redeem code $code: $redeemCode")
            when {
                redeemCode == null -> {
                    Log.e(TAG, "Invalid redeem code: $code")
                    Result.failure(Exception("Invalid redeem code"))
                }
                redeemCode.isUsed -> {
                    Log.e(TAG, "Redeem code $code has been used")
                    Result.failure(Exception("Redeem code has been used"))
                }
                else -> {
                    Log.d(TAG, "Redeem code $code is valid")
                    Result.success(redeemCode)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating redeem code $code: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun markRedeemCodeAsUsed(code: String): Result<Unit> {
        return try {
            dataSource.markRedeemCodeAsUsed(code)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerUser(user: User): Result<Unit> {
        return try {
            dataSource.saveUser(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createUserWithEmail(email: String, password: String): Result<String> {
        return try {
            val uid = dataSource.createUserWithEmail(email, password)
            if (uid != null) {
                Result.success(uid)
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginUser(email: String, password: String): Result<String> {
        return try {
            val uid = dataSource.signInWithEmail(email, password)
            if (uid != null) {
                Result.success(uid)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUser(uid: String): Result<User> {
        return try {
            val user = dataSource.getUserById(uid)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}