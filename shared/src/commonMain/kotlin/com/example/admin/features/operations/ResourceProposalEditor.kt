package com.example.admin.features.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.*

@Composable internal fun ResourceProposalEditor(plan:JsonObject,stock:JsonArray,busy:Boolean,name:(String)->String,onCancel:()->Unit,onSave:(JsonObject,String)->Unit) {
    val allocations=(plan["allocations"] as? JsonArray).orEmpty().map{it.jsonObject}
    var included by remember{mutableStateOf(allocations.map{it.text("user_id")}.toSet())}
    var quantities by remember{mutableStateOf(allocations.flatMap{a->(a["supplies"] as? JsonArray).orEmpty().map{s->"${a.text("user_id")}:${s.jsonObject.text("resource_id")}" to s.jsonObject.text("quantity")}}.toMap())}
    var reason by remember{mutableStateOf("")}
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Adjust proposed allocations",style=MaterialTheme.typography.titleLarge)
        Text("Remove an allocation or change its quantities. Recompute to consider different applicants or requirements.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        allocations.forEach { a ->val user=a.text("user_id")
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(user in included,{included=if(it)included+user else included-user},enabled=!busy);Text(name(user),style=MaterialTheme.typography.titleSmall)}
            if(user in included)(a["supplies"] as? JsonArray).orEmpty().forEach { item->val supply=item.jsonObject;val id=supply.text("resource_id");val key="$user:$id"
                val resource=stock.firstOrNull{it.jsonObject.text("id")==id}?.jsonObject
                OutlinedTextField(quantities[key].orEmpty(),{quantities=quantities+(key to it.take(7))},label={Text(resource?.text("title").orEmpty().ifBlank{"Resource"})},supportingText={Text("${resource?.text("unit").orEmpty()} · ${resource?.text("location").orEmpty()} · 0 removes this item")},enabled=!busy,modifier=Modifier.fillMaxWidth().semantics { contentDescription = "Quantity: ${resource?.text("title").orEmpty()} for ${name(user)}" },singleLine=true)
            }
        }
        OutlinedTextField(reason,{reason=it.take(1500)},label={Text("Reason for revision")},enabled=!busy,minLines=2,modifier=Modifier.fillMaxWidth().semantics { contentDescription = "Reason for revision" })
        val valid=allocations.filter{it.text("user_id") in included}.all{a->(a["supplies"] as? JsonArray).orEmpty().all{s->quantities["${a.text("user_id")}:${s.jsonObject.text("resource_id")}"]?.toIntOrNull()?.let{it in 0..1000000}==true}}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(enabled=!busy&&valid&&reason.trim().length>=10,onClick={
                val revised=allocations.filter{it.text("user_id") in included}.map{a->JsonObject(a+("supplies" to JsonArray((a["supplies"] as? JsonArray).orEmpty().mapNotNull{item->val supply=item.jsonObject;val quantity=quantities["${a.text("user_id")}:${supply.text("resource_id")}"]!!.toInt();if(quantity==0)null else JsonObject(supply+("quantity" to JsonPrimitive(quantity)))})))}
                onSave(JsonObject(plan+("allocations" to JsonArray(revised))),reason)
            }){Text("Save revision")}
            TextButton(enabled=!busy,onClick=onCancel){Text("Cancel revision")}
        }
    }
}
