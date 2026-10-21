package br.com.fiap.letssign.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val SIGNATURE_ONBOARDING = "signature_onboarding"
    const val SIGNATURE_EDIT = "signature_edit"
    const val ADMIN_HOME = "admin_home"
    const val SIGNER_HOME = "signer_home"
    const val SIGNERS = "signers"
    const val SIGNER_FORM = "signer_form?signerId={signerId}"

    /** Monta a rota do formulário: sem id = novo; com id = editar. */
    fun signerForm(id: Long? = null): String =
        if (id == null) "signer_form" else "signer_form?signerId=$id"
}