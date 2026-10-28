package com.arady.phone

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.arady.phone.ui.PhoneApp

class MainActivity : ComponentActivity() {

    private val viewModel: PhoneViewModel by viewModels()

    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onContactsPermission(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            PhoneApp(viewModel, requestContactsPermission = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) })
        }
        if (!hasContactsPermission()) {
            contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    override fun onStart() {
        super.onStart()
        // picks up contacts that changed while the app was in the background, or a permission granted in Settings
        if (hasContactsPermission()) viewModel.onContactsPermission(true)
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
}
