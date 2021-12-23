package com.im.flutter_plugin_gpay



import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64
import android.util.Base64.encodeToString
import android.util.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.math.BigInteger



class FlutterPluginGpayPlugin internal constructor(registrar: Registrar, channel: MethodChannel) : MethodCallHandler, ActivityResultListener {
  private val activity = registrar.activity()

  private var result: Result? = null
  private var requestCodeNumber = 201119

  var hasResponded = false

  override fun onMethodCall(call: MethodCall, result: Result) {
    hasResponded = false

    this.result = result

    when (call.method) {
      "initiateTransaction" -> this.initiateTransaction(call)
      "getInstalledUpiApps" -> this.getInstalledUpiApps()
      else -> result.notImplemented()
    }
  }

  private fun initiateTransaction(call: MethodCall) {
    val app: String? = call.argument("app")
    val pa: String? = call.argument("pa")
    val pn: String? = call.argument("pn")
    val mc: String? = call.argument("mc")
    val tr: String? = call.argument("tr")
    val tn: String? = call.argument("tn")
    val am: String? = call.argument("am")
    val cu: String? = call.argument("cu")
    val url: String? = call.argument("url")
    val mode: String? = call.argument("mode")
    val orgid: String? = call.argument("orgid")

    try {
      /*
       * Some UPI apps extract incorrect format VPA due to url encoding of `pa` parameter.
       * For example, the VPA 'abc@upi' gets url encoded as 'abc%40upi' and is extracted as
       * 'abc 40upi' by these apps. The URI building logic is changed to avoid URL encoding
       * of the value of 'pa' parameter. - Reetesh
      */
      var uriStr: String? = "upi://pay?pa=" + pa +
              "&pn=" + Uri.encode(pn) +
              "&tr=" + Uri.encode(tr) +
              "&am=" + Uri.encode(am) +
              "&cu=" + Uri.encode(cu) +
              "&mode=" + Uri.encode(mode) +
              "&orgid=" + Uri.encode(orgid)
      if(url != null) {
        uriStr += ("&url=" + Uri.encode(url))
      }
      if(mc != null) {
        uriStr += ("&mc=" + Uri.encode(mc))
      }
      if(tn != null) {
        uriStr += ("&tn=" + Uri.encode(tn))
      }
      //uriStr += "&mode=02" // &orgid=000000"
      //uriStr += "&orgid=000000"
      var uri = Uri.parse(uriStr)
      Log.d("upi_pay", "initiateTransaction URI: " + uri.toString())

      //var signhashed = hashString("SHA-256", uri.toString());
      var signhashed = base64encode(uri.toString());

      Log.d("upi_pay", "initiateTransaction URI: " + uri.toString() + "&sign=" + signhashed)

      //uri = Uri.parse(uriStr + "&sign=" + signhashed)

      Log.d("final_upi_pay", "final_initiateTransaction URI: " + uri.toString())

      val intent = Intent(Intent.ACTION_VIEW, uri)
      intent.setPackage(app)





      if (intent.resolveActivity(activity.packageManager) == null) {
        this.success("activity_unavailable")
        return
      }

      //activity.startActivityForResult(intent, requestCodeNumber)

      val chooser: Intent = Intent.createChooser(intent, "Pay with...")
      //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      activity.startActivityForResult(chooser, 1, null)
      //}

    } catch (ex: Exception) {
      Log.e("upi_pay", ex.toString())
      this.success("failed_to_open_app")
    }
  }

  private fun getInstalledUpiApps() {
    val uriBuilder = Uri.Builder()
    uriBuilder.scheme("upi").authority("pay")

    val uri = uriBuilder.build()
    val intent = Intent(Intent.ACTION_VIEW, uri)

    val packageManager = activity.packageManager

    try {
      val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

      // Convert the activities into a response that can be transferred over the channel.
      val activityResponse = activities.map {
        val packageName = it.activityInfo.packageName
        val drawable = packageManager.getApplicationIcon(packageName)

        val bitmap = getBitmapFromDrawable(drawable)
        val icon = if (bitmap != null) {
          encodeToBase64(bitmap)
        } else {
          null
        }

        mapOf(
                "packageName" to packageName,
                "icon" to icon,
                "priority" to it.priority,
                "preferredOrder" to it.preferredOrder
        )
      }

      result?.success(activityResponse)
      Log.d("result", result.toString())
      Log.d("activityResponse", activityResponse.toString())
    } catch (ex: Exception) {
      Log.e("upi_pay", ex.toString())
      result?.error("getInstalledUpiApps", "exception", ex)
    }
  }

  private fun encodeToBase64(image: Bitmap): String? {
    val byteArrayOS = ByteArrayOutputStream()
    image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOS)
    return encodeToString(byteArrayOS.toByteArray(), Base64.NO_WRAP)
  }

  private fun getBitmapFromDrawable(drawable: Drawable): Bitmap? {
    val bmp: Bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
    drawable.draw(canvas)
    return bmp
  }

  private fun success(o: String) {
    if (!hasResponded) {
      hasResponded = true
      result?.success(o)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    Log.d("resultcode", requestCode.toString()+ resultCode.toString() + data?.getStringExtra("response"))

    if (data != null) {
      try {
        val response = data.getStringExtra("response")!!
        this.success(response)
      } catch (ex: Exception) {
        this.success("invalid_response")
      }
    } else {
      this.success("user_cancelled")
    }

    if (requestCodeNumber == requestCode && result != null) {

    }
    return true
  }

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "upi_pay")
      val plugin = FlutterPluginGpayPlugin(registrar, channel)
      registrar.addActivityResultListener(plugin)
      channel.setMethodCallHandler(plugin)
    }
  }

  private fun hashString(type: String, input: String): String {
    val HEX_CHARS = "0123456789ABCDEF"
    val bytes = MessageDigest
            .getInstance(type)
            .digest(input.toByteArray())
    val result = StringBuilder(bytes.size * 2)

    bytes.forEach {
      val i = it.toInt()
      result.append(HEX_CHARS[i shr 4 and 0x0f])
      result.append(HEX_CHARS[i and 0x0f])
    }

    return result.toString()
  }

  private fun base64encode(input: String): String {

    val encodedString: String = encodeToString(input.toByteArray(), Base64.DEFAULT);
    

    return encodedString;
  }

  /*private fun hashString(type: String, input: String): String {
    val bytes = MessageDigest
            .getInstance(type)
            .digest(input.toByteArray())
    //return bytes.toString().toUpperCase()
    Log.d("bytes",bytes.toString())

  }*/

  /*fun isAppInstalled(packageName: String): Boolean {
    val pm = getPackageManager()
    try {
      pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
      return true;
    } catch (e: PackageManager.NameNotFoundException) {
      e.printStackTrace()
    }
    return false;
  }

  fun isAppUpiReady(packageName: String): Boolean {
    var appUpiReady = false
    val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
    val pm = getPackageManager()
    val upiActivities: List<ResolveInfo> = pm.queryIntentActivities(upiIntent, 0)
    for (a in upiActivities) {
      if (a.activityInfo.packageName == packageName) appUpiReady = true
    }
    return appUpiReady
  }*/
}
