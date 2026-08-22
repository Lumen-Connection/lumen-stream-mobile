package com.lumenconnection.stream.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.lumenconnection.stream.MainActivity

/**
 * Alvo do share sheet: recebe texto de qualquer app, extrai o primeiro link
 * e abre a Home com ele pré-preenchido para escolha de formato.
 */
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val url = URL_REGEX.find(text)?.value

        if (url != null) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(MainActivity.EXTRA_SHARED_URL, url)
            )
        }
        finish()
    }

    companion object {
        private val URL_REGEX = Regex("""https?://\S+""")
    }
}
