package br.com.fiap.letssign.domain.model

/** Perfil de acesso do usuário no app. */
enum class UserRole(val label: String) {
    ADMIN("Administrador"),
    SIGNATARIO("Signatário")
}

/** Papel do signatário em um documento específico (definido por documento, não no perfil). */
enum class SignerRole(val label: String) {
    CONTRATANTE("Contratante"),
    CONTRATADO("Contratado"),
    TESTEMUNHA("Testemunha")
}

/** Situação geral do documento ao longo do ciclo de vida. */
enum class DocumentStatus(val label: String) {
    RASCUNHO("Rascunho"),
    ENVIADO("Enviado"),
    PARCIALMENTE_ASSINADO("Parcialmente assinado"),
    CONCLUIDO("Concluído"),
    RECUSADO("Recusado"),
    EXPIRADO("Expirado")
}

/** Situação individual de cada signatário. */
enum class SignerStatus(val label: String) {
    PENDENTE("Pendente"),
    ASSINADO("Assinado"),
    RECUSADO("Recusado")
}

/** Tipos de evento registrados na trilha de auditoria. */
enum class AuditEventType(val label: String) {
    CRIADO("Documento criado"),
    ENVIADO("Enviado para assinatura"),
    VISUALIZADO("Documento visualizado"),
    ASSINADO("Assinatura realizada"),
    RECUSADO("Assinatura recusada"),
    CONCLUIDO("Documento concluído"),
    NOVA_VERSAO("Nova versão criada")
}

/** Tipo de identificação profissional no perfil de assinatura. */
enum class IdentificationType(val label: String) {
    CPF("CPF"),
    OAB("OAB"),
    CRC("CRC"),
    CRM("CRM"),
    CREA("CREA"),
    MATRICULA("Matrícula"),
    OUTRO("Outro")
}

/** Forma visual escolhida para a assinatura. */
enum class SignatureStyle(val label: String) {
    DESENHADA("Desenhada"),
    DIGITADA("Digita da"),
    RUBRICA("Rubrica")
}

/** Resultado de uma tentativa de envio (usado para não duplicar solicitações). */
enum class SendStatus {
    PENDENTE,
    SUCESSO,
    FALHA
}