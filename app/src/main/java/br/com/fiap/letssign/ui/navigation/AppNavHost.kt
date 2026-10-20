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

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {

    /** Volta ao login limpando todo o histórico (o "voltar" não retorna à área logada). */
    fun goToLogin() {
        navController.navigate(Routes.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { user ->
                    val destino = if (user.perfil == UserRole.ADMIN) Routes.ADMIN_HOME
                    else Routes.SIGNER_HOME
                    navController.navigate(destino) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(onLogout = { goToLogin() })
        }

        composable(Routes.SIGNER_HOME) {
            SignerHomeScreen(onLogout = { goToLogin() })
        }
    }
}