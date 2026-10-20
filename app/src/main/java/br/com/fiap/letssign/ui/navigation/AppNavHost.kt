package br.com.fiap.letssign.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.ui.screens.home.AdminHomeScreen
import br.com.fiap.letssign.ui.screens.home.SignerHomeScreen
import br.com.fiap.letssign.ui.screens.login.LoginScreen
import br.com.fiap.letssign.ui.screens.signature.SignatureSetupScreen

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
            AdminHomeScreen(
                onLogout = { irParaLogin() },
                onOpenSignature = { navController.navigate(Routes.SIGNATURE_EDIT) }
            )
        }

        composable(Routes.SIGNER_HOME) {
            SignerHomeScreen(
                onLogout = { irParaLogin() },
                onOpenSignature = { navController.navigate(Routes.SIGNATURE_EDIT) }
            )
        }
    }
}