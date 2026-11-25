package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOut
import com.example.data.model.Surprise
import com.example.data.repository.GiftRepository
import com.example.ui.screens.AuthDestination
import com.example.ui.screens.CategoryGalleryScreen
import com.example.ui.screens.DashboardShellScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.GiftCodeDialog
import com.example.ui.screens.LandingAuthScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.SurpriseRevealScreen
import com.example.ui.theme.GiftForYouTheme
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.RevealViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_CATEGORIES = "categories"
private const val ROUTE_EDITOR = "editor"
private const val ROUTE_PREVIEW = "preview"
private const val ROUTE_REVEAL = "reveal/{surpriseCode}"

class MainActivity : ComponentActivity() {
    private var deepLinkSurpriseCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkSurpriseCode = extractCodeFromIntent(intent)

        setContent {
            GiftForYouTheme {
                GiftForYouApp(
                    initialDeepLinkCode = deepLinkSurpriseCode,
                    onConsumeDeepLink = { deepLinkSurpriseCode = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractCodeFromIntent(intent)?.let { deepLinkSurpriseCode = it }
    }

    private fun extractCodeFromIntent(intent: Intent?): String? {
        val data = intent?.dataString ?: return null
        val code = GiftRepository.extractSurpriseId(data)
        return code.takeIf { it.isNotBlank() && it != data } ?: code.takeIf { data.contains("/s/") || data.startsWith("giftforyou://") }
    }
}

@Composable
fun GiftForYouApp(
    initialDeepLinkCode: String?,
    onConsumeDeepLink: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var activeRecipientCode by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var authDestination by remember { mutableStateOf(AuthDestination.LANDING) }
    var showGiftCodeDialog by remember { mutableStateOf(false) }
    var pendingDisplayName by remember { mutableStateOf<String?>(null) }

    // Open incoming deep link immediately without forcing sign-in
    LaunchedEffect(initialDeepLinkCode) {
        if (!initialDeepLinkCode.isNullOrBlank()) {
            activeRecipientCode = initialDeepLinkCode
            onConsumeDeepLink()
        }
    }

    // Attempt silent auto sign-in once on startup if no deep link
    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { user ->
                    scope.launch { GiftRepository(context).ensureUserProfile(user) }
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    // Public recipient reveal experience works WITHOUT login
    val codeToOpen = activeRecipientCode
    if (codeToOpen != null) {
        key(codeToOpen) {
            val revealVm: RevealViewModel = viewModel(
                key = "public_$codeToOpen",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                        RevealViewModel(GiftRepository(db))
                    }
                }
            )
            LaunchedEffect(codeToOpen) {
                revealVm.loadPublishedSurprise(codeToOpen)
            }
            SurpriseRevealScreen(
                viewModel = revealVm,
                onClose = { activeRecipientCode = null }
            )
        }
        return
    }

    val startGoogleSignIn: (String?) -> Unit = { customName ->
        isSigningIn = true
        authError = null
        pendingDisplayName = customName
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { signedInUser ->
                isSigningIn = false
                val nameToApply = pendingDisplayName
                pendingDisplayName = null
                scope.launch {
                    GiftRepository(context).ensureUserProfile(signedInUser, nameToApply)
                }
            },
            onAuthError = { msg ->
                isSigningIn = false
                pendingDisplayName = null
                authError = msg
            },
            scope = scope,
            onAuthCancelled = {
                isSigningIn = false
                pendingDisplayName = null
            }
        )
    }

    val user: FirebaseUser? = currentUser
    if (user == null) {
        when (authDestination) {
            AuthDestination.LANDING -> {
                LandingAuthScreen(
                    isSigningIn = isSigningIn,
                    authError = authError,
                    onNavigateToLogin = {
                        authError = null
                        authDestination = AuthDestination.LOGIN
                    },
                    onNavigateToRegister = {
                        authError = null
                        authDestination = AuthDestination.REGISTER
                    },
                    onOpenGiftCodeClick = { showGiftCodeDialog = true }
                )
            }
            AuthDestination.LOGIN -> {
                LoginScreen(
                    isSigningIn = isSigningIn,
                    authError = authError,
                    onGoogleSignInClick = { startGoogleSignIn(null) },
                    onNavigateToRegister = {
                        authError = null
                        authDestination = AuthDestination.REGISTER
                    },
                    onOpenGiftCodeClick = { showGiftCodeDialog = true },
                    onBack = {
                        authError = null
                        authDestination = AuthDestination.LANDING
                    }
                )
            }
            AuthDestination.REGISTER -> {
                RegistrationScreen(
                    isSigningIn = isSigningIn,
                    authError = authError,
                    onGoogleSignUpClick = { displayName -> startGoogleSignIn(displayName) },
                    onNavigateToLogin = {
                        authError = null
                        authDestination = AuthDestination.LOGIN
                    },
                    onBack = {
                        authError = null
                        authDestination = AuthDestination.LANDING
                    }
                )
            }
        }

        if (showGiftCodeDialog) {
            GiftCodeDialog(
                onDismiss = { showGiftCodeDialog = false },
                onOpenGiftCode = { code ->
                    activeRecipientCode = GiftRepository.extractSurpriseId(code)
                }
            )
        }
    } else {
        key(user.uid) {
            LaunchedEffect(user.uid) {
                val nameToApply = pendingDisplayName
                if (nameToApply != null) pendingDisplayName = null
                GiftRepository(context).ensureUserProfile(user, nameToApply)
            }
            AuthenticatedCreatorFlow(
                user = user,
                onOpenGiftCode = { code -> activeRecipientCode = GiftRepository.extractSurpriseId(code) },
                onSignOut = {
                    authDestination = AuthDestination.LANDING
                    signOut(credentialManager, scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedCreatorFlow(
    user: FirebaseUser,
    onOpenGiftCode: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val uid = user.uid
    val senderName = user.displayName ?: "Someone Special"

    val dashboardVm: DashboardViewModel = viewModel(
        key = "dash_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                DashboardViewModel(GiftRepository(db), uid)
            }
        }
    )

    val editorVm: EditorViewModel = viewModel(
        key = "editor_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                EditorViewModel(GiftRepository(db), uid, senderName)
            }
        }
    )

    var previewTarget by remember { mutableStateOf<Surprise?>(null) }

    NavHost(navController = navController, startDestination = ROUTE_DASHBOARD) {
        composable(ROUTE_DASHBOARD) {
            DashboardShellScreen(
                viewModel = dashboardVm,
                userDisplayName = senderName,
                userEmail = user.email ?: "",
                userPhotoUrl = user.photoUrl?.toString(),
                onCreateSurprise = { navController.navigate(ROUTE_CATEGORIES) },
                onEditSurprise = { surprise ->
                    editorVm.startEditing(surprise)
                    navController.navigate(ROUTE_EDITOR)
                },
                onPreviewSurprise = { surprise ->
                    previewTarget = surprise
                    navController.navigate(ROUTE_PREVIEW)
                },
                onOpenGiftCode = onOpenGiftCode,
                onSignOut = onSignOut
            )
        }

        composable(ROUTE_CATEGORIES) {
            CategoryGalleryScreen(
                onBack = { navController.popBackStack() },
                onCategorySelected = { categoryId ->
                    editorVm.startNewWithCategory(categoryId)
                    navController.navigate(ROUTE_EDITOR)
                }
            )
        }

        composable(ROUTE_EDITOR) {
            EditorScreen(
                viewModel = editorVm,
                onBack = { navController.popBackStack() },
                onPreviewSurprise = { draft ->
                    previewTarget = draft
                    navController.navigate(ROUTE_PREVIEW)
                },
                onPublishedDone = {
                    navController.popBackStack(ROUTE_DASHBOARD, inclusive = false)
                }
            )
        }

        composable(ROUTE_PREVIEW) {
            val target = previewTarget
            if (target != null) {
                val revealVm: RevealViewModel = viewModel(
                    key = "preview_${target.id}_${target.title.hashCode()}",
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                            RevealViewModel(GiftRepository(db))
                        }
                    }
                )
                LaunchedEffect(target) {
                    revealVm.loadPreviewSurprise(target)
                }
                SurpriseRevealScreen(
                    viewModel = revealVm,
                    onClose = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = ROUTE_REVEAL,
            arguments = listOf(navArgument("surpriseCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("surpriseCode") ?: ""
            val revealVm: RevealViewModel = viewModel(
                key = "reveal_$code",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                        RevealViewModel(GiftRepository(db))
                    }
                }
            )
            LaunchedEffect(code) {
                revealVm.loadPublishedSurprise(code)
            }
            SurpriseRevealScreen(
                viewModel = revealVm,
                onClose = { navController.popBackStack() }
            )
        }
    }
}
