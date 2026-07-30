package com.followme.app.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.followme.app.AppContainer
import com.followme.app.data.repository.AppRole
import com.followme.app.ui.camera.CameraHomeScreen
import com.followme.app.ui.camera.CameraPairingScreen
import com.followme.app.ui.devicedetail.DeviceDetailScreen
import com.followme.app.ui.devices.AddDeviceScreen
import com.followme.app.ui.devices.DeviceListScreen
import com.followme.app.ui.login.LoginScreen
import com.followme.app.ui.recordings.RecordingListScreen
import com.followme.app.ui.recordings.RecordingPlayerScreen
import com.followme.app.ui.register.RegisterScreen
import com.followme.app.ui.role.RoleSelectionScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private object Routes {
    const val SPLASH = "splash"
    const val ROLE_SELECTION = "role_selection"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DEVICES = "devices"
    const val ADD_DEVICE = "add_device"
    const val DEVICE_DETAIL = "device_detail/{deviceId}/{deviceName}"
    const val RECORDINGS = "recordings/{deviceId}/{deviceName}"
    const val PLAYER = "player/{recordingId}/{type}"
    const val CAMERA_PAIRING = "camera_pairing"
    const val CAMERA_HOME = "camera_home"

    fun deviceDetail(deviceId: String, deviceName: String) = "device_detail/$deviceId/${Uri.encode(deviceName)}"
    fun recordings(deviceId: String, deviceName: String) = "recordings/$deviceId/${Uri.encode(deviceName)}"
    fun player(recordingId: String, type: String) = "player/$recordingId/$type"
}

@Composable
fun FollowMeNavHost(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        container.authRepository.loggedOutEvents.collect {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            LaunchedEffect(Unit) {
                val destination = when (container.cameraSessionRepository.appRole.first()) {
                    null -> Routes.ROLE_SELECTION
                    AppRole.CONTROLLER.value -> {
                        if (container.authRepository.isLoggedIn.first()) Routes.DEVICES else Routes.LOGIN
                    }
                    AppRole.CAMERA.value -> {
                        if (container.cameraSessionRepository.deviceSession.first() != null) {
                            Routes.CAMERA_HOME
                        } else {
                            Routes.CAMERA_PAIRING
                        }
                    }
                    else -> Routes.ROLE_SELECTION
                }
                navController.navigate(destination) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        composable(Routes.ROLE_SELECTION) {
            RoleSelectionScreen(
                cameraSessionRepository = container.cameraSessionRepository,
                onRoleChosen = { role ->
                    val destination = if (role == AppRole.CONTROLLER) Routes.LOGIN else Routes.CAMERA_PAIRING
                    navController.navigate(destination) {
                        popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CAMERA_PAIRING) {
            CameraPairingScreen(
                cameraSessionRepository = container.cameraSessionRepository,
                onPaired = {
                    navController.navigate(Routes.CAMERA_HOME) {
                        popUpTo(Routes.CAMERA_PAIRING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CAMERA_HOME) {
            CameraHomeScreen(
                cameraSessionRepository = container.cameraSessionRepository,
                onUnpaired = {
                    navController.navigate(Routes.CAMERA_PAIRING) {
                        popUpTo(Routes.CAMERA_HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                authRepository = container.authRepository,
                onLoginSuccess = {
                    navController.navigate(Routes.DEVICES) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                authRepository = container.authRepository,
                onRegisterSuccess = {
                    navController.navigate(Routes.DEVICES) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.DEVICES) {
            DeviceListScreen(
                deviceRepository = container.deviceRepository,
                onDeviceClick = { deviceId, deviceName ->
                    navController.navigate(Routes.deviceDetail(deviceId, deviceName))
                },
                onAddDevice = { navController.navigate(Routes.ADD_DEVICE) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    scope.launch { container.authRepository.logout() }
                },
            )
        }

        composable(Routes.ADD_DEVICE) {
            AddDeviceScreen(
                deviceRepository = container.deviceRepository,
                onDone = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.DEVICE_DETAIL) { backStackEntry ->
            DeviceDetailScreen(
                deviceRepository = container.deviceRepository,
                deviceId = requireNotNull(backStackEntry.getRouteArg("deviceId")),
                deviceName = Uri.decode(requireNotNull(backStackEntry.getRouteArg("deviceName"))),
                onViewRecordings = {
                    val id = requireNotNull(backStackEntry.getRouteArg("deviceId"))
                    val name = Uri.decode(requireNotNull(backStackEntry.getRouteArg("deviceName")))
                    navController.navigate(Routes.recordings(id, name))
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.RECORDINGS) { backStackEntry ->
            RecordingListScreen(
                recordingRepository = container.recordingRepository,
                deviceId = requireNotNull(backStackEntry.getRouteArg("deviceId")),
                deviceName = Uri.decode(requireNotNull(backStackEntry.getRouteArg("deviceName"))),
                onOpenRecording = { recordingId, type ->
                    navController.navigate(Routes.player(recordingId, type))
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PLAYER) { backStackEntry ->
            RecordingPlayerScreen(
                recordingRepository = container.recordingRepository,
                okHttpClient = container.okHttpClient,
                recordingId = requireNotNull(backStackEntry.getRouteArg("recordingId")),
                recordingType = requireNotNull(backStackEntry.getRouteArg("type")),
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

private fun NavBackStackEntry.getRouteArg(name: String): String? = arguments?.getString(name)
