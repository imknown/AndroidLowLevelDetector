package net.imknown.android.forefrontinfo.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { Content() }
    }

    @Preview
    @Composable
    fun Content(){
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        var selectedItem by remember { mutableIntStateOf(0) }

        val navigationItems = listOf(
            "Home" to Icons.Outlined.Home,
            "Others" to Icons.Outlined.Info,
            "Properties" to Icons.AutoMirrored.Outlined.List,
            "Settings" to Icons.Outlined.Settings
        )

        Scaffold(
            modifier = Modifier.wrapContentWidth(),
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Scaffold Demo") },
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("FAB clicked")
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            },
            bottomBar = {
                NavigationBar {
                    navigationItems.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = index == selectedItem,
                            onClick = {
                                selectedItem = index
                                scope.launch {
                                    snackbarHostState.showSnackbar("Selected $label")
                                }
                            }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = contentColorFor(MaterialTheme.colorScheme.background)
        ) { paddingValues ->
            // Main content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Text(
                        text = "Content Area",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    if (selectedItem >= 0 && selectedItem < navigationItems.size) {
                        Text(
                            text = "Selected tab: ${navigationItems[selectedItem].first}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

