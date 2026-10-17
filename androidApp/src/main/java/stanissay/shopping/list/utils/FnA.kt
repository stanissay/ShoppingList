package stanissay.shopping.list.utils

import android.content.Context
import android.widget.Toast

fun showCustomToast(text: String, context: Context) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
