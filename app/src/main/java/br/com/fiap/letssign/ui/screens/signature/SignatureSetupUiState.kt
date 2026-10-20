package br.com.fiap.letssign.ui.screens.signature

import br.com.fiap.letssign.domain.model.IdentificationType
import br.com.fiap.letssign.domain.model.SignatureStyle
import br.com.fiap.letssign.domain.model.UserRole

data class SignatureSetupUiState(
    val carregando: Boolean = true,
    val userId: Long? = null,
    val perfil: UserRole? = null,

    val nome: String = "",
    val cargo: String = "",
    val tipoIdentificacao: IdentificationType = IdentificationType.CPF,
    val numero: String = "",
    val empresa: String = "",
    val estilo: SignatureStyle = SignatureStyle.DESENHADA,
    val textoDigitado: String = "",

    // Imagens já salvas antes (quando a pessoa está editando o perfil)
    val assinaturaPath: String? = null,
    val rubricaPath: String? = null,

    val nomeErro: String? = null,
    val numeroErro: String? = null,
    val empresaErro: String? = null,
    val visualErro: String? = null,
    val erroGeral: String? = null,

    val salvando: Boolean = false,
    val salvo: Boolean = false
)