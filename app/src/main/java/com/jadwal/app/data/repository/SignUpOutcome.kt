package com.jadwal.app.data.repository

sealed class SignUpOutcome {
    data object SuccessWithSession : SignUpOutcome()
    data object EmailConfirmationRequired : SignUpOutcome()
    data object EmailAlreadyRegistered : SignUpOutcome()
}
