package net.imknown.android.forefrontinfo.ui

import android.os.Bundle
import android.os.PowerManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.base.ext.toast
import net.imknown.android.forefrontinfo.ui.base.list.BaseListScreen
import net.imknown.android.forefrontinfo.ui.common.State
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid10
import net.imknown.android.forefrontinfo.ui.home.HomeViewModel
import net.imknown.android.forefrontinfo.ui.home.datasource.LldDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.MountDataSource
import net.imknown.android.forefrontinfo.ui.home.repository.HomeRepository
import net.imknown.android.forefrontinfo.ui.others.OthersViewModel
import net.imknown.android.forefrontinfo.ui.others.datasource.ArchitectureDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.BasicDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.FingerprintDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.KernelDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.OthersDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.RomDataSource
import net.imknown.android.forefrontinfo.ui.others.repository.OthersRepository
import net.imknown.android.forefrontinfo.ui.prop.PropViewModel
import net.imknown.android.forefrontinfo.ui.prop.datasource.PropertiesDataSource
import net.imknown.android.forefrontinfo.ui.prop.datasource.SettingsDataSource
import net.imknown.android.forefrontinfo.ui.prop.repository.PropRepository
import net.imknown.android.forefrontinfo.ui.settings.SettingsScreen
import net.imknown.android.forefrontinfo.ui.settings.SettingsViewModel
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import net.imknown.android.forefrontinfo.ui.settings.repository.SettingsRepository
import net.imknown.android.forefrontinfo.ui.theme.AppTheme
import net.imknown.android.forefrontinfo.ui.settings.datasource.FingerprintDataSource as SettingsFingerprintDataSource

class MainActivity : AppCompatActivity() {

    private val homeViewModel by viewModels<HomeViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                this[HomeViewModel.MY_REPOSITORY_KEY] = HomeRepository(
                    LldDataSource(), MountDataSource(), AppInfoDataSource()
                )
            }
        },
        factoryProducer = { HomeViewModel.Factory }
    )

    private val othersViewModel by viewModels<OthersViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                this[OthersViewModel.MY_REPOSITORY_KEY] = OthersRepository(
                    BasicDataSource(),
                    ArchitectureDataSource(),
                    RomDataSource(),
                    FingerprintDataSource(),
                    KernelDataSource(),
                    OthersDataSource()
                )
            }
        },
        factoryProducer = { OthersViewModel.Factory }
    )

    private val propViewModel by viewModels<PropViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                this[PropViewModel.MY_REPOSITORY_KEY] = PropRepository(
                    PropertiesDataSource(), SettingsDataSource()
                )
            }
        },
        factoryProducer = { PropViewModel.Factory }
    )

    private val settingsViewModel by viewModels<SettingsViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                this[SettingsViewModel.MY_REPOSITORY_KEY] = SettingsRepository(
                    AppInfoDataSource(), SettingsFingerprintDataSource()
                )
            }
        },
        factoryProducer = { SettingsViewModel.Factory }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (isAtLeastAndroid10()) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val sharedPreferences = remember(context) {
                PreferenceManager.getDefaultSharedPreferences(context)
            }

            val themesKey = stringResource(id = R.string.interface_themes_key)
            val followSystem = stringResource(id = R.string.interface_themes_follow_system_value)
            val powerSaver = stringResource(id = R.string.interface_themes_power_saver_value)
            val alwaysLight = stringResource(id = R.string.interface_themes_always_light_value)
            val alwaysDark = stringResource(id = R.string.interface_themes_always_dark_value)

            val themeValue = remember(sharedPreferences, SettingsViewModel.themeChanged) {
                sharedPreferences.getString(themesKey, followSystem)
            }

            val darkTheme = when (themeValue) {
                followSystem -> isSystemInDarkTheme()
                powerSaver -> {
                    val powerManager = context.getSystemService<PowerManager>()
                    powerManager?.isPowerSaveMode == true
                }
                alwaysLight -> false
                alwaysDark -> true
                else -> isSystemInDarkTheme()
            }

            AppTheme(darkTheme = darkTheme) {
                MainScreen(homeViewModel, othersViewModel, propViewModel, settingsViewModel)
            }
        }
    }
}

private val navigationItemsData: List<Triple<Int, Int, ImageVector>> = listOf(
    Triple(R.string.title_home, R.id.navigation_home, Icons.Outlined.Home),
    Triple(R.string.title_others, R.id.navigation_others, Icons.Outlined.Info),
    Triple(R.string.title_prop, R.id.navigation_prop, Icons.AutoMirrored.Outlined.List),
    Triple(R.string.title_settings, R.id.navigation_settings, Icons.Outlined.Settings)
)

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    othersViewModel: OthersViewModel,
    propViewModel: PropViewModel,
    settingsViewModel: SettingsViewModel
) {
    val pagerState = rememberPagerState { navigationItemsData.size }
    val coroutineScope = rememberCoroutineScope()

    val homeListState = rememberLazyListState()
    val othersListState = rememberLazyListState()
    val propListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()

    val context = LocalContext.current

    // Initialize version info
    LaunchedEffect(Unit) {
        settingsViewModel.setBuiltInDataVersion(context.packageManager, context.packageName)
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                navigationItemsData.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.third, contentDescription = stringResource(item.first)) },
                        label = { Text(stringResource(item.first)) },
                        selected = index == pagerState.currentPage,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            beyondViewportPageCount = navigationItemsData.size
        ) { page ->
            when (page) {
                0 -> BaseListScreen(homeViewModel, homeListState)
                1 -> BaseListScreen(othersViewModel, othersListState)
                2 -> BaseListScreen(propViewModel, propListState)
                3 -> {
                    val versionState = settingsViewModel.version
                    val versionSummary = if (versionState is State.Done) {
                        val version = versionState.toValue()
                        stringResource(
                            id = version.id,
                            version.versionName,
                            version.versionCode,
                            version.assetLldVersion,
                            version.distributor,
                            version.installer,
                            version.firstInstallTime,
                            version.lastUpdateTime
                        )
                    } else {
                        ""
                    }

                    SettingsScreen(
                        listState = settingsListState,
                        versionSummary = versionSummary,
                        onThemeChanged = { settingsViewModel.emitThemeChanged(it) },
                        onScrollBarModeChanged = { settingsViewModel.emitScrollBarModeChanged(it) },
                        onOutdatedOrderChanged = { settingsViewModel.emitOutdatedOrderChanged() },
                        onVersionClick = {
                            settingsViewModel.getVersionClickedMessage()?.let {
                                context.toast(it)
                            }
                        }
                    )
                }
            }
        }
    }
}
