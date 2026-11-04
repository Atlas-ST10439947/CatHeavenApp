package com.example.catheavenapp.ui.admin

/**
 * The cat portraits bundled in res/drawable. The app stores the drawable's
 * name in a cat's photoUrl field and the residents screens look it up by name.
 *
 * Photos are bundled rather than uploaded because Cloud Storage needs the
 * paid Blaze plan; a blank key shows the paw-print placeholder instead.
 */
object CatPhotos {
    const val NONE = ""

    val KEYS: List<String> = listOf(
        NONE, "amore", "aurora", "barack", "benji", "booboo", "chatterley", "coco", "faith", "gigi",
        "hope", "jlo", "lady", "major", "messi", "nunu", "oz", "patches", "paulie", "sparkles",
        "sunshine", "tickey", "tutu", "zaza"
    )

    fun label(key: String): String =
        if (key == NONE) "No photo (paw print)" else key.replaceFirstChar { it.uppercase() }

    /**
     * The drawable to show for a cat: its photoUrl, or, when that's blank, a bundled photo
     * named after the cat (e.g. "Tutu" -> tutu.jpg). Lower-cased, because drawable names are.
     */
    fun drawableKey(photoUrl: String, catName: String): String =
        photoUrl.trim().lowercase().ifBlank { catName.trim().lowercase().replace(Regex("[^a-z0-9_]"), "") }
}
