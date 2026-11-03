package br.com.fiap.letssign.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.ui.screens.login.LoginScreen
import br.com.fiap.letssign.ui.screens.signature.SignatureSetupScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import br.com.fiap.letssign.ui.screens.signers.SignerFormScreen
import br.com.fiap.letssign.ui.screens.signers.SignersListScreen
import br.com.fiap.letssign.ui.screens.document.NewDocumentScreen
import br.com.fiap.letssign.ui.screens.document.DocumentSignersScreen
import br.com.fiap.letssign.ui.screens.admin.AdminDashboardScreen
import br.com.fiap.letssign.ui.screens.splash.SplashScreen
import br.com.fiap.letssign.ui.screens.document.DocumentFieldsScreen
import br.com.fiap.letssign.ui.screens.document.DocumentReviewScreen
import br.com.fiap.letssign.ui.screens.signer.SignDocumentScreen
import br.com.fiap.letssign.ui.screens.signer.SignerDashboardScreen
import br.com.fiap.letssign.ui.screens.tracking.DocumentTrackingScreen
import br.com.fiap.letssign.ui.screens.profile.ProfileScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {

    fun homeDe(perfil: UserRole?): String =
        if (perfil == UserRole.ADMIN) Routes.ADMIN_HOME else Routes.SIGNER_HOME

    fun irParaLogin() {
        navController.navigate(Routes.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    /** Troca de aba sem empilhar telas: o "voltar" sempre leva à aba inicial. */
    fun irParaAba(rota: String, inicio: String) {
        navController.navigate(rota) {
            popUpTo(inicio) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.LOGIN) {
                        // A splash sai do histórico: o "voltar" no login fecha o app
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { user ->
                    // Primeiro acesso (sem assinatura) → onboarding; senão → área do perfil
                    val destino = if (!user.assinaturaConfigurada) Routes.SIGNATURE_ONBOARDING
                    else homeDe(user.perfil)
                    navController.navigate(destino) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SIGNATURE_ONBOARDING) {
            SignatureSetupScreen(
                primeiroAcesso = true,
                onFinish = { perfil ->
                    navController.navigate(homeDe(perfil)) {
                        popUpTo(Routes.SIGNATURE_ONBOARDING) { inclusive = true }
                    }
                },
                onBack = {}
            )
        }

        composable(Routes.SIGNATURE_EDIT) {
            SignatureSetupScreen(
                primeiroAcesso = false,
                onFinish = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_HOME) {
            AdminDashboardScreen(
                onNewDocument = { navController.navigate(Routes.NEW_DOCUMENT) },
                onContinueDraft = { id -> navController.navigate(Routes.documentSigners(id)) },
                onOpenDocument = { id -> navController.navigate(Routes.documentTracking(id)) },
                bottomBar = {
                    RatificaBottomBar(ABAS_ADMIN, Routes.ADMIN_HOME) { irParaAba(it, Routes.ADMIN_HOME) }
                }
            )
        }

        composable(Routes.ADMIN_PROFILE) {
            ProfileScreen(
                onOpenSignature = { navController.navigate(Routes.SIGNATURE_EDIT) },
                onLogout = { irParaLogin() },
                bottomBar = {
                    RatificaBottomBar(ABAS_ADMIN, Routes.ADMIN_PROFILE) { irParaAba(it, Routes.ADMIN_HOME) }
                }
            )
        }

        composable(Routes.SIGNER_PROFILE) {
            ProfileScreen(
                onOpenSignature = { navController.navigate(Routes.SIGNATURE_EDIT) },
                onLogout = { irParaLogin() },
                bottomBar = {
                    RatificaBottomBar(ABAS_SIGNATARIO, Routes.SIGNER_PROFILE) { irParaAba(it, Routes.SIGNER_HOME) }
                }
            )
        }

        composable(Routes.SIGNERS) {
            SignersListScreen(
                onBack = { navController.popBackStack() },
                onNovo = { navController.navigate(Routes.signerForm()) },
                onEditar = { id -> navController.navigate(Routes.signerForm(id)) },
                mostrarVoltar = false,
                bottomBar = {
                    RatificaBottomBar(ABAS_ADMIN, Routes.SIGNERS) { irParaAba(it, Routes.ADMIN_HOME) }
                }
            )
        }

        composable(Routes.NEW_DOCUMENT) {
            NewDocumentScreen(
                onBack = { navController.popBackStack() },
                onCreated = { id ->
                    navController.navigate(Routes.documentSigners(id)) {
                        // Remove a Etapa 1 do histórico: o "voltar" não recria o rascunho
                        popUpTo(Routes.NEW_DOCUMENT) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.SIGNER_FORM,
            arguments = listOf(
                navArgument("signerId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            SignerFormScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SIGNER_HOME) {
            SignerDashboardScreen(
                onOpenDocument = { id -> navController.navigate(Routes.signDocument(id)) },
                onOpenSignature = { navController.navigate(Routes.SIGNATURE_EDIT) },
                bottomBar = {
                    RatificaBottomBar(ABAS_SIGNATARIO, Routes.SIGNER_HOME) { irParaAba(it, Routes.SIGNER_HOME) }
                }
            )
        }

        composable(
            route = Routes.DOCUMENT_SIGNERS,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) {
            DocumentSignersScreen(
                onBack = { navController.popBackStack() },
                onContinue = { id -> navController.navigate(Routes.documentFields(id)) },
                onCadastrarNovo = { navController.navigate(Routes.signerForm()) }
            )
        }

        composable(
            route = Routes.DOCUMENT_FIELDS,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) {
            DocumentFieldsScreen(
                onBack = { navController.popBackStack() },
                onContinue = { id -> navController.navigate(Routes.documentReview(id)) }
            )
        }

        composable(
            route = Routes.DOCUMENT_REVIEW,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) {
            DocumentReviewScreen(
                onBack = { navController.popBackStack() },
                // "Editar" volta para a etapa correspondente (que já está no histórico)
                onEditSigners = { navController.popBackStack(Routes.DOCUMENT_SIGNERS, inclusive = false) },
                onEditFields = { navController.popBackStack(Routes.DOCUMENT_FIELDS, inclusive = false) },
                // Depois do envio, volta ao painel limpando as etapas do histórico
                onFinish = { navController.popBackStack(Routes.ADMIN_HOME, inclusive = false) }
            )
        }

        composable(
            route = Routes.SIGN_DOCUMENT,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) {
            SignDocumentScreen(
                onBack = { navController.popBackStack() },
                onOpenSignature = { navController.navigate(Routes.SIGNATURE_EDIT) }
            )
        }

        composable(
            route = Routes.DOCUMENT_TRACKING,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) {
            DocumentTrackingScreen(
                onBack = { navController.popBackStack() },
                onSign = { id -> navController.navigate(Routes.signDocument(id)) }
            )
        }

    }
}