package com.uniflow.app.core.common

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

fun Throwable?.toTurkishErrorMessage(defaultMessage: String = "Bir hata oluştu. Lütfen tekrar deneyin."): String {
    if (this == null) return defaultMessage

    val rawMessage = (message ?: "").lowercase()
    val localizedStr = (localizedMessage ?: "").lowercase()
    val errorType = this::class.java.simpleName.lowercase()
    val combined = "$rawMessage $localizedStr $errorType"

    return when {
        this is FirebaseAuthUserCollisionException ||
                combined.contains("already in use") ||
                combined.contains("email-already-in-use") ||
                combined.contains("already exists") -> {
            "Bu e-posta adresi zaten başka bir hesap tarafından kullanılıyor. Lütfen giriş yapın."
        }

        this is FirebaseAuthWeakPasswordException ||
                combined.contains("weak-password") ||
                combined.contains("weak password") ||
                combined.contains("6 characters") -> {
            "Şifre en az 6 karakter olmalıdır."
        }

        this is FirebaseAuthInvalidUserException ||
                combined.contains("user-not-found") ||
                combined.contains("no user record") ||
                combined.contains("user disabled") -> {
            "Hesap bulunamadı, lütfen kayıt olun."
        }

        this is FirebaseAuthInvalidCredentialsException ||
                combined.contains("invalid-email") ||
                combined.contains("badly formatted") ||
                combined.contains("invalid email") -> {
            if (combined.contains("password") || combined.contains("credential")) {
                "Hesap bulunamadı veya e-posta/şifre hatalı."
            } else {
                "Geçersiz e-posta adresi formatı."
            }
        }

        combined.contains("wrong-password") ||
                combined.contains("invalid credential") ||
                combined.contains("credential is incorrect") ||
                combined.contains("malformed or has expired") -> {
            "Hesap bulunamadı veya şifre hatalı."
        }

        this is FirebaseNetworkException ||
                combined.contains("network") ||
                combined.contains("connection") ||
                combined.contains("unreachable") -> {
            "İnternet bağlantınızı kontrol edip tekrar deneyin."
        }

        combined.contains("too-many-requests") ||
                combined.contains("blocked") -> {
            "Çok fazla hatalı deneme yapıldı. Lütfen biraz bekleyip tekrar deneyin."
        }

        combined.contains("permission-denied") -> {
            "Bu işlem için yetkiniz bulunmuyor."
        }

        combined.contains("unavailable") -> {
            "Sunucu şu anda hizmet veremiyor. Lütfen daha sonra tekrar deneyin."
        }

        else -> defaultMessage
    }
}

fun String?.toTurkishErrorMessage(defaultMessage: String = "Bir hata oluştu. Lütfen tekrar deneyin."): String {
    if (this.isNullOrBlank()) return defaultMessage

    val str = this.lowercase()

    return when {
        str.contains("already in use") || str.contains("email-already-in-use") || str.contains("already exists") -> {
            "Bu e-posta adresi zaten başka bir hesap tarafından kullanılıyor. Lütfen giriş yapın."
        }

        str.contains("weak-password") || str.contains("weak password") || str.contains("6 characters") -> {
            "Şifre en az 6 karakter olmalıdır."
        }

        str.contains("user-not-found") || str.contains("no user record") || str.contains("user disabled") -> {
            "Hesap bulunamadı, lütfen kayıt olun."
        }

        str.contains("invalid-email") || str.contains("badly formatted") -> {
            "Geçersiz e-posta adresi."
        }

        str.contains("wrong-password") || str.contains("credential is incorrect") || str.contains("invalid credential") || str.contains("malformed or has expired") -> {
            "Hesap bulunamadı veya şifre hatalı."
        }

        str.contains("network") || str.contains("connection") || str.contains("unavailable") -> {
            "İnternet bağlantınızı kontrol edip tekrar deneyin."
        }

        str.contains("too-many-requests") || str.contains("blocked") -> {
            "Çok fazla hatalı deneme yapıldı. Lütfen biraz bekleyip tekrar deneyin."
        }

        else -> this
    }
}
