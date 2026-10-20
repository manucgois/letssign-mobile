package br.com.fiap.letssign.ui.screens.login

import br.com.fiap.letssign.data.local.entity.UserEntity

data class LoginUiState(
    val email: String = "",
    val senha: String = "",
    val senhaVisivel: Boolean = false,
    val emailErro: String? = null,
    val senhaErro: String? = null,
    val erroGeral: String? = null,
    val carregando: Boolean = false,
    val usuarioLogado: UserEntity? = null
)