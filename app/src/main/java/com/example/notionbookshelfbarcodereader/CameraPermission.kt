package com.example.notionbookshelfbarcodereader;

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object CameraPermission {
    private const val PERMISSION = Manifest.permission.CAMERA
    private const val ACTION = ActivityResultContracts.RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS
    private const val EXTRA_REQUEST = ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSIONS
    private const val EXTRA_RESULT = ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS

    class RequestContract : ActivityResultContract<Unit, Boolean>() {
        override fun createIntent(context: Context, input: Unit): Intent =
            Intent(ACTION).putExtra(EXTRA_REQUEST, arrayOf(PERMISSION))

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            if (resultCode != AppCompatActivity.RESULT_OK) return false
            return intent
                ?.getIntArrayExtra(EXTRA_RESULT)
                ?.getOrNull(0) == PackageManager.PERMISSION_GRANTED
        }

        override fun getSynchronousResult(
            context: Context, input: Unit?
        ): SynchronousResult<Boolean>? {
            return when {
                input == null -> SynchronousResult(false)
                hasPermission(context) -> SynchronousResult(true)
                else -> null
            }
        }
    }

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
}