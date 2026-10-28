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

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val contactsGranted = perms[Manifest.permission.READ_CONTACTS] ?: hasContactsPermission()
        val callLogGranted = perms[Manifest.permission.READ_CALL_LOG] ?: hasCallLogPermission()
        viewModel.onContactsPermission(contactsGranted)
        viewModel.onCallLogPermission(callLogGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            PhoneApp(
                vm = viewModel,
                requestContactsPermission = {
                    permissionsLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                },
                requestCallLogPermission = {
                    permissionsLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG))
                },
            )
        }
        val needed = mutableListOf<String>()
        if (!hasContactsPermission()) needed.add(Manifest.permission.READ_CONTACTS)
        if (!hasCallLogPermission()) needed.add(Manifest.permission.READ_CALL_LOG)
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onContactsPermission(hasContactsPermission())
        viewModel.onCallLogPermission(hasCallLogPermission())
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun hasCallLogPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
}
