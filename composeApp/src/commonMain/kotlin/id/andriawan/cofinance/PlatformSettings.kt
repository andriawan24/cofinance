package id.andriawan.cofinance

import coil3.PlatformContext

enum class PermissionStatus {
    GRANTED, DENIED, NOT_DETERMINED
}

interface PermissionHandler {
    fun askPermission(context: PlatformContext, onResult: (Boolean) -> Unit)
    fun checkPermission(context: PlatformContext): PermissionStatus
}

expect fun createCameraPermissionHandler(): PermissionHandler

expect fun PlatformContext.goToSystemSettings()
