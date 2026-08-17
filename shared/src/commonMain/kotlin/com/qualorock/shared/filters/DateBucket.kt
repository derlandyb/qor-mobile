package com.qualorock.shared.filters

enum class DateBucket(val wireValue: String, val label: String) {
    HOJE("hoje", "Hoje"),
    AMANHA("amanha", "Amanhã"),
    FIM_DE_SEMANA("fim_de_semana", "Este fim de semana"),
    PROXIMA_SEMANA("proxima_semana", "Próxima semana"),
}
