package com.fh.bluetoothexample

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.fh.bluetoothexample.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var companionDeviceManager: CompanionDeviceManager
    private lateinit var intentSender: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var enableBlueToothLauncher: ActivityResultLauncher<Intent>
    private lateinit var intentFilter: IntentFilter


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        companionDeviceManager =
            getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()


        intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, intentFilter)


        intentSender =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val deviceToPair =
                        it.data?.getParcelableExtra<BluetoothDevice>(CompanionDeviceManager.EXTRA_DEVICE)
                    deviceToPair?.createBond()
                    Log.i("TAG", "onCreate: ${deviceToPair?.name}")
                }

            }


        enableBlueToothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

                if (it.resultCode != RESULT_OK) {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                } else {
                    getNearbyBluetoothDevices()
                }
            }


        checkBluetoothStatus()
        getNearbyBluetoothDevices()
        getPairedDevice()


    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun getNearbyBluetoothDevices() {
        val deviceFilter = BluetoothDeviceFilter.Builder()
//            .setNamePattern(Pattern.compile("My Device"))
//            .addServiceUuid(ParcelUuid(UUID(0x123abcL, -1L)), null)
            .build()

        val pairingRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(false)
            .build()

        companionDeviceManager.associate(
            pairingRequest,
            object : CompanionDeviceManager.Callback() {
                override fun onDeviceFound(chooserLauncher: IntentSender?) {
                    intentSender.launch(chooserLauncher?.let {
                        IntentSenderRequest.Builder(it).build()
                    })

                }

                override fun onFailure(error: CharSequence?) {
                    Log.i("TAG", "onFailure:${error.toString()} ")
                    Toast.makeText(this@MainActivity, "${error.toString()}", Toast.LENGTH_SHORT).show()
                }
            }, null
        )
    }


    private fun getPairedDevice() {
        val pairedDevice: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        pairedDevice.forEach {
            val deviceName = it.name
            val deviceHardWareAddress = it.address

            Log.i("TAG", "getPairedDevice: $deviceName,$deviceHardWareAddress")
            Log.i("TAG", "getPairedDevice_________________________________________: ")

        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkBluetoothStatus() {
        if (!bluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBlueToothLauncher.launch(intent)
        } else {
            getNearbyBluetoothDevices()
        }


    }


    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {
                if (it.action == BluetoothAdapter.ACTION_STATE_CHANGED) {

                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_OFF -> {

                            Snackbar.make(
                                binding.root,
                                "Bluetooth Is Disabled",
                                Snackbar.LENGTH_LONG
                            ).show()

                        }
                        BluetoothAdapter.STATE_ON -> {

                            Snackbar.make(
                                binding.root,
                                "Bluetooth Is Enabled",
                                Snackbar.LENGTH_LONG
                            ).show()

                        }
                    }

                }
            }

        }
    }


    override fun onResume() {
        super.onResume()
        registerReceiver(bluetoothReceiver, intentFilter)
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }


}