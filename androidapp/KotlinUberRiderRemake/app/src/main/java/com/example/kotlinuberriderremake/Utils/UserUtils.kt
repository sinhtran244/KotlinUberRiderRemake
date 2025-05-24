package com.example.kotlinuberriderremake.Utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.example.kotlinuberriderremake.Common.Common
import com.example.kotlinuberriderremake.Model.TokenModel

import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


object UserUtils{
    fun updateToken( context: Context, token: String) {
        val tokenModel = TokenModel()
        tokenModel.token = token;

        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(tokenModel)
            .addOnFailureListener{e -> Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()}
            .addOnSuccessListener{}
    }
}