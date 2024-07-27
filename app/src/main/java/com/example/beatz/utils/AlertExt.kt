@file:Suppress("NOTHING_TO_INLINE", "UNUSED_PARAMETER")


import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.beatz.R
import com.google.android.material.snackbar.Snackbar

import java.lang.ref.WeakReference

/**
 * USAGE : Alert utils class for basic operations like display Alert.
 * Created by R.S.
 */
var snackBarWeekReference: WeakReference<Snackbar?>? = null

fun AppCompatActivity?.dismissSnackBar() {
    takeIf { it != null }?.let {
        snackBarWeekReference!!.get()!!.dismiss()
    }
}



fun Context?.isInternetAvailable(): Boolean {
    val connectivityManager =
        this?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}

fun Context?.showToast(
    @StringRes messageResId: Int? = null,
    message: CharSequence? = null,
    duration: Int = Toast.LENGTH_LONG
) {
    if (messageResId == null && message != null)
        Toast.makeText(this, message, duration).show()
    else if (messageResId != null && message == null)
        Toast.makeText(this, messageResId, duration).show()
}

//
//fun Context?.showSnackBar(
//    message: String = this!!.getString(R.string.alert_message_error),
//    duration: Int = Snackbar.LENGTH_LONG
//) {
//    val context = this as AppCompatActivity
//    context.showSnackBar(context.findViewById(android.R.id.content)!!, message, duration, false)
//}


/**
 * Adds action to the SnackBar
 *
 * @param actionRes Action text to be shown inside the SnackBar
 * @param color Color of the action text
 * @param listener Onclick listener for the action
 */
fun Snackbar.action(@StringRes actionRes: Int, color: Int? = null, listener: (View) -> Unit) {
    setAction(actionRes, listener)
    color?.let { setActionTextColor(color) }
}