package com.cydoniancitizen.bingee

import android.app.LocaleManager
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.cydoniancitizen.bingee.app.AppUpdateDialog
import com.cydoniancitizen.bingee.app.AppUpdateViewModel
import com.cydoniancitizen.bingee.app.BingeeApp
import com.cydoniancitizen.bingee.core.designsystem.theme.BingeeTheme
import com.cydoniancitizen.bingee.core.navigation.AppShortcut
import com.cydoniancitizen.bingee.core.navigation.DetailRoute
import com.cydoniancitizen.bingee.data.notification.NotificationDetailIntent
import com.cydoniancitizen.bingee.data.settings.AppLanguage
import com.cydoniancitizen.bingee.data.settings.AppTheme
import com.cydoniancitizen.bingee.data.settings.AppearancePreferences
import com.cydoniancitizen.bingee.data.settings.FontSizePreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appUpdateViewModel: AppUpdateViewModel by viewModels()

    @Inject
    lateinit var appearancePreferences: AppearancePreferences

    @Inject
    lateinit var fontSizePreferences: FontSizePreferences

    /** Route requested by the launching intent: a notification's Details, or a launcher shortcut. */
    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute.value = routeFor(intent)
        enableEdgeToEdge()
        lifecycleScope.launch {
            appearancePreferences.observeLanguage()
                .collect(::applyAppLanguage)
        }
        lifecycleScope.launch {
            appearancePreferences.observeTheme()
                .collect(::applyNightMode)
        }
        setContent {
            val fontScalePercent by remember { fontSizePreferences.observeScalePercent() }
                .collectAsStateWithLifecycle(initialValue = FontSizePreferences.DEFAULT_PERCENT)
            val theme by appearancePreferences.observeTheme().collectAsStateWithLifecycle(
                initialValue = AppTheme.SYSTEM_DEFAULT
            )
            val darkTheme = when (theme) {
                AppTheme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).run {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            BingeeTheme(darkTheme = darkTheme, fontScaleMultiplier = fontScalePercent / 100f) {
                // The root surface gives every screen the theme background and a matching content colour.
                // Screens drawn outside a Scaffold, such as the startup check, otherwise fall back to black
                // text on the window background, which is unreadable in the dark theme.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BingeeApp(
                        pendingRoute = pendingRoute,
                        onPendingRouteConsumed = {
                            pendingRoute.value = null
                            setIntent(Intent(this, MainActivity::class.java))
                        }
                    )
                    val availableUpdate by appUpdateViewModel.availableUpdate.collectAsStateWithLifecycle()
                    AppUpdateDialog(
                        update = availableUpdate,
                        onDismiss = appUpdateViewModel::dismissUpdate,
                        onViewRelease = { url ->
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            } catch (_: ActivityNotFoundException) {
                                // The app remains usable if no browser is installed.
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appUpdateViewModel.onAppOpened()
    }

    override fun onStop() {
        // A retained ViewModel keeps the same check/dialog through rotation, theme and locale changes.
        if (!isChangingConfigurations) appUpdateViewModel.onAppClosed()
        super.onStop()
    }

    /**
     * Keeps the platform night mode on the same preference the Compose theme reads.
     *
     * [BingeeTheme] only decides which `ColorScheme` Compose draws with. Resources resolved through the
     * view system — the placeholder tint, the window background — follow `values-night`, which without
     * this call stays on the system setting. The platform ignores a call that changes nothing.
     */
    private fun applyNightMode(theme: AppTheme) {
        getSystemService(UiModeManager::class.java).setApplicationNightMode(
            when (theme) {
                AppTheme.SYSTEM_DEFAULT -> UiModeManager.MODE_NIGHT_AUTO
                AppTheme.LIGHT -> UiModeManager.MODE_NIGHT_NO
                AppTheme.DARK -> UiModeManager.MODE_NIGHT_YES
            }
        )
    }

    private fun applyAppLanguage(language: AppLanguage) {
        val localeManager = getSystemService(LocaleManager::class.java)
        val locales = LocaleList.forLanguageTags(language.languageTag)
        if (localeManager.applicationLocales != locales) localeManager.applicationLocales = locales
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = routeFor(intent)
    }

    private fun routeFor(intent: Intent?): String? =
        NotificationDetailIntent.parse(intent)?.let { DetailRoute.create(it.mediaType, it.tmdbId) }
            ?: AppShortcut.route(intent)
}
