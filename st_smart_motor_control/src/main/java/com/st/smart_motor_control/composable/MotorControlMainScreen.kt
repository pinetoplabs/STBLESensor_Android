package com.st.smart_motor_control.composable

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.st.blue_sdk.board_catalog.models.DtmiContent
import com.st.smart_motor_control.MotorControlConfig
import com.st.smart_motor_control.R
import com.st.smart_motor_control.SmartMotorControlViewModel
import com.st.smart_motor_control.model.MotorControlFault
import com.st.ui.composables.BottomAppBarItem
import com.st.ui.composables.BottomAppBarItemColor
import com.st.ui.composables.CommandRequest
import com.st.ui.composables.ComposableLifecycle
import com.st.ui.theme.Grey0
import com.st.ui.theme.Grey6
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.SecondaryBlue
import kotlinx.serialization.json.JsonObject

@Composable
fun MotorControlMainScreen(
    modifier: Modifier,
    viewModel: SmartMotorControlViewModel,
    nodeId: String
) {

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.startDemo(nodeId = nodeId)
            Lifecycle.Event.ON_STOP -> viewModel.stopDemo(nodeId = nodeId)
            else -> Unit
        }
    }

    val isLogging by viewModel.isLogging.collectAsStateWithLifecycle()
    val sensorsActuators by viewModel.sensorsActuators.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val status by viewModel.componentStatusUpdates.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSDCardInserted by viewModel.isSDCardInserted.collectAsStateWithLifecycle()
    val acquisitionName by viewModel.acquisitionName.collectAsStateWithLifecycle()
    val vespucciTags by viewModel.vespucciTags.collectAsStateWithLifecycle()


    val faultStatus by viewModel.faultStatus.collectAsStateWithLifecycle()
    val temperature by viewModel.temperature.collectAsStateWithLifecycle()
    val speedRef by viewModel.speedRef.collectAsStateWithLifecycle()
    val speedMeas by viewModel.speedMeas.collectAsStateWithLifecycle()
    val busVoltage by viewModel.busVoltage.collectAsStateWithLifecycle()
    val isMotorRunning by viewModel.isMotorRunning.collectAsStateWithLifecycle()

    val motorSpeed by viewModel.motorSpeed.collectAsStateWithLifecycle()

    val motorSpeedControl by viewModel.motorSpeedControl.collectAsStateWithLifecycle()


    val context = LocalContext.current


    SmartMotorControlScreen(
        modifier = modifier,
        sensorsActuators = sensorsActuators,
        tags = tags,
        status = status,
        isSDCardInserted = isSDCardInserted,
        isLogging = isLogging,
        isLoading = isLoading,
        vespucciTags = vespucciTags,
        acquisitionName = acquisitionName,
        onTagChangeState = { tag, newState ->
            viewModel.onTagChangeState(nodeId, tag, newState)
        },
        onValueChange = { name, value ->
            if (isLoading.not()) {
                viewModel.sendChange(
                    nodeId = nodeId,
                    name = name,
                    value = value
                )
            }
        },
        onSendCommand = { name, value ->
            if (isLoading.not()) {
                viewModel.sendCommand(
                    nodeId = nodeId,
                    name = name,
                    value = value
                )
            }
        },
        onStartStopLog = {
            if (it) {
                if (isLogging.not() && isLoading.not()) {
                    viewModel.startLog(nodeId)
                }
            } else {
                if (isMotorRunning.not() && isLoading.not()) {
                    viewModel.stopLog(nodeId)
                } else {
                    Toast.makeText(
                        context,
                        "Motor is still Running...\nStop before the Motor",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        onRefresh = {
            if (isLogging.not() && isLoading.not()) {
                viewModel.refresh(nodeId)
            }
        },
        faultStatus = faultStatus,
        temperature = temperature,
        speedRef = speedRef,
        speedMeas = speedMeas,
        busVoltage = busVoltage,
        isMotorRunning = isMotorRunning,
        motorSpeed = motorSpeed,
        motorSpeedControl = motorSpeedControl,
        temperatureUnit = viewModel.temperatureUnit,
        speedRefUnit = viewModel.speedRefUnit,
        speedMeasUnit = viewModel.speedMeasUnit,
        busVoltageUnit = viewModel.busVoltageUnit
    )
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun SmartMotorControlScreen(
    modifier: Modifier,
    sensorsActuators: List<Pair<DtmiContent.DtmiComponentContent, DtmiContent.DtmiInterfaceContent>> = emptyList(),
    tags: List<Pair<DtmiContent.DtmiComponentContent, DtmiContent.DtmiInterfaceContent>> = emptyList(),
    status: List<JsonObject>,
    vespucciTags: Map<String, Boolean>,
    isLogging: Boolean,
    isSDCardInserted: Boolean = false,
    isLoading: Boolean = false,
    acquisitionName: String = "",
    onValueChange: (String, Pair<String, Any>) -> Unit,
    onSendCommand: (String, CommandRequest?) -> Unit,
    onTagChangeState: (String, Boolean) -> Unit = { _, _ -> /**NOOP**/ },
    onStartStopLog: (Boolean) -> Unit = { /**NOOP **/ },
    onRefresh: () -> Unit = { /**NOOP **/ },
    faultStatus: MotorControlFault = MotorControlFault.None,
    temperature: Int? = null,
    speedRef: Int? = null,
    speedMeas: Int? = null,
    busVoltage: Int? = null,
    temperatureUnit: String,
    speedRefUnit: String,
    speedMeasUnit: String,
    busVoltageUnit: String,
    isMotorRunning: Boolean = false,
    motorSpeed: Int = 1024,
    motorSpeedControl: DtmiContent.DtmiPropertyContent.DtmiIntegerPropertyContent? = null,
    navController: NavHostController = rememberNavController()
) {
    val sensorsActuatorsTitle = stringResource(id = R.string.st_motor_control_configuration)
    val tagsTitle = stringResource(id = R.string.st_motor_control_tags)
    var currentTitle by remember { mutableStateOf(sensorsActuatorsTitle) }
    var openStopDialog by remember { mutableStateOf(value = false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = onRefresh
    )

    var selectedIndex by remember {
        mutableIntStateOf(0)
    }

    val haptic = LocalHapticFeedback.current

    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        isFloatingActionButtonDocked = true,
        topBar = {
            MotorControlConfig.motorControlTabBar?.invoke(currentTitle, isLoading)
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                backgroundColor = SecondaryBlue,
                shape = CircleShape,
                onClick = {
                    if (isSDCardInserted) {
                        if (isLogging) {
                            onStartStopLog(false)
                            openStopDialog = MotorControlConfig.showStopDialog
                        } else {
                            onStartStopLog(true)
                        }
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.st_motor_control_missingSdCard),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) {
                Icon(
                    tint = MaterialTheme.colorScheme.primary,
                    imageVector = if (isLogging) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
        },
        bottomBar = {
            BottomNavigation(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = Grey0
            ) {
                BottomNavigationItem(
                    selectedContentColor = Grey0,
                    unselectedContentColor = Grey6,
                    selected = 0 == selectedIndex,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedIndex = 0
                        currentTitle = sensorsActuatorsTitle
                        navController.navigate("MotorControl") {
                            navController.graph.startDestinationRoute?.let { screenRoute ->
                                popUpTo(screenRoute) {
                                    saveState = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_motor_control),
                            contentDescription = stringResource(id = R.string.st_motor_control),
                            tint =  if(0 == selectedIndex) {Grey0 } else {Grey6}
                        )
                    },
                    label = { Text(text = stringResource(id = R.string.st_motor_control)) },
                    enabled = !isLoading
                )

                if (isLogging) {
                    BottomNavigationItem(
                        selectedContentColor = Grey0,
                        unselectedContentColor = Grey6,
                        selected = 1 == selectedIndex,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIndex = 1
                            currentTitle = tagsTitle
                            navController.navigate("Tags") {
                                navController.graph.startDestinationRoute?.let { screenRoute ->
                                    popUpTo(screenRoute) {
                                        saveState = true
                                    }
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tags),
                                contentDescription = stringResource(id = R.string.st_motor_control_tags),
                                tint =  if(1 == selectedIndex) {Grey0 } else {Grey6}
                            )
                        },
                        label = { Text(text = stringResource(id = R.string.st_motor_control_tags)) },
                        enabled = !isLoading
                    )
                } else {
                    BottomNavigationItem(
                        selectedContentColor = Grey0,
                        unselectedContentColor = Grey6,
                        selected = 1 == selectedIndex,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIndex = 1
                            currentTitle = sensorsActuatorsTitle
                            navController.navigate("Sensors") {
                                navController.graph.startDestinationRoute?.let { screenRoute ->
                                    popUpTo(screenRoute) {
                                        saveState = true
                                    }
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = stringResource(id = R.string.st_motor_control_configuration),
                                tint =  if(1 == selectedIndex) {Grey0 } else {Grey6}
                            )
                        },
                        label = { Text(text = stringResource(id = R.string.st_motor_control_configuration)) },
                        enabled = !isLoading
                    )
                }

            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.pullRefresh(state = pullRefreshState)) {
            NavHost(
                modifier = modifier.padding(paddingValues),
                navController = navController,
                startDestination = "MotorControl",
            ) {

                composable(
                    route = "MotorControl"
                ) {
                    MotorControl(
                        isLoading = isLoading,
                        faultStatus = faultStatus,
                        temperature = temperature,
                        speedRef = speedRef,
                        speedMeas = speedMeas,
                        busVoltage = busVoltage,
                        isRunning = isMotorRunning,
                        isLogging = isLogging,
                        motorSpeed = motorSpeed,
                        motorSpeedControl = motorSpeedControl,
                        onSendCommand = onSendCommand,
                        onValueChange = onValueChange,
                        temperatureUnit = temperatureUnit,
                        speedRefUnit = speedRefUnit,
                        speedMeasUnit = speedMeasUnit,
                        busVoltageUnit = busVoltageUnit
                    )
                }

                composable(
                    route = "Sensors"
                ) {
                    if (isLogging.not()) {
                        MotorControlSensors(
                            sensorsActuators = sensorsActuators,
                            status = status,
                            isLoading = isLoading,
                            onValueChange = onValueChange,
                            onSendCommand = onSendCommand
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.material3.Text(text = stringResource(id = R.string.st_motor_control_logging))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                    }
                }

                composable(
                    route = "Tags"
                ) {
                    if (MotorControlConfig.tags.isEmpty()) {
                        MotorControlTags(
                            tags = tags,
                            status = status,
                            isLoading = isLoading,
                            onValueChange = onValueChange,
                            onSendCommand = onSendCommand
                        )
                    } else {
                        VespucciMotorControlTags(
                            acquisitionInfo = acquisitionName,
                            vespucciTags = vespucciTags,
                            isLoading = isLoading,
                            isLogging = isLogging,
                            onTagChangeState = onTagChangeState
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(alignment = Alignment.TopCenter),
                scale = true
            )
        }
    }

    if (openStopDialog) {
        StopLoggingDialog(
            onDismissRequest = { openStopDialog = false }
        )
    }
}