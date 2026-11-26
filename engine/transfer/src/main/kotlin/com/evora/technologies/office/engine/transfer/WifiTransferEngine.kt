package com.evora.technologies.office.engine.transfer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.domain.port.WifiTransferPort
import com.evora.technologies.office.core.model.WifiTransferConfig
import com.evora.technologies.office.core.model.WifiTransferLogEntry
import com.evora.technologies.office.core.model.WifiTransferServerState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferEngineImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WifiTransferPort {

    private val _serverState = MutableStateFlow(WifiTransferServerState())
    override val serverState: StateFlow<WifiTransferServerState> = _serverState.asStateFlow()

    private val managerScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val lifecycleMutex = Mutex()
    private var idleTimerJob: Job? = null
    private var httpServer: KtorWifiHttpServer? = null
    private var ftpServer: EmbeddedFtpServer? = null

    @Volatile
    private var lastActivityEpochMs: Long = 0L

    override suspend fun startServer(config: WifiTransferConfig): Result<Unit> = withContext(ioDispatcher) {
        lifecycleMutex.withLock {
            startServerLocked(config)
        }
    }

    private fun startServerLocked(config: WifiTransferConfig): Result<Unit> {
        return try {
            stopEnginesOnly()

            val sharedRoot = resolveSharedRoot()
            val uploadRoot = resolveUploadRoot(sharedRoot)
            val ip = resolveLanIpv4Address()

            val http = KtorWifiHttpServer(
                sharedRootDir = sharedRoot,
                uploadRootDir = uploadRoot,
                loadIndexHtml = {
                    context.assets.open("wifi_web/index.html").bufferedReader().use { it.readText() }
                },
                configProvider = { _serverState.value.config },
                onActivity = ::recordTransferActivity,
                onFileUploaded = { file ->
                    runCatching {
                        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                    }
                },
            )
            val boundHttpPort = http.start(port = config.httpPort)
            httpServer = http

            val boundFtpPort = if (config.ftpEnabled) {
                val desiredFtp = if (config.ftpPort == boundHttpPort) boundHttpPort + 1 else config.ftpPort
                val ftp = EmbeddedFtpServer(
                    sharedRootDir = sharedRoot,
                    uploadRootDir = uploadRoot,
                    hostIpProvider = ::resolveLanIpv4Address,
                    configProvider = { _serverState.value.config },
                    onActivity = ::recordTransferActivity,
                    onFileUploaded = { file ->
                        runCatching {
                            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                        }
                    },
                )
                ftpServer = ftp
                ftp.start(desiredFtp)
            } else {
                config.ftpPort
            }

            lastActivityEpochMs = System.currentTimeMillis()
            val httpUrl = "http://$ip:$boundHttpPort"
            val ftpUrl = if (config.ftpEnabled) "ftp://$ip:$boundFtpPort" else ""

            _serverState.value = _serverState.value.copy(
                isRunning = true,
                ipAddress = ip,
                httpUrl = httpUrl,
                ftpUrl = ftpUrl,
                activeHttpPort = boundHttpPort,
                activeFtpPort = boundFtpPort,
                config = config,
                idleSecondsRemaining = config.idleTimeoutSec,
                errorMessage = null,
            )

            startForegroundServiceNotification()
            startIdleWatchdog(config.idleTimeoutSec)
            requestTileSync()
            Result.success(Unit)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            stopEnginesOnly()
            _serverState.update {
                it.copy(isRunning = false, errorMessage = t.message ?: "Unable to start Wi-Fi transfer")
            }
            Result.failure(t)
        }
    }

    override suspend fun stopServer(): Result<Unit> = withContext(ioDispatcher) {
        lifecycleMutex.withLock {
            stopInternal()
        }
        Result.success(Unit)
    }

    override suspend fun updateConfig(config: WifiTransferConfig) = withContext(ioDispatcher) {
        lifecycleMutex.withLock {
            val wasRunning = _serverState.value.isRunning
            val ftpToggled = _serverState.value.config.ftpEnabled != config.ftpEnabled
            _serverState.update { it.copy(config = config) }
            if (wasRunning && ftpToggled) {
                startServerLocked(config)
            }
        }
    }

    private fun stopInternal() {
        stopEnginesOnly()
        _serverState.update {
            it.copy(
                isRunning = false,
                httpUrl = "",
                ftpUrl = "",
                idleSecondsRemaining = it.config.idleTimeoutSec,
            )
        }
        runCatching {
            context.stopService(Intent(context, WifiTransferService::class.java))
        }
        requestTileSync()
    }

    private fun stopEnginesOnly() {
        idleTimerJob?.cancel()
        idleTimerJob = null
        httpServer?.stop()
        httpServer = null
        ftpServer?.stop()
        ftpServer = null
    }

    private fun startIdleWatchdog(timeoutSec: Int) {
        idleTimerJob?.cancel()
        idleTimerJob = managerScope.launch {
            val safeTimeout = timeoutSec.coerceAtLeast(30)
            while (isActive && _serverState.value.isRunning) {
                delay(1_000L)
                val elapsedSec = ((System.currentTimeMillis() - lastActivityEpochMs) / 1000L).toInt()
                val remaining = (safeTimeout - elapsedSec).coerceAtLeast(0)
                _serverState.update { it.copy(idleSecondsRemaining = remaining) }
                if (remaining == 0) {
                    lifecycleMutex.withLock {
                        if (_serverState.value.isRunning) {
                            stopInternal()
                        }
                    }
                    break
                }
            }
        }
    }

    private fun recordTransferActivity(
        protocol: String,
        action: String,
        targetName: String,
        bytes: Long,
    ) {
        lastActivityEpochMs = System.currentTimeMillis()
        val timeoutSec = _serverState.value.config.idleTimeoutSec
        if (action == "UPLOAD" || action == "DOWNLOAD") {
            val entry = WifiTransferLogEntry(
                id = UUID.randomUUID().toString(),
                protocol = protocol,
                action = action,
                targetName = targetName,
                bytes = bytes,
                timestampEpochMs = lastActivityEpochMs,
            )
            _serverState.update {
                it.copy(
                    idleSecondsRemaining = timeoutSec,
                    transferLogs = (listOf(entry) + it.transferLogs).take(30),
                )
            }
        } else {
            _serverState.update { it.copy(idleSecondsRemaining = timeoutSec) }
        }
    }

    private fun startForegroundServiceNotification() {
        runCatching {
            val intent = Intent(context, WifiTransferService::class.java)
            context.startForegroundService(intent)
        }
    }

    private fun requestTileSync() {
        runCatching {
            TileService.requestListeningState(
                context,
                ComponentName(context, WifiTransferTileService::class.java),
            )
        }
    }

    private fun resolveSharedRoot(): File {
        val ext = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
        if (ext != null && ext.exists() && ext.canRead() && ext.listFiles() != null) {
            return ext
        }
        return (context.getExternalFilesDir(null) ?: File(context.filesDir, "documents")).apply { mkdirs() }
    }

    private fun resolveUploadRoot(sharedRoot: File): File {
        val publicDownloads = runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }.getOrNull()
        if (publicDownloads != null && publicDownloads.exists() && publicDownloads.canWrite()) {
            return File(publicDownloads, "EvoraOffice").apply { mkdirs() }
        }
        return File(sharedRoot, "EvoraOffice").apply { mkdirs() }
    }

    private fun resolveLanIpv4Address(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val linkAddresses = cm?.getLinkProperties(activeNetwork)?.linkAddresses.orEmpty()
        linkAddresses.mapNotNull { it.address as? Inet4Address }
            .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            ?.hostAddress
            ?.let { return it }

        val interfaces = runCatching { NetworkInterface.getNetworkInterfaces() }.getOrNull()
        while (interfaces != null && interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            if (!iface.isUp || iface.isLoopback) continue
            val addrs = iface.inetAddresses
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                    return addr.hostAddress ?: continue
                }
            }
        }
        return "127.0.0.1"
    }
}

@AndroidEntryPoint
class WifiTransferService : Service() {

    @Inject
    lateinit var wifiTransferPort: WifiTransferPort

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_TRANSFER) {
            serviceScope.launch {
                wifiTransferPort.stopServer()
                stopSelf()
            }
            return START_NOT_STICKY
        }

        ensureChannel()
        val state = wifiTransferPort.serverState.value
        val notification = buildNotification(state.httpUrl)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (intent == null || !state.isRunning) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Wi-Fi File Transfer",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Active LAN HTTP and FTP file transfer server"
                },
            )
        }
    }

    private fun buildNotification(httpUrl: String): Notification {
        val stopIntent = Intent(this, WifiTransferService::class.java).apply {
            action = ACTION_STOP_TRANSFER
        }
        val stopPending = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPending = if (launchIntent != null) {
            PendingIntent.getActivity(
                this,
                102,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            null
        }
        val contentText = if (httpUrl.isNotBlank()) {
            "Open $httpUrl on your PC browser"
        } else {
            "Wi-Fi transfer server active"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Evora Office Wi-Fi Transfer")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(contentPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Server", stopPending)
            .build()
    }

    companion object {
        const val ACTION_STOP_TRANSFER = "com.evora.technologies.office.ACTION_STOP_TRANSFER"
        private const val CHANNEL_ID = "evora_wifi_transfer"
        private const val NOTIFICATION_ID = 8998
    }
}

@AndroidEntryPoint
class WifiTransferTileService : TileService() {

    @Inject
    lateinit var wifiTransferPort: WifiTransferPort

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        syncTileState()
    }

    override fun onClick() {
        super.onClick()
        tileScope.launch {
            val current = wifiTransferPort.serverState.value
            if (current.isRunning) {
                wifiTransferPort.stopServer()
            } else {
                wifiTransferPort.startServer(current.config)
            }
            syncTileState()
        }
    }

    private fun syncTileState() {
        val tile = qsTile ?: return
        val running = wifiTransferPort.serverState.value.isRunning
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (running) wifiTransferPort.serverState.value.httpUrl else "Off"
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        tileScope.cancel()
        super.onDestroy()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TransferEngineModule {
    @Binds
    @Singleton
    abstract fun bindWifiTransferPort(impl: TransferEngineImpl): WifiTransferPort
}
