package midni.test

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.graphics.BitmapFactory
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.view.View
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import midni.test.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var verifier: MidniQrVerifier

    // â”€â”€ Lectura HID (teclado USB) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private val hidBuffer = StringBuilder()
    private var isListeningHid = false
    private val hidHandler = Handler(Looper.getMainLooper())
    private var hidPendingBytes: ByteArray? = null
    /** Procesa el buffer si no llegan mÃ¡s caracteres en 300 ms (fin de trama). */
    private val hidTimeoutRunnable = Runnable { processHidBuffer() }

    // â”€â”€ Lectura puerto serie virtual USB â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private var serialPort: UsbSerialPort? = null
    private var serialIoManager: SerialInputOutputManager? = null
    private val serialRecvBuffer = ByteArrayOutputStream()
    private val serialHandler = Handler(Looper.getMainLooper())
    private var serialPendingBytes: ByteArray? = null
    private var pendingSerialDriver: UsbSerialDriver? = null
    private var pendingBaudRate: Int = 9600
    private val serialTimeoutRunnable = Runnable { processSerialBuffer() }
    private var tcpThread: Thread? = null
    private var tcpSocket: java.net.Socket? = null

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (USB_PERMISSION_ACTION == intent.action) {
                @Suppress("DEPRECATION")
                unregisterReceiver(this)
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (granted) {
                    pendingSerialDriver?.let {
                        openSerialPort(getSystemService(USB_SERVICE) as UsbManager, it, pendingBaudRate)
                    }
                } else {
                    updateStatus("Sin permiso", "Permiso USB denegado por el usuario.")
                }
                pendingSerialDriver = null
            }
        }
    }

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null && result.originalIntent == null) {
            updateStatus("Escaneo cancelado", "No se recibieron datos del QR.")
            return@registerForActivityResult
        }

        val rawBytes = result.contents?.toByteArray(StandardCharsets.ISO_8859_1)
            ?: ByteArray(0)

        if (rawBytes.isEmpty()) {
            updateStatus("Error de lectura", "El QR no contiene datos validos.")
            return@registerForActivityResult
        }

        val verification = verifier.verify(rawBytes)
        binding.tvRaw.text = verification.debugSummary

        if (verification.success) {
            updateStatus("VÃLIDO", verification.userSummary)
            showCredential(verification.personalData)
        } else {
            updateStatus("INVÃLIDO", verification.userSummary)
            binding.cardCredential.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verifier = MidniQrVerifier(assets)

        binding.tvTitle.text = "MVP MIDNI v${BuildConfig.VERSION_NAME}"

        if (!deviceHasCamera()) {
            binding.btnScan.isEnabled = false
            binding.tvStatus.text = getString(R.string.status_no_camera)
        }

        binding.btnScan.setOnClickListener {
            startScan()
        }

        binding.btnHid.setOnClickListener {
            startHidListening()
        }

        binding.btnHidComprobar.setOnClickListener {
            val bytes = hidPendingBytes ?: serialPendingBytes ?: return@setOnClickListener
            hidPendingBytes = null
            serialPendingBytes = null
            verifyHidBytes(bytes)
        }

        binding.btnHidCancelar.setOnClickListener {
            resetToInitialState()
        }

        binding.btnSerial.setOnClickListener {
            startSerialSelection()
        }
    }

    private fun startScan() {
        if (!deviceHasCamera()) {
            Toast.makeText(this, R.string.status_no_camera, Toast.LENGTH_LONG).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            return
        }

        val options = ScanOptions()
            .setCaptureActivity(AnyOrientationCaptureActivity::class.java)
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Escanea el QR de miDNI")
            .setBeepEnabled(false)
            .setOrientationLocked(false)
            .addExtra(Intents.Scan.CHARACTER_SET, "ISO-8859-1")

        scanLauncher.launch(options)
    }

    private fun deviceHasCamera(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    // â”€â”€ HID â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun startHidListening() {
        hidBuffer.clear()
        isListeningHid = true
        binding.btnHid.setText(R.string.hid_button_listening)
        binding.btnHid.isEnabled = false
        updateStatus("Esperando USBâ€¦", "Pase el QR por el lector USB.")
        // Timeout de seguridad: 10 segundos sin datos
        hidHandler.postDelayed({
            if (isListeningHid) {
                if (hidBuffer.isNotEmpty()) processHidBuffer()
                else {
                    stopHidListening()
                    updateStatus("Sin datos", "No se recibieron datos del lector USB.")
                }
            }
        }, 10_000)
    }

    private fun stopHidListening() {
        isListeningHid = false
        hidHandler.removeCallbacksAndMessages(null)
        binding.btnHid.setText(R.string.hid_button)
        binding.btnHid.isEnabled = true
    }

    private fun processHidBuffer() {
        val rawText = hidBuffer.toString()
        val rawBytes = rawText.toByteArray(Charsets.ISO_8859_1)
        stopHidListening()
        if (rawBytes.isEmpty()) {
            updateStatus("Sin datos", "El lector USB no enviÃ³ datos.")
            return
        }
        hidPendingBytes = rawBytes
        binding.tvPreviewTitle.setText(R.string.hid_preview_title)
        binding.tvHidReceived.text = rawText
        binding.cardHidPreview.visibility = View.VISIBLE
        updateStatus("Datos recibidos", "Revise los datos y pulse 'Comprobar' para validar.")
    }

    private fun verifyHidBytes(rawBytes: ByteArray) {
        val verification = verifier.verify(rawBytes)
        binding.tvRaw.text = verification.debugSummary
        binding.cardHidPreview.visibility = View.GONE
        hidPendingBytes = null
        if (verification.success) {
            updateStatus("VÃLIDO", verification.userSummary)
            showCredential(verification.personalData)
        } else {
            updateStatus("INVÃLIDO", verification.userSummary)
            binding.cardCredential.visibility = View.GONE
        }
    }

    private fun resetToInitialState() {
        hidPendingBytes = null
        serialPendingBytes = null
        hidBuffer.clear()
        stopHidListening()
        stopSerialListening()
        binding.cardHidPreview.visibility = View.GONE
        binding.cardCredential.visibility = View.GONE
        binding.tvRaw.text = ""
        updateStatus(getString(R.string.status_idle), "")
    }

    /**
     * Intercepta los eventos de teclado del dispositivo HID USB.
     * Los caracteres se acumulan en [hidBuffer]; al recibir Enter o tras
     * 300 ms de inactividad, se procesa el buffer como datos QR.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isListeningHid || event.action != KeyEvent.ACTION_DOWN)
            return super.dispatchKeyEvent(event)

        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            hidHandler.removeCallbacks(hidTimeoutRunnable)
            processHidBuffer()
            return true
        }

        val ch = event.unicodeChar
        if (ch != 0) {
            hidBuffer.append(ch.toChar())
            // Reiniciar temporizador de inactividad
            hidHandler.removeCallbacks(hidTimeoutRunnable)
            hidHandler.postDelayed(hidTimeoutRunnable, 300)
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScan()
        } else if (requestCode == CAMERA_PERMISSION_REQUEST) {
            updateStatus("Permiso denegado", "Se necesita permiso de camara para escanear el QR.")
        }
    }

    private fun updateStatus(title: String, details: String) {
        binding.tvStatus.text = title
        binding.tvDetails.text = details
    }

    private fun showCredential(data: PersonalData?) {
        if (data == null) {
            binding.cardCredential.visibility = View.GONE
            return
        }
        
        binding.cardCredential.visibility = View.VISIBLE
        binding.tvName.text = data.name ?: "N/D"
        binding.tvSurnames.text = data.surnames ?: "N/D"
        binding.tvDocumentNumber.text = data.documentNumber ?: "N/D"
        binding.tvDateOfBirth.text = data.dateOfBirth ?: "N/D"
        binding.tvSex.text = data.sex ?: "N/D"
        binding.tvExpiration.text = data.documentExpiration ?: "N/D"
        
        if (data.photoBytes != null) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(data.photoBytes, 0, data.photoBytes.size)
                if (bitmap != null) {
                    binding.ivPhoto.setImageBitmap(bitmap)
                } else {
                    binding.ivPhoto.setImageResource(android.R.color.transparent)
                }
            } catch (e: Exception) {
                binding.ivPhoto.setImageResource(android.R.color.transparent)
            }
        } else {
            binding.ivPhoto.setImageResource(android.R.color.transparent)
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1001
        private const val USB_PERMISSION_ACTION = "midni.test.USB_PERMISSION"
    }

    // â”€â”€ Puerto serie virtual USB â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun startSerialSelection() {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        val portItems = mutableListOf<String>()
        portItems.addAll(drivers.map { d ->
            val dev = d.device
            "${dev.deviceName}  [VID:${"%04X".format(dev.vendorId)} PID:${"%04X".format(dev.productId)}]  (${d.ports.size} puerto/s)"
        })
        portItems.add("[Simulacion TCP puerto 9876 â€” emulador/debug]")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar fuente de datos serie")
            .setItems(portItems.toTypedArray()) { _, idx ->
                if (idx == portItems.size - 1) {
                    startTcpBridgeMode()
                } else {
                    val driver = drivers[idx]
                    val baudRates = arrayOf("9600", "115200")
                    AlertDialog.Builder(this)
                        .setTitle("Velocidad del puerto")
                        .setItems(baudRates) { _, bIdx ->
                            connectToSerialPort(usbManager, driver, baudRates[bIdx].toInt())
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun startTcpBridgeMode(port: Int = 9876) {
        serialRecvBuffer.reset()
        binding.btnSerial.setText(R.string.serial_button_listening)
        binding.btnSerial.isEnabled = false
        updateStatus("Conectando TCP\u2026",
            "Intentando localhost:$port. Aseg\u00FArese de que 'adb reverse tcp:$port tcp:$port' est\u00E1 activo y el bridge PowerShell est\u00E1 en ejecuci\u00F3n.")
        tcpThread = Thread {
            try {
                val socket = java.net.Socket("localhost", port)
                tcpSocket = socket
                runOnUiThread {
                    updateStatus("Esperando datos TCP\u2026", "Conectado al puerto $port. Env\u00EDe datos desde el host.")
                }
                val inputStream = socket.getInputStream()
                val buf = ByteArray(4096)
                while (!socket.isClosed && !Thread.currentThread().isInterrupted) {
                    val n = inputStream.read(buf)
                    if (n < 0) break
                    val data = buf.copyOf(n)
                    runOnUiThread {
                        serialRecvBuffer.write(data)
                        serialHandler.removeCallbacks(serialTimeoutRunnable)
                        serialHandler.postDelayed(serialTimeoutRunnable, 300)
                    }
                }
            } catch (e: java.net.ConnectException) {
                runOnUiThread {
                    stopSerialListening()
                    updateStatus("Error TCP",
                        "Conexi\u00F3n rechazada en puerto $port. \u00BFEst\u00E1 el bridge PowerShell activo?")
                }
            } catch (e: Exception) {
                if (!Thread.currentThread().isInterrupted) {
                    runOnUiThread {
                        stopSerialListening()
                        updateStatus("Error TCP", e.message ?: "Error desconocido")
                    }
                }
            }
        }
        tcpThread!!.isDaemon = true
        tcpThread!!.start()
    }

    private fun connectToSerialPort(usbManager: UsbManager, driver: UsbSerialDriver, baudRate: Int) {
        if (!usbManager.hasPermission(driver.device)) {
            val pi = PendingIntent.getBroadcast(
                this, 0, Intent(USB_PERMISSION_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            pendingSerialDriver = driver
            pendingBaudRate = baudRate
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(usbPermissionReceiver, IntentFilter(USB_PERMISSION_ACTION), RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(usbPermissionReceiver, IntentFilter(USB_PERMISSION_ACTION))
            }
            usbManager.requestPermission(driver.device, pi)
        } else {
            openSerialPort(usbManager, driver, baudRate)
        }
    }

    private fun openSerialPort(usbManager: UsbManager, driver: UsbSerialDriver, baudRate: Int) {
        try {
            val connection = usbManager.openDevice(driver.device)
                ?: run { updateStatus("Error", "No se pudo abrir el dispositivo USB."); return }
            val port = driver.ports[0]
            port.open(connection)
            port.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            serialPort = port
            serialRecvBuffer.reset()
            binding.btnSerial.setText(R.string.serial_button_listening)
            binding.btnSerial.isEnabled = false
            updateStatus("Esperando datos serieâ€¦", "Puerto abierto a $baudRate bps. Pase el QR por el lector.")
            val listener = object : SerialInputOutputManager.Listener {
                override fun onNewData(data: ByteArray) {
                    runOnUiThread {
                        serialRecvBuffer.write(data)
                        serialHandler.removeCallbacks(serialTimeoutRunnable)
                        serialHandler.postDelayed(serialTimeoutRunnable, 300)
                    }
                }
                override fun onRunError(e: Exception) {
                    runOnUiThread {
                        stopSerialListening()
                        updateStatus("Error serie", "Error de lectura: ${e.message}")
                    }
                }
            }
            serialIoManager = SerialInputOutputManager(port, listener)
            serialIoManager!!.start()
        } catch (e: Exception) {
            updateStatus("Error", "No se pudo abrir el puerto: ${e.message}")
        }
    }

    private fun stopSerialListening() {
        serialHandler.removeCallbacksAndMessages(null)
        serialIoManager?.stop()
        serialIoManager = null
        try { serialPort?.close() } catch (_: Exception) {}
        serialPort = null
        tcpThread?.interrupt()
        tcpThread = null
        try { tcpSocket?.close() } catch (_: Exception) {}
        tcpSocket = null
        binding.btnSerial.setText(R.string.serial_button)
        binding.btnSerial.isEnabled = true
    }

    private fun processSerialBuffer() {
        val bytes = serialRecvBuffer.toByteArray()
        stopSerialListening()
        if (bytes.isEmpty()) {
            updateStatus("Sin datos", "El lector serie no enviÃ³ datos.")
            return
        }
        serialPendingBytes = bytes
        binding.tvPreviewTitle.setText(R.string.serial_preview_title)
        binding.tvHidReceived.text = String(bytes, Charsets.ISO_8859_1)
        binding.cardHidPreview.visibility = View.VISIBLE
        updateStatus("Datos recibidos", "Revise los datos y pulse \u2018Comprobar\u2019 para validar.")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSerialListening()
    }
}

