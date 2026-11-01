package com.example.admin.features.operations

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.viewinterop.WebElementView
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLIFrameElement
import com.example.admin.core.network.BackendConnection
@OptIn(ExperimentalComposeUiApi::class)
@Composable actual fun EvidenceMap(data:String,modifier:Modifier,onSelect:(String)->Unit){
 val selection by rememberUpdatedState(onSelect)
 val iframe=remember{(document.createElement("iframe") as HTMLIFrameElement).apply{src=BackendConnection.apiOrigin+"/sentinel/map-widget.html";title="Forecast and reported hazard map";style.border="0";setAttribute("referrerpolicy","no-referrer")}}
 LaunchedEffect(data){while(true){iframe.contentWindow?.postMessage(JSON.parse<dynamic>(data),BackendConnection.apiOrigin);delay(3000)}}
 DisposableEffect(iframe) {
  val listener:(Event)->Unit = { event ->
   val message=event.asDynamic()
   if(message.origin==BackendConnection.apiOrigin&&message.source==iframe.contentWindow&&message.data?.type=="resq.evidence-selection") {
    val id=message.data.id
    if(jsTypeOf(id)=="string"&&(id as String).length<=160)selection(id)
   }
  }
  window.addEventListener("message",listener)
  onDispose{window.removeEventListener("message",listener)}
 }
 WebElementView(factory={iframe},modifier=modifier)
}
