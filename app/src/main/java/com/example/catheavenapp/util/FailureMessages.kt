package com.example.catheavenapp.util

import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Turns a failed Firestore call into a message that tells the user what actually went wrong.
 * A refusal by the security rules is not a connection problem, so it gets its own wording.
 */
object FailureMessages {

    fun forSupporter(error: Throwable, action: String): String = when (codeOf(error)) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Cat Heaven couldn't $action because the change isn't allowed. Refresh and try again, " +
                "or contact Cat Heaven if it keeps happening."
        else -> "We couldn't $action. Check your connection and try again."
    }

    fun forAdmin(error: Throwable, action: String): String = when (codeOf(error)) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore refused to $action. Check that this account is an admin and the details are valid."
        else -> "We couldn't $action. Check your connection and try again."
    }

    /** Firestore sometimes wraps its exception (e.g. inside a transaction), so look one level down too. */
    private fun codeOf(error: Throwable): FirebaseFirestoreException.Code? =
        (error as? FirebaseFirestoreException)?.code ?: (error.cause as? FirebaseFirestoreException)?.code
}
