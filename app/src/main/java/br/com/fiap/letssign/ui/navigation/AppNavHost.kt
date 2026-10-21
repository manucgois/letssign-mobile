package br.com.fiap.letssign.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.ui.screens.home.SignerHomeScreen
import br.com.fiap.letssign.ui.screens.login.LoginScreen
import br.com.fiap.letssign.ui.screens.signature.SignatureSetupScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import br.com.fiap.letssign.ui.screens.signers.SignerFormScreen
import br.com.fiap.letssign.ui.screens.signers.SignersListScreen
import br.com.fiap.letssign.ui.screens.document.NewDocumentScreen
import br.com.fiap.letssign.ui.screens.document.DocumentSignersScreen
import br.com.fiap.letssign.ui.screens.admin.AdminDashboardScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {

    fun homeDe(perfil: UserRole?): String =
        if (perfil == UserRole.ADMIN) Routes.ADMIN_HOME else Routes.SIGNER_HOME

    fun irParaLogin() {
        navController.navigate(Routes.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

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
            val context = androidx.compose.ui.platform.LocalContext.current
            AdminDashboardScreen(
                onNewDocument = { navController.navigate(Routes.NEW_DOCUMENT) },
                onOpenSigners = { navController.navigate(Routes.SIGNERS) },
                onOpenSignature = { navController.navigate(Routes.SIGNATURE_EDIT) },
                onContinueDraft = { id -> navController.navigate(Routes.documentSigners(id)) },
                // Provisório: depois abre a tela de acompanhamento
                onOpenDocument = {
                    android.widget.Toast.makeText(
                        context, "Acompanhamento: em breve!", android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                onLogout = { irParaLogin() }
            )
        }

        composable(Routes.SIGNERS) {
            SignersListScreen(
                onBack = { navController.popBackStack() },
                onNovo = { navController.navigate(Routes.signerForm()) },
                onEditar = { id -> navController.navigate(Routes.signerForm(id)) }
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
            SignerHomeScreen(
                onLogout = { irParaLogin() },
                onOpenSignature = { navController.navigate(Routes.SIGNATURE_EDIT) }
            )
        }

        composable(
            route = Routes.DOCUMENT_SIGNERS,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            DocumentSignersScreen(
                onBack = { navController.popBackStack() },
                // Provisório: na próxima etapa, vai para "Campos de assinatura"
                onContinue = {
                    android.widget.Toast.makeText(
                        context, "Signatários salvos! Próxima etapa: campos.", android.widget.Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                },
                onCadastrarNovo = { navController.navigate(Routes.signerForm()) }
            )
        }
    }
}