package com.spark.edtech.data.source

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
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
            val redeemCode = snapshot.getValue(RedeemCode::class.java)?.copy(code = code)
            Log.d(TAG, "Redeem code $code: $redeemCode")
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

    suspend fun saveUser(user: User) {
        usersRef.child(user.uid).setValue(user).await()
    }

    suspend fun createUserWithEmail(email: String, password: String): String? {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid
    }

    suspend fun signInWithEmail(email: String, password: String): String? {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.uid
    }

    suspend fun getUserById(id: String): User? {
        val snapshot = usersRef.child(id).get().await()
        return snapshot.getValue(User::class.java)
    }
}