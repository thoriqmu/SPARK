package com.spark.edtech.data.source

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.spark.edtech.data.model.RedeemCode
import com.spark.edtech.data.model.User
import com.spark.edtech.util.Constants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    private val redeemCodesRef = database.getReference(Constants.REDEEM_CODES_PATH)
    private val usersRef = database.getReference(Constants.USERS_PATH)
    private val TAG = "FirebaseDataSource"

    suspend fun validateRedeemCode(code: String): RedeemCode? {
        return try {
            val snapshot = redeemCodesRef.child(code).get().await()
            Log.d(TAG, "Raw snapshot for redeem code $code: ${snapshot.value}")
            if (!snapshot.exists()) {
                Log.e(TAG, "Redeem code $code does not exist")
                return null
            }
            val isUsed = snapshot.child("isUsed").getValue(Boolean::class.java) ?: false
            val createdAt = snapshot.child("createdAt").getValue(String::class.java) ?: ""
            val redeemCode = RedeemCode(code = code, isUsed = isUsed, createdAt = createdAt)
            Log.d(TAG, "Deserialized redeem code $code: $redeemCode")
            redeemCode
        } catch (e: Exception) {
            Log.e(TAG, "Error validating redeem code $code: ${e.message}")
            null
        }
    }

    suspend fun markRedeemCodeAsUsed(code: String) {
        try {
            redeemCodesRef.child(code).child("isUsed").setValue(true).await()
            Log.d(TAG, "Marked redeem code $code as used")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking redeem code $code as used: ${e.message}")
        }
    }

    suspend fun registerUser(user: User) {
        try {
            usersRef.child(user.uid).setValue(user).await()
            Log.d(TAG, "Registered user ${user.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user: ${e.message}")
            throw e
        }
    }

    suspend fun createUserWithEmail(email: String, password: String): String? {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Log.d(TAG, "Created user with email $email, uid: ${result.user?.uid}")
            return result.user?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user with email $email: ${e.message}")
            throw e
        }
    }

    suspend fun loginUser(email: String, password: String): String? {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Log.d(TAG, "Logged in user with email $email, uid: ${result.user?.uid}")
            return result.user?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in user with email $email: ${e.message}")
            throw e
        }
    }

    suspend fun getUser(uid: String): User? {
        try {
            val snapshot = usersRef.child(uid).get().await()
            val user = snapshot.getValue(User::class.java)
            Log.d(TAG, "Fetched user $uid: $user")
            return user
        } catch (exception: Exception) {
            Log.e(TAG, "Error fetching user $uid: ${exception.message}")
            return null
        }
    }

    suspend fun updateUser(user: User) {
        try {
            usersRef.child(user.uid).setValue(user).await()
            Log.d(TAG, "Updated user ${user.uid}")
        } catch (exception: Exception) {
            Log.e(TAG, "Error updating user ${user.uid}: ${exception.message}")
            throw exception
        }
    }
}