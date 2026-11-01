package com.example.admin.features.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.example.admin.core.network.BackendConnection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import com.example.admin.core.icons.*
import com.example.admin.core.theme.*
import com.example.admin.core.onboarding.OnboardingArtwork
import com.example.admin.core.platform.*

private const val IsAdmin = true
private val JourneyInk=Color(0xFF0F172A)
private val JourneySky=Color(0xFFF5F8FC)
private val JourneyBlue=Color(0xFF2563EB)
private val JourneyMuted=Color(0xFF596A7F)
private data class Introduction(val title:String,val message:String,val promise:String)
private val introductions=listOf(Introduction("See the whole response.","Bring your people, missions and resources into one clear view, so the next decision starts with understanding.","Clarity for your community"),
Introduction("Bring the right people together.","Match skills to the work ahead. Give every team a clear assignment and the support to carry it through.","Coordinate with confidence"),
Introduction("Keep every team connected.","Share timely updates and thoughtful handovers, so the next shift can pick up exactly where you left off.","Continuity when it matters"))

/** Verified identity and saved profile are required before opening the live workspace. */
@Composable fun EntryJourney(onAuthenticated: () -> Unit, reducedMotion: Boolean = false) {
    val saved=remember { runCatching { readJourneyState() }.getOrNull() }
    var introSeen by remember { mutableStateOf(saved?.substringBefore('\n') == "1") }
    var draft by remember { mutableStateOf(SetupDraft()) }
    var route by rememberSaveable { mutableStateOf(if(introSeen) "choice" else "intro") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var actionCode by remember { mutableStateOf("") }
    var authenticatorCode by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var identity by remember { mutableStateOf<FirebaseIdentity?>(null) }
    var membership by remember { mutableStateOf<Membership?>(null) }
    var request by remember { mutableStateOf<Job?>(null) }
    val scope=rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val formScroll=rememberScrollState()
    LaunchedEffect(route,draft.step) { formScroll.scrollTo(0) }
    LaunchedEffect(error,status) { if(error != null || status != null) { delay(100);formScroll.animateScrollTo(formScroll.maxValue) } }
    fun persist() {
        runCatching { writeJourneyState((if(introSeen) "1" else "0") + "\n" + draft.encode()) }
            .onFailure { status="Your progress could not be saved on this device. You can still continue." }
    }
    fun go(next:String) { request?.cancel();busy=false;error=null;status=null;password="";confirmation="";authenticatorCode="";route=next }
    fun navigateBack() {
        when(route) {
            "setup" -> if(draft.step>0) { draft=draft.copy(step=draft.step-1); } else go("choice")
            "check","reset" -> go("forgot")
            "resetDone" -> go("login")
            "terms","privacy" -> go("signup")
            else -> go("choice")
        }
    }
    JourneyBackHandler(route !in listOf("choice","intro"), ::navigateBack)
    fun finishIntro() { introSeen=true;persist();go("choice") }
    suspend fun resolveAccess(person:FirebaseIdentity) {
        identity=person
        com.example.admin.features.subscriptions.SubscriptionServices.controller.identify(person.uid)
        if(!person.emailVerified) { route="verify";return }
        val access=IdentityServices.database.membership(person.idToken)
        membership=access
        // Approval and organization membership come from the database, never setup selections.
        route=if(access?.canEnter(IsAdmin)==true) { if(access.setupComplete) "ready" else "setup" } else "pending"
        draft=SetupDraft()
    }
    LaunchedEffect(Unit) {
        try {
            when (val session = IdentityServices.firebase.refreshIdentity()) {
                is IdentityResult.SignedIn -> resolveAccess(session.identity)
                else -> Unit
            }
        } catch (c: CancellationException) { throw c }
        catch (_: Exception) { error="Your account could not be loaded. Check your connection and sign in again." }
    }
    fun perform(action:suspend () -> IdentityResult, completeRoute:String?=null, signingUp:Boolean=false) {
        if(busy)return
        error=null;status=null;busy=true
        request=scope.launch {
            try {
                when(val result=action()) {
                    is IdentityResult.Error -> {
                        error=result.message
                        if(result.code=="MFA_REQUIRED") { password="";authenticatorCode="";route="mfa" }
                    }
                    is IdentityResult.SignedIn -> {
                        password="";confirmation=""
                        if(signingUp && !result.identity.emailVerified) {
                            identity=result.identity;route="verify"
                            val sent=IdentityServices.firebase.sendEmailVerification()
                            if(sent is IdentityResult.Error) error=sent.message
                        } else resolveAccess(result.identity)
                    }
                    IdentityResult.Completed -> if(completeRoute!=null) route=completeRoute else status="Request completed."
                    is IdentityResult.MfaEnrollmentReady -> status="Continue authenticator setup in Account security."
                }
            } catch(c:CancellationException) { throw c }
            catch(_:Exception) { error="We couldn’t connect. Check your connection and try again." }
            finally { busy=false }
        }
    }
    fun saveSetup() {
        
        val person=identity ?: run { go("login");return }
        if(busy)return
        busy=true;error=null
        request=scope.launch {
            try {
                membership=IdentityServices.database.saveSetup(person.idToken,draft.copy(displayName=name.ifBlank { person.email.substringBefore("@") }))
                route=if(membership?.canEnter(IsAdmin)==true) "ready" else "pending"
            } catch(c:CancellationException) { throw c }
            catch(_:Exception) { error="Your setup wasn’t saved. Try again when you’re connected." }
            finally { busy=false }
        }
    }
    LaunchedEffect(route) { if(route !in listOf("signup","login","reset")) { password="";confirmation="" } }
    if(route=="intro") {
        IntroductionPages(reducedMotion,::finishIntro,{ introSeen=true;persist();go("login") })
        return
    }
    if(route=="account") {
        Column(Modifier.fillMaxSize().background(JourneySky)) {
            TextButton(onClick={go("pending")},modifier=Modifier.safeDrawingPadding()) { Text("Back to workspace access") }
            Box(Modifier.weight(1f)) { LiveAccountScreen() }
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(JourneySky).safeDrawingPadding().imePadding()) {
        val desktop=maxWidth>=1000.dp
        Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.Center) {
        if(desktop) Column(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0F172A)).padding(48.dp),verticalArrangement=Arrangement.Center) {
            Text("ResQOps.",color=Color(0xFFE4D076),style=MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            Text("Lead with clarity.\nRespond with care.",color=Color.White,style=MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(24.dp))
            Text("One shared view of your people, missions and resources.",color=Color(0xFFBFDBFE),style=MaterialTheme.typography.titleLarge)
            OnboardingArtwork(0,Modifier.fillMaxWidth().aspectRatio(1.5f),reducedMotion)
        }
        Column(Modifier.widthIn(max=560.dp).fillMaxWidth().fillMaxHeight()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                if(route!="choice") GlassIconButton(OpsSymbol.Back,"Back",::navigateBack)
                Column(Modifier.weight(1f)) {
                    Text(androidx.compose.ui.text.buildAnnotatedString { append("ResQ"); pushStyle(androidx.compose.ui.text.SpanStyle(color=Color(0xFFE4D076))); append("Ops."); pop() },fontFamily=getOverlockFontFamily(),fontWeight=FontWeight.Bold,color=JourneyInk,fontSize=18.sp)
                    Text(if(IsAdmin) "For coordinators" else "For your community",color=JourneyMuted,style=MaterialTheme.typography.bodySmall)
                }
                if(route=="choice") TextButton({ go("intro") }) { Text("Our story") }
            }
            Column(Modifier.weight(1f).verticalScroll(formScroll).padding(horizontal=24.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                when(route) {
                    "choice" -> {
                        OnboardingArtwork(0,Modifier.fillMaxWidth().aspectRatio(1.8f).clip(RoundedCornerShape(24.dp)),reducedMotion)
                        JourneyTitle(if(IsAdmin) "Lead with clarity.\nRespond with care." else "Your next chapter\nstarts with people.")
                        JourneyBody(if(IsAdmin) "A shared place to plan the response, support your volunteers and keep everyone informed." else "Bring your skills, your kindness and a little time. We’ll help you find your place.")
                        JourneyBody("Your organization?s people, missions, resources and operational insights in one workspace.", small=true)
                        JourneyButton(if(IsAdmin) "Create a work account" else "Create an account",{ go("signup") })
                        JourneyButton("Log in",{ go("login") },secondary=true)
                        SocialChoices(busy) { provider -> perform({IdentityServices.firebase.signInWith(provider)}) }
                    }
                    "login","signup" -> {
                        val signup=route=="signup"
                        JourneyTitle(if(signup) (if(IsAdmin) "Make room for\nbetter coordination." else "A little about you.\nA lot of possibility.") else "Good to see\nyou again.")
                        JourneyBody(if(IsAdmin) "Use your corporate email. Your organisation confirms access to its workspace." else if(signup) "Create your account, then choose where and how you’d like to help." else "Log in to pick up where you left off.")
                        if(signup) JourneyField("Full name",name,{name=it},enabled=!busy)
                        JourneyField(if(IsAdmin) "Corporate email" else "Email address",email,{email=it},keyboard=KeyboardType.Email,enabled=!busy)
                        JourneyField("Password",password,{password=it},secret=true,enabled=!busy)
                        if(signup) {
                            JourneyBody("Use at least 8 characters. A longer, unique passphrase is a good choice.",small=true)
                            JourneyField("Confirm password",confirmation,{confirmation=it},secret=true,enabled=!busy)
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Checkbox(agreed,{agreed=it},enabled=!busy)
                                Text("I agree to the Terms and Privacy Policy.",Modifier.weight(1f),color=JourneyMuted,style=MaterialTheme.typography.bodyMedium)
                            }
                            Row { TextButton({uriHandler.openUri("https://resq-web-staging.up.railway.app/terms")}){Text("Terms")};TextButton({uriHandler.openUri("https://resq-web-staging.up.railway.app/privacy")}){Text("Privacy") } }
                            if(IsAdmin) JourneyBody("Creating an account does not grant administrative access. Your membership is checked by your organisation.",small=true)
                        } else TextButton({go("forgot")},Modifier.align(Alignment.End)){Text("Forgot password?")}
                        JourneyButton(if(busy) "Please wait…" else if(signup) "Create account" else "Log in",{
                            when {
                                signup && name.trim().length<2 -> error="Enter your full name."
                                !validEmail(email) -> error="Enter a valid email address."
                                signup && !validNewPassword(password) -> error="Choose a password of 8–128 characters."
                                !signup && password.isBlank() -> error="Enter your password."
                                signup && password!=confirmation -> error="The passwords don’t match."
                                signup && !agreed -> error="Review and accept the Terms and Privacy Policy to continue."
                                else -> { val submitted=password;perform({if(signup) IdentityServices.firebase.signUp(name.trim(),email.trim(),submitted) else IdentityServices.firebase.signIn(email.trim(),submitted)},signingUp=signup) }
                            }
                        },enabled=!busy)
                        SocialChoices(busy) {provider->perform({IdentityServices.firebase.signInWith(provider)})}
                        TextButton({go(if(signup) "login" else "signup")},Modifier.fillMaxWidth()) { Text(if(signup) "Already a member? Log in" else "New here? Create an account") }
                    }
                    "forgot" -> {
                        RecoveryEmblem(OpsSymbol.Mail)
                        JourneyTitle("Let’s get you\nback in.")
                        JourneyBody("Enter your ${if(IsAdmin) "corporate " else ""}email and we’ll help you reset your password.")
                        JourneyField("Email address",email,{email=it},keyboard=KeyboardType.Email,enabled=!busy)
                        JourneyButton("Send reset link",{if(!validEmail(email))error="Enter a valid email address." else perform({IdentityServices.firebase.sendPasswordReset(email.trim())},"check")},enabled=!busy)
                        TextButton({go("reset")}){Text("I already have a reset code")}
                        if(!IsAdmin) JourneyBody("If you joined with Google, use that sign-in button instead. Your provider manages its password.",small=true)
                    }
                    "check" -> {
                        RecoveryEmblem(OpsSymbol.Mail);JourneyTitle("Check your inbox.")
                        JourneyBody("If an account uses $email, you’ll receive a link to reset its password. Check your spam folder too.")
                        JourneyButton("I have a reset code",{go("reset")})
                        TextButton({perform({IdentityServices.firebase.sendPasswordReset(email.trim())})},enabled=!busy){Text("Send another link")}
                        TextButton({go("forgot")}){Text("Use a different email")}
                    }
                    "reset" -> {
                        RecoveryEmblem(OpsSymbol.Shield);JourneyTitle("A fresh start.")
                        JourneyBody("Paste the reset link from your email, then choose a new password. Reset links expire and can only be used once.")
                        JourneyField("Reset link or code",actionCode,{actionCode=it},enabled=!busy)
                        JourneyField("New password",password,{password=it},secret=true,enabled=!busy)
                        JourneyField("Confirm new password",confirmation,{confirmation=it},secret=true,enabled=!busy)
                        JourneyButton("Save new password",{
                            when { recoveryCode(actionCode).isBlank()->error="Paste a valid reset link or code from your email."; !validNewPassword(password)->error="Choose a password of 8–128 characters."; password!=confirmation->error="The passwords don’t match.";else->{val submitted=password;perform({IdentityServices.firebase.confirmPasswordReset(recoveryCode(actionCode),submitted)},"resetDone")} }
                        },enabled=!busy)
                    }
                    "resetDone" -> { RecoveryEmblem(OpsSymbol.Check);JourneyTitle("You’re ready\nto return.");JourneyBody("Your password was updated. Log in with your new password.");JourneyButton("Back to log in",{go("login")}) }
                    "mfa" -> {
                        RecoveryEmblem(OpsSymbol.Shield);JourneyTitle("Confirm it's you")
                        JourneyBody("Enter the six-digit code from your authenticator app.")
                        JourneyField("Authenticator code",authenticatorCode,{authenticatorCode=it.filter(Char::isDigit).take(6)},keyboard=KeyboardType.Number,enabled=!busy)
                        JourneyButton("Verify and continue",{
                            if(authenticatorCode.length!=6)error="Enter the six-digit code."
                            else { val submitted=authenticatorCode;authenticatorCode="";perform({IdentityServices.firebase.completeMfa(submitted)}) }
                        },enabled=!busy)
                        TextButton({go("login")},enabled=!busy) { Text("Return to sign-in") }
                    }
                    "verify" -> {
                        RecoveryEmblem(OpsSymbol.Mail);JourneyTitle("One small check.\nA safer community.")
                        JourneyBody("Verify ${identity?.email ?: email} using the link in your email before continuing.")
                        JourneyButton("I’ve verified my email",{perform({IdentityServices.firebase.refreshIdentity()})},enabled=!busy)
                        TextButton({perform({IdentityServices.firebase.sendEmailVerification()})},enabled=!busy){Text("Send a new verification email")}
                    }
                    "pending" -> {
                        RecoveryEmblem(OpsSymbol.Shield);JourneyTitle(if(IsAdmin) "Your organisation\nhas the next step." else "Let’s connect you\nto your community.")
                        JourneyBody(if(IsAdmin) "Your identity and your organisation’s access approval are separate. A coordinator must confirm your database membership before this workspace opens." else "Your identity is signed in. Your community membership still needs to be confirmed before live missions can open.")
                        JourneyButton("Check access again",{perform({IdentityServices.firebase.refreshIdentity()})},enabled=!busy)
                        JourneyButton("Prepare my setup",{draft=SetupDraft();go("setup")},secondary=true,enabled=!busy)
                        TextButton({go("account")},enabled=!busy) { Text("Account and security") }
                        TextButton({scope.launch {IdentityServices.firebase.signOut();identity=null;membership=null;go("login")}}){Text("Use another account")}
                    }
                    "setup" -> {
                        SetupProgress(draft.step)
                        if(IsAdmin) AdminSetup(draft,{draft=it;}) else VolunteerSetup(draft,{draft=it;})
                        JourneyButton(if(draft.step<2) "Continue" else "Save my setup",{
                            if(draft.step<2) {draft=draft.copy(step=draft.step+1);} else saveSetup()
                        },enabled=!busy)
                        if(draft.step<2) TextButton({draft=draft.copy(step=draft.step+1);},Modifier.fillMaxWidth()){Text("I’ll do this later")}
                    }
                    "ready" -> {
                        RecoveryEmblem(OpsSymbol.Check)
                        JourneyTitle("Your next chapter starts here.")
                        JourneyBody("Your profile is saved. You can update it any time in your account.")
                        SetupSummary(draft)
                        JourneyButton("Open my workspace",onAuthenticated)
                    }

                }
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Surface(color=Color(0xFFFFF1EF),shape=RoundedCornerShape(14.dp),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) { Text(it,Modifier.padding(16.dp),color=Color(0xFFB42318)) } }
                status?.let { Text(it,color=JourneyMuted,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite}) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    }
}

@Composable private fun IntroductionPages(reducedMotion:Boolean,onFinish:()->Unit,onLogin:()->Unit) {
    val pager=rememberPagerState(pageCount={3});val scope=rememberCoroutineScope()
    JourneyBackHandler(pager.currentPage>0) { scope.launch { if(reducedMotion)pager.scrollToPage(pager.currentPage-1) else pager.animateScrollToPage(pager.currentPage-1) } }
    BoxWithConstraints(Modifier.fillMaxSize().background(JourneySky).safeDrawingPadding()) {
        val graphicHeight=(maxHeight*.46f).coerceIn(180.dp,460.dp)
        val wide=maxWidth>760.dp
        Column(Modifier.widthIn(max=1120.dp).fillMaxWidth().fillMaxHeight().align(Alignment.Center)) {
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                if(pager.currentPage>0) GlassIconButton(OpsSymbol.Back,"Previous introduction",{scope.launch{if(reducedMotion)pager.scrollToPage(pager.currentPage-1)else pager.animateScrollToPage(pager.currentPage-1)}})
                Text(androidx.compose.ui.text.buildAnnotatedString { append("ResQ"); pushStyle(androidx.compose.ui.text.SpanStyle(color=Color(0xFFE4D076))); append(if(IsAdmin) "Ops." else "."); pop() },Modifier.weight(1f).padding(start=8.dp),fontFamily=getOverlockFontFamily(),fontWeight=FontWeight.Bold,color=JourneyInk)
                TextButton(onFinish){Text("Skip")}
            }
            HorizontalPager(pager,Modifier.weight(1f),beyondViewportPageCount=1,key={it}) { index ->
                val page=introductions[index]
                if(wide) Row(Modifier.fillMaxSize().padding(24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(32.dp)) {
                    OnboardingArtwork(index,Modifier.weight(1f).aspectRatio(1.25f).clip(RoundedCornerShape(28.dp)),reducedMotion)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)){JourneyTitle(page.title);JourneyBody(page.message);PromiseBadge(page.promise)}
                } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    OnboardingArtwork(index,Modifier.fillMaxWidth().height(graphicHeight),reducedMotion)
                    Column(Modifier.padding(horizontal=28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) { JourneyTitle(page.title);JourneyBody(page.message);PromiseBadge(page.promise) }
                }
            }
            Column(Modifier.widthIn(max=560.dp).fillMaxWidth().align(Alignment.CenterHorizontally).padding(horizontal=24.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
                    repeat(3) { index ->
                        val width by animateDpAsState(if(index==pager.currentPage) 28.dp else 8.dp,animationSpec=tween(if(reducedMotion)0 else 220))
                        Box(Modifier.padding(4.dp).height(7.dp).width(width).clip(RoundedCornerShape(8.dp)).background(if(index==pager.currentPage) JourneyBlue else Color(0xFFCAD6E6)))
                    }
                    Text("${pager.currentPage+1} of 3",Modifier.padding(start=12.dp),color=JourneyMuted,style=MaterialTheme.typography.labelMedium)
                }
                JourneyButton(if(pager.currentPage==2) "Find my place".let {if(IsAdmin) "Build a better response" else it} else "Continue",{
                    if(pager.currentPage==2)onFinish() else scope.launch{if(reducedMotion)pager.scrollToPage(pager.currentPage+1)else pager.animateScrollToPage(pager.currentPage+1)}
                })
                TextButton(onLogin,Modifier.fillMaxWidth()){Text("Already have an account? Log in")}
            }
        }
    }
}

@Composable private fun JourneyTitle(text:String) { Text(text,fontFamily=getOswaldFontFamily(),fontWeight=FontWeight.Medium,fontSize=32.sp,lineHeight=39.sp,color=JourneyInk) }
@Composable private fun JourneyBody(text:String,small:Boolean=false) { Text(text,fontFamily=getOverlockFontFamily(),fontSize=if(small)14.sp else 17.sp,lineHeight=if(small)21.sp else 25.sp,color=JourneyMuted) }
@Composable private fun PromiseBadge(text:String) { Surface(color=Color(0xFFE9F4F2),shape=RoundedCornerShape(12.dp)) { Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){OpsIcon(OpsSymbol.Check,Modifier.size(17.dp),Color(0xFF087F75));Text(text,fontFamily=getOverlockFontFamily(),color=Color(0xFF087F75),fontSize=13.sp)} } }
@Composable private fun RecoveryEmblem(symbol:OpsSymbol) { Surface(color=Color(0xFFE6EEFE),shape=RoundedCornerShape(24.dp)) { Box(Modifier.size(80.dp),contentAlignment=Alignment.Center){OpsIcon(symbol,Modifier.size(36.dp),JourneyBlue)} } }
@Composable private fun JourneyButton(text:String,onClick:()->Unit,secondary:Boolean=false,enabled:Boolean=true) {
    if(secondary) OutlinedButton(onClick,Modifier.fillMaxWidth().heightIn(min=54.dp),enabled=enabled,shape=RoundedCornerShape(16.dp),contentPadding=PaddingValues(16.dp)){Text(text,fontFamily=getOverlockFontFamily(),fontSize=17.sp,fontWeight=FontWeight.Bold)}
    else Button(onClick,Modifier.fillMaxWidth().heightIn(min=54.dp),enabled=enabled,shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=JourneyBlue),contentPadding=PaddingValues(16.dp)){Text(text,fontFamily=getOverlockFontFamily(),fontSize=17.sp,fontWeight=FontWeight.Bold)}
}
@Composable private fun JourneyField(label:String,value:String,onChange:(String)->Unit,secret:Boolean=false,keyboard:KeyboardType=KeyboardType.Text,enabled:Boolean=true) {
    var visible by remember {mutableStateOf(false)}
    OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),enabled=enabled,label={Text(label)},singleLine=true,shape=RoundedCornerShape(14.dp),visualTransformation=if(secret&&!visible)PasswordVisualTransformation()else VisualTransformation.None,keyboardOptions=KeyboardOptions(keyboardType=if(secret)KeyboardType.Password else keyboard,autoCorrectEnabled=if(secret)false else null),trailingIcon=if(secret)({TextButton({visible=!visible}){Text(if(visible)"Hide" else "Show",style=MaterialTheme.typography.labelSmall)}})else null)
}
@Composable private fun SocialChoices(busy:Boolean,onSelect:(SocialProvider)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){HorizontalDivider(Modifier.weight(1f));Text("or",color=JourneyMuted);HorizontalDivider(Modifier.weight(1f))}
    JourneyButton("Continue with Google",{onSelect(SocialProvider.Google)},secondary=true,enabled=!busy)
}
@Composable private fun SetupProgress(step:Int) {
    Text("A few things to make this yours · ${step+1} of 3",color=JourneyBlue,style=MaterialTheme.typography.labelMedium)
    LinearProgressIndicator(progress={(step+1)/3f},modifier=Modifier.fillMaxWidth(),color=JourneyBlue)
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun SetupChoices(options:List<String>,selected:Set<String>,onSelect:(String)->Unit) {
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {options.forEach{option->FilterChip(option in selected,{onSelect(option)},label={Text(option,fontFamily=getOverlockFontFamily())},shape=RoundedCornerShape(12.dp))}}
}
@Composable private fun VolunteerSetup(draft:SetupDraft,onChange:(SetupDraft)->Unit) {
    when(draft.step) {
        0->{RecoveryEmblem(OpsSymbol.Location);JourneyTitle("Start close\nto home.");JourneyBody("Which community would you like to support? You can change this later.");JourneyField("Town, city or region",draft.location,{onChange(draft.copy(location=it))});JourneyBody("Enter a place manually. We won’t request your precise location during setup.",small=true)}
        1->{RecoveryEmblem(OpsSymbol.People);JourneyTitle("What do you\nlove bringing?");JourneyBody("Choose a few interests or skills. You don’t need to be an expert to begin.");SetupChoices(listOf("Medical support","Food & supplies","Community care","Communications","Logistics","Learning new skills"),draft.choices){onChange(draft.copy(choices=if(it in draft.choices)draft.choices-it else draft.choices+it))};JourneyBody("These are self-reported interests, not verified credentials.",small=true)}
        else->{RecoveryEmblem(OpsSymbol.Clock);JourneyTitle("Make space\nat your pace.");JourneyBody("A sustainable commitment starts with what works for you.");SetupChoices(listOf("A few hours a month","A day each week","Occasional weekends","I’m exploring"),setOf(draft.availability)){onChange(draft.copy(availability=it))};UpdatePreference(draft,onChange)}
    }
}
@Composable private fun AdminSetup(draft:SetupDraft,onChange:(SetupDraft)->Unit) {
    when(draft.step) {
        0->{RecoveryEmblem(OpsSymbol.People);JourneyTitle("Give your response\na shared home.");JourneyBody("Tell us which organisation and team you’re joining.");JourneyField("Organisation",draft.organization,{onChange(draft.copy(organization=it))});JourneyField("Team or department",draft.team,{onChange(draft.copy(team=it))})}
        1->{RecoveryEmblem(OpsSymbol.Shield);JourneyTitle("Your role.\nA clear responsibility.");JourneyBody("Choose the role you’re requesting. Your organisation decides which permissions to grant.");SetupChoices(listOf("Coordinator","Team lead","Operations support"),setOf(draft.requestedRole)){onChange(draft.copy(requestedRole=it))};JourneyBody("This selection never grants access. Approval is checked against your organisation’s database membership.",small=true)}
        else->{RecoveryEmblem(OpsSymbol.Location);JourneyTitle("Keep your region\nin focus.");JourneyBody("Set a starting region and how you’d like to hear from your team.");JourneyField("Response region",draft.location,{onChange(draft.copy(location=it))});UpdatePreference(draft,onChange)}
    }
}
@Composable private fun UpdatePreference(draft:SetupDraft,onChange:(SetupDraft)->Unit) {
    Surface(color=Color.White,shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Keep me informed",fontWeight=FontWeight.Bold);JourneyBody("Team and opportunity updates. You can change this later.",small=true)};Switch(draft.updates,{onChange(draft.copy(updates=it))})}}
    JourneyBody("This saves a preference. Device notification permission is requested only when you enable notifications in the workspace.",small=true)
}
@Composable private fun SetupSummary(draft:SetupDraft) {
    Surface(color=Color.White,shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Your starting point",fontWeight=FontWeight.Bold,color=JourneyInk)
        if(IsAdmin){JourneyBody(draft.organization.ifBlank{"Organisation to be added"});JourneyBody("Requested role: ${draft.requestedRole}")}
        else JourneyBody(draft.choices.joinToString(" · ").ifBlank{"Interests to explore"})
        JourneyBody(draft.location.ifBlank{"Location to be added"});JourneyBody(if(draft.updates)"Updates preferred"else "Updates off")
    }}
}
