package com.example.expensestracker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.expensestracker.data.LockManager

class AppLockActivity : AppCompatActivity() {

    private lateinit var lockManager: LockManager
    private var isSetupMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_lock)

        lockManager = LockManager(this)

        isSetupMode = intent.getBooleanExtra("isSetupMode", false)

        val tvTitle = findViewById<TextView>(R.id.tvLockTitle)
        val tvSub = findViewById<TextView>(R.id.tvLockSub)
        val etPin = findViewById<EditText>(R.id.etLockPin)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)

        if (isSetupMode) {
            tvTitle.text = "Set Up PIN Lock 🔐"
            tvSub.text = "Create a 4-digit PIN to secure your app"
            btnUnlock.text = "Save PIN"
        } else {
            tvTitle.text = "App Locked 🔐"
            tvSub.text = "Enter 4-digit PIN to access your financial data"
            btnUnlock.text = "Unlock App"
        }

        btnUnlock.setOnClickListener {
            val pin = etPin.text.toString().trim()
            if (pin.length != 4) {
                etPin.error = "PIN must be 4 digits"
                return@setOnClickListener
            }

            if (isSetupMode) {
                lockManager.setPin(pin)
                lockManager.setSessionUnlocked(true)
                Toast.makeText(this, "PIN Lock enabled!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                if (lockManager.verifyPin(pin)) {
                    lockManager.setSessionUnlocked(true)
                    Toast.makeText(this, "Unlocked!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    etPin.error = "Incorrect PIN"
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isSetupMode) {
                    finishAffinity()
                } else {
                    finish()
                }
            }
        })
    }
}