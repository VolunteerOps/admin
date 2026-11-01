package com.example.admin.features.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException

private data class LessonDraft(val title:String="",val body:String="",val question:String="",val first:String="",val second:String="",val answer:Int=0)
@Composable internal fun CourseCreation(api:LiveApiTransport,path:String,enabled:Boolean,send:(JsonObject)->Unit){
 var prerequisites by remember(path){mutableStateOf(setOf<String>())}
 var title by remember{mutableStateOf("")};var skill by remember{mutableStateOf("")};var capacity by remember{mutableStateOf("20")};var lessons by remember{mutableStateOf(listOf(LessonDraft()))}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Create a training course",style=MaterialTheme.typography.titleLarge)
  Text("Assessment completion is reviewed by a coordinator before a qualification is issued.")
  OutlinedTextField(title,{title=it},label={Text("Course title")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(skill,{skill=it},label={Text("Qualification name")},modifier=Modifier.fillMaxWidth())
  Text("Prerequisite courses (optional, up to 10)")
  ScopedRecordPicker(api,path,"COURSE",prerequisites,10){prerequisites=it}
  OutlinedTextField(capacity,{capacity=it},label={Text("Enrollment capacity")})
  lessons.forEachIndexed{index,lesson->
   fun change(value:LessonDraft){lessons=lessons.mapIndexed{i,l->if(i==index)value else l}}
   Text("Lesson ${index+1}",style=MaterialTheme.typography.titleMedium)
   OutlinedTextField(lesson.title,{change(lesson.copy(title=it))},label={Text("Lesson title")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(lesson.body,{change(lesson.copy(body=it))},label={Text("Lesson content")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(lesson.question,{change(lesson.copy(question=it))},label={Text("Assessment question")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(lesson.first,{change(lesson.copy(first=it))},label={Text("First answer choice")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(lesson.second,{change(lesson.copy(second=it))},label={Text("Second answer choice")},modifier=Modifier.fillMaxWidth())
   Row{FilterChip(selected=lesson.answer==0,onClick={change(lesson.copy(answer=0))},label={Text("First is correct")});Spacer(Modifier.width(8.dp));FilterChip(selected=lesson.answer==1,onClick={change(lesson.copy(answer=1))},label={Text("Second is correct")})}
  }
  TextButton(enabled=lessons.size<20,onClick={lessons=lessons+LessonDraft()}){Text("Add lesson")}
  Button(enabled=enabled&&title.length>=3&&skill.length>=2&&(capacity.toIntOrNull()?:0)>0&&lessons.all{it.title.length>=3&&it.body.length>=20&&it.question.length>=10&&it.first.isNotBlank()&&it.second.isNotBlank()},onClick={send(buildJsonObject{put("action","CREATE_COURSE");put("title",title);put("skill",skill);put("capacity",capacity.toInt());put("prerequisites",buildJsonArray{prerequisites.forEach{add(it)}});put("lessons",buildJsonArray{lessons.forEach{l->add(buildJsonObject{put("title",l.title);put("body",l.body);put("question",l.question);put("choices",buildJsonArray{add(l.first);add(l.second)});put("answer",l.answer)})}})})}){Text("Save course draft")}
 }
}
@Composable internal fun CourseStudy(api:LiveApiTransport,path:String,enrollment:JsonObject,enabled:Boolean,send:(JsonObject)->Unit){
 var course by remember(enrollment["id"]){mutableStateOf<JsonObject?>(null)};var answer by remember(enrollment["version"]){mutableStateOf<Int?>(null)};var error by remember{mutableStateOf("")}
 LaunchedEffect(path,enrollment["id"]){try{course=api.execute(ApiEndpoint(HttpMethod.Get,path+"/records/"+enrollment["parentId"]!!.jsonPrimitive.content){it.jsonObject})}catch(c:CancellationException){throw c}catch(_:Exception){error="Unable to load this course. Refresh and try again."}}
 if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
 val payload=enrollment["payload"]!!.jsonObject;val index=payload["next_lesson"]!!.jsonPrimitive.int
 val lesson=course?.get("payload")?.jsonObject?.get("lessons")?.jsonArray?.getOrNull(index)?.jsonObject
 if(lesson!=null){
  Text(lesson["title"]!!.jsonPrimitive.content,style=MaterialTheme.typography.titleLarge)
  Text(lesson["body"]!!.jsonPrimitive.content)
  Text(lesson["question"]!!.jsonPrimitive.content,style=MaterialTheme.typography.titleMedium)
  lesson["choices"]!!.jsonArray.forEachIndexed{i,choice->FilterChip(selected=answer==i,onClick={answer=i},label={Text(choice.jsonPrimitive.content)})}
  if(payload["last_answer_correct"]?.jsonPrimitive?.booleanOrNull==false)Text("Review the lesson before trying again. There is a 30-second pause between attempts.")
  Button(enabled=enabled&&answer!=null,onClick={send(buildJsonObject{put("action","SUBMIT_LESSON");put("id",enrollment["id"]!!);put("expected_version",enrollment["version"]!!);put("lesson",index);put("answer",answer!!)})}){Text("Submit answer")}
 }
}
@Composable internal fun TrainingReview(record:JsonObject,enabled:Boolean,send:(JsonObject)->Unit){
 var reason by remember(record["id"]){mutableStateOf("")};var expiry by remember(record["id"]){mutableStateOf("")}
 OutlinedTextField(reason,{reason=it},label={Text("Assessment and practical review notes")},modifier=Modifier.fillMaxWidth())
 OutlinedTextField(expiry,{expiry=it},label={Text("Qualification expiry (UTC, e.g. 2027-10-01T00:00:00Z)")},modifier=Modifier.fillMaxWidth())
 Row{listOf(true,false).forEach{approved->TextButton(enabled=enabled&&reason.length>=10&&expiry.isNotBlank(),onClick={send(buildJsonObject{put("action","REVIEW_TRAINING");put("id",record["id"]!!);put("expected_version",record["version"]!!);put("approved",approved);put("reason",reason);put("valid_until",expiry)})}){Text(if(approved)"Approve qualification" else "Request reassessment")}}}
}
@Composable internal fun CredentialRevocation(record:JsonObject,enabled:Boolean,send:(JsonObject)->Unit){
 var reason by remember(record["id"]){mutableStateOf("")}
 OutlinedTextField(reason,{reason=it},label={Text("Reason for revoking qualification")},modifier=Modifier.fillMaxWidth())
 TextButton(enabled=enabled&&reason.length>=10,onClick={send(buildJsonObject{put("action","REVOKE_CREDENTIAL");put("id",record["id"]!!);put("expected_version",record["version"]!!);put("reason",reason)})}){Text("Revoke qualification")}
}
