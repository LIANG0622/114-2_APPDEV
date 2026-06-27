package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.MarineDatabase
import com.example.data.MarineRepository
import com.example.ui.MarineAppLayout
import com.example.ui.MarineViewModel
import com.example.ui.MarineViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: MarineDatabase
    private lateinit var repository: MarineRepository
    private lateinit var viewModel: MarineViewModel

    // Activity Contract Launcher to request fine Gps locations and camera permissions on startup
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        
        if (fineGranted) {
            viewModel.triggerGPSLocation(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init Room Database and Repository
        database = MarineDatabase.getDatabase(applicationContext, lifecycleScope)
        repository = MarineRepository(database.marineDao())

        // Init ViewModel with Factory pattern
        val factory = MarineViewModelFactory(repository, application)
        viewModel = ViewModelProvider(this, factory)[MarineViewModel::class.java]

        // Fire permission request
        requestPermissionsLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.CAMERA
            )
        )

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MarineAppLayout(viewModel = viewModel)
                }
            }
        }
    }
}
