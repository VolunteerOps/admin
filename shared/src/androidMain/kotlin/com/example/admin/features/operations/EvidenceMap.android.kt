package com.example.admin.features.operations

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.admin.core.network.BackendConnection
@SuppressLint("SetJavaScriptEnabled")
@Composable actual fun EvidenceMap(data:String,modifier:Modifier,onSelect:(String)->Unit){
 val current by rememberUpdatedState(data)
 val script="window.postMessage("+data+",location.origin)"
 AndroidView(modifier=modifier,factory={context->WebView(context).apply{
  settings.javaScriptEnabled=true;settings.allowFileAccess=false;settings.allowContentAccess=false;settings.domStorageEnabled=false
  webViewClient=object:WebViewClient(){
   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
    if(request.url.toString().startsWith(BackendConnection.apiOrigin+"/sentinel/"))return false
    if(request.url.scheme=="https")runCatching{context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,request.url))}
    return true
   }
   override fun onPageFinished(view:WebView,url:String){view.evaluateJavascript("window.postMessage("+current+",location.origin)",null)}
  }
  loadUrl(BackendConnection.apiOrigin+"/sentinel/map-widget.html")
 }},update={it.evaluateJavascript(script,null)},onRelease={it.stopLoading();it.destroy()})
}
