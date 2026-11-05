/** User-facing recovery text. Raw provider errors and internal codes stay out of the UI. */
export const errorGroups: [string[], string][] = [
[["MISSION_NOT_CLOSED"],"Complete or cancel the mission before preparing its after-action summary."],
[["DRAFT_CONFLICT"],"Another device changed this draft. Your local edits are safe. Reload to review the latest saved copy before saving again."],
[["CANCELLED_BY_COORDINATOR"],"This run was cancelled. Start a new request when you are ready."],
[["FOLLOW_UP_UNAVAILABLE","FOLLOW_UP_UNSUPPORTED"],"Choose a completed analysis that supports follow-up questions, or start a new request."],
[["BASE_PROPOSAL_MISMATCH"],"Choose a completed resource proposal for the same mission before comparing scenarios."],
[["ASSIGNEE_UNAVAILABLE"],"Choose an active workspace member who has access to this region."],
[["AGENT_REQUEST_DECLINED"],"Muse declined this request, so no assessment was produced. Contact support if this was an ordinary weather or mission-planning question."],
[["AGENT_STEP_LIMIT","AGENT_TOOL_BUDGET"],"Sentinel reached its analysis limit before finishing. Try a more focused question or a shorter date range."],
[["AGENT_TIMEOUT"],"This analysis took too long to finish. Try a shorter date range or ask again shortly."],
[["AGENT_UNSUPPORTED_ANSWER","AGENT_EVIDENCE_REQUIRED"],"Sentinel could not verify its answer against the information retrieved. No assessment was published. Please try again."],
[["AGENT_FINISH_REQUIRED","AGENT_OUTPUT_REQUIRED","AGENT_RESPONSE_INVALID"],"Sentinel collected information but could not finish a readable answer. Please try again."],
[["AGENT_TOOL_NOT_ALLOWED"],"Sentinel requested an unsupported action, so the analysis was stopped. No changes were made."],
[["PROMO_UNAVAILABLE"],"This promotion code is expired, already used, or unavailable for your account. Check the code or contact the person who shared it."],
[["CHECKOUT_REQUIRED"],"Apply this discount at checkout. Pro starts after payment is confirmed."],
[["CHECKOUT_NOT_CONFIGURED"],"Online checkout is not available yet. You can keep using Standard or redeem an eligible no-charge promotion."],
[["CAMPAIGN_ARCHIVED"],"This campaign is archived. Create a new campaign to plan more preparation tasks."],
[["MODEL_UNAVAILABLE"],"Muse is not accepting requests from our server right now. Your message is saved here. Please try again later; if this continues, contact support."],
[["STREAM_INTERRUPTED"],"The connection ended before the answer arrived. Your message is still here. Try again to check for the completed answer."],
[["META_ACCESS_DENIED"],"The assistant could not connect with the current service credentials. Ask your administrator to check the Meta connection."],
[["TOKEN_EXPIRED","UNAUTHORIZED","SESSION_REVOKED","INVALID_FIREBASE_IDENTITY","FIREBASE_AUTH_REQUIRED"],"Your sign-in has expired. Sign in again to continue."],
[["RECENT_AUTH_REQUIRED"],"Confirm your password to make this change. Your work will stay here."],
[["EMAIL_VERIFICATION_REQUIRED"],"Check your inbox and verify your email address before continuing."],
[["MEMBERSHIP_REQUIRED","SCOPE_REQUIRED","CAPABILITY_REQUIRED","PLATFORM_GRANT_REQUIRED","SUPPORT_GRANT_REQUIRED","RESPONDER_SCOPE_REQUIRED","GRANT_EXCEEDS_AUTHORITY"],"Your account does not have permission to do this. Ask a workspace administrator for access."],
[["ORG_NOT_ACTIVE","ENROLLMENT_PENDING"],"This workspace is not ready for operations yet. Ask its administrator to check its approval status."],
[["VERSION_CONFLICT","STALE_VERSION","ASSESSMENT_CHANGED","ASSESSMENT_POLICY_CHANGED","STREAM_RESNAPSHOT_REQUIRED"],"This information changed since you opened it. Refresh, review the latest details, and try again."],
[["IDEMPOTENCY_CONFLICT","REPLAY_CONFLICT"],"This request has already been used for a different change. Check the latest record before submitting again."],
[["AGENT_DAILY_LIMIT","INGRESS_LIMIT","INVITATION_LIMIT","JOIN_REQUEST_LIMIT","SHARE_LIMIT"],"Your workspace has reached its limit for now. Try later or ask an administrator to review usage."],
[["WEATHER_SOURCE_UNAVAILABLE","GCS_READ_FAILED","PLACE_SEARCH_UNAVAILABLE","META_NOT_CONFIGURED","AGENT_UNAVAILABLE","ANALYSIS_FAILED"],"The assistant could not finish this request. Your existing records have not changed. Try again shortly; if this continues, contact support."],
[["WEATHERNEXT_NOT_CONFIGURED","GCS_NOT_CONFIGURED"],"Weather data access is not enabled for this service yet. Ask your administrator to finish connecting the weather data."],
[["FORECAST_QUERY_BUDGET_EXCEEDED"],"This weather search is larger than the workspace's spending limit. Try a smaller area or ask an administrator to review the limit."],
[["WEATHER_ACCESS_DENIED","GCS_ACCESS_DENIED"],"ResQ cannot read this weather data yet. Ask your administrator to finish setting up access with the weather provider."],
[["WEATHER_BILLING_REQUIRED"],"Google could not run this weather search because billing is not ready or a usage limit has been reached. Ask your administrator to check the Google Cloud account."],
[["FORECAST_RUN_UNAVAILABLE"],"No forecast was found for that time and area. Choose the latest forecast option instead."],
[["STALE_FORECAST_RUN","ASSESSMENT_STALE"],"This forecast is too old for current planning. Get the latest forecast and run the assessment again."],
[["PROPOSAL_EXPIRED"],"This plan is too old to approve. Generate a new plan so availability and equipment can be checked again."],
[["AGENT_IN_PROGRESS"],"Your previous request is still being prepared. Wait a moment, then try again to retrieve its result."],
[["ANALYSIS_INTERRUPTED"],"That request stopped before it finished. Try again."],
[["EMPTY_PROPOSAL"],"Add at least one volunteer allocation before approving this plan."],
[["DISPATCH_PAUSED"],"Sending teams into the field is paused for this region. Ask a coordinator to resume it before assigning a response."],
[["STOCK_UNAVAILABLE","STOCK_LIMIT"],"There is not enough available stock for this change. Check existing reservations or reduce the quantity."],
[["RESOURCE_MISMATCH"],"These stock items use different types or units. Choose matching items for the transfer."],
[["RESPONDER_UNAVAILABLE","ASSIGNMENT_EXISTS"],"This volunteer already has a commitment that prevents this assignment. Choose another person or time."],
[["CAPACITY_REACHED","COURSE_FULL"],"There are no spaces left. Increase capacity if appropriate, or choose another activity."],
[["QUALIFICATION_REQUIRED","PREREQUISITE_REQUIRED","COURSE_PREREQUISITE"],"The required training or qualifications are not complete. Review the person's qualifications before continuing."],
[["MISSION_CLOSED","MISSION_NOT_ACTIVE","INVALID_TRANSITION","COURSE_STATE","APPLICATION_NOT_WITHDRAWABLE"],"This action is not available at the activity's current stage. Refresh and check its status."],
[["ASSIGNMENTS_OPEN"],"Some assignments are still open. Finish or cancel them before closing the mission."],
[["ASSIGNMENT_WINDOW"],"This action is outside the mission's scheduled time. Check the mission dates."],
[["APPLICATION_REQUIRED"],"This volunteer must apply to the mission before they can be assigned."],
[["APPLICATION_EXISTS","ENROLLMENT_EXISTS","MEMBERSHIP_EXISTS"],"This person is already registered. Refresh to see their existing record."],
[["SPONSOR_EXISTS"],"This organisation already has a sponsorship record. Refresh and review the existing sponsorship."],
[["APPLICATION_LIMIT","SEAT_LIMIT"],"The available places have been used. Ask your administrator to review capacity."],
[["MESSAGE_REMOVED","THREAD_UNAVAILABLE","CASE_RESOLVED","CASE_REPLY_LIMIT"],"This conversation can no longer accept this change. Check its status or open a new support case."],
[["LESSON_ORDER","REVIEW_NOT_READY"],"Complete the earlier learning steps before continuing."],
[["ASSESSMENT_COOLDOWN","ASSESSMENT_UNAVAILABLE"],"This training assessment is not available yet. Check the course instructions and try when the waiting period ends."],
[["LAST_OWNER_REQUIRED","LAST_ADMIN_REQUIRED","HANDOVER_REQUIRED"],"Assign another active administrator before removing this administrator's access."],
[["INVITATION_EXPIRED","INVITATION_CLOSED","INVITATION_UNAVAILABLE","INVITATION_ALREADY_ACCEPTED"],"This invitation has expired, was already used, or is no longer available. Check your membership or ask for a new invitation."],
[["DOMAIN_DISPUTED","DOMAIN_CLAIM_CONFLICT"],"Another workspace has registered this email address ending. Ask support to check which organisation owns it."],
[["DOMAIN_PROOF_UNAVAILABLE","DNS_PROOF_UNAVAILABLE","DOMAIN_PROOF_MISMATCH"],"We could not confirm ownership of this website address. Ask the person who manages your website to add the verification record shown in the setup instructions, then try again."],
[["PRO_REQUIRED","VOLUNTEER_MEMBERSHIP_REQUIRED"],"This feature is not included in the current account access. Check your membership and subscription."],
[["BILLING_NOT_CONFIGURED"],"Subscription checks are not set up yet, so Pro features cannot open. Ask your administrator to connect ResQOps billing. Your message has been kept."],
[["BILLING_UNAVAILABLE"],"We could not check your subscription right now. Try again in a moment. Your message has been kept."],
[["EMAIL_NOT_CONFIGURED","SSO_PROVIDER_NOT_CONFIGURED","STORAGE_NOT_CONFIGURED","STREAM_NOT_CONFIGURED"],"This service has not been connected or is temporarily unavailable. Ask your workspace administrator to check its setup."],
[["STORAGE_QUOTA_EXCEEDED"],"The workspace's file storage is full. Remove unneeded files or ask an administrator to increase storage."],
[["UPLOAD_EXPIRED","UPLOAD_NOT_FOUND","OBJECT_NOT_READY","OBJECT_MISMATCH"],"This upload is unavailable or incomplete. Choose the file again and retry the upload."],
[["CONSENT_REQUIRED","DEVICE_CONSENT_REQUIRED"],"Review and enable the required consent before using this feature."],
[["DEVICE_REVOKED","DEVICE_CONFLICT","DEVICE_LIMIT"],"This device cannot be registered right now. Review your registered devices or contact support."],
[["PROFILE_SHARE_REVOKED","RECRUITMENT_UNAVAILABLE","MEMBERSHIP_REMOVED","REQUEST_CLOSED"],"This access or request is no longer available. Refresh to review its current status."],
[["MFA_REQUIRED","SSO_REQUIRED"],"Your workspace requires a different sign-in method. Contact its administrator for help."],
[["WEATHER_READ_BUDGET","WEATHER_WINDOW_TOO_WIDE","METRICS_RANGE_TOO_LARGE","PLANNING_SCOPE_TOO_LARGE","FORECAST_REGION_REQUIRES_TILING","FORECAST_RESULT_REQUIRES_TILING","ANALYSIS_REQUIRES_SMALLER_REGION"],"This request covers too much information at once. Choose a smaller area or shorter date range."],
[["FORECAST_WINDOW_BEFORE_CYCLE"],"The analysis starts before this forecast was issued. Choose a later start date, or use Ask Sentinel to explore earlier days."],
[["FORECAST_CYCLE_FUTURE"],"This forecast has not been issued yet. Choose an earlier forecast time."],
[["FORECAST_HOUR_REQUIRED","INIT_MUST_BE_ON_HOUR"],"The minutes must be 00, for example 18:00 rather than 18:35. Choose a published forecast, or use the latest forecast option."],
[["INVALID_FORECAST_WINDOW","INVALID_ANALYSIS_WINDOW"],"Choose an end time after the start, with no more than two days in this detailed assessment. Ask Sentinel for a broader question."],
[["TENANT_MIGRATION_REQUIRED","IDENTITY_MIGRATION_REQUIRED","UNSAFE_RUNTIME_ROLE"],"This workspace needs a service update before you can continue. Contact support; changing this form will not fix it."],
[["AUTHORIZATION_UNAVAILABLE","IDENTITY_UNAVAILABLE"],"We could not check your account access right now. Try again shortly."],
[["FIREBASE_REVOCATION_FAILED"],"We could not sign out your other sessions. Try again, then check your signed-in devices."],
[["CSRF_REJECTED"],"This page can no longer confirm your request. Reload it, sign in if asked, and try again."],
[["IDENTITY_CONFLICT","IDENTITY_BINDING_REQUIRED"],"We could not match this sign-in to your workspace account. Contact support to check the account connection."],
[["BOOTSTRAP_ALREADY_USED","WORKSPACE_EXISTS"],"This workspace has already been set up. Refresh and open the existing workspace."],
[["CASE_OWNER_REQUIRED"],"Only the coordinator responsible for this case can make this change. Ask them to review it or hand it over to you."],
[["STORAGE_UNAVAILABLE","storage_not_configured"],"The file service is unavailable. Keep your file and try uploading it again later."],
[["JOB_NOT_RETRYABLE"],"This request cannot be restarted. Check its progress, or request a new forecast if it has finished."],
[["WEATHER_BUSY","WEATHER_RESULT_BUDGET"],"The weather service is handling too much information right now. Try a shorter period or try again in a few minutes."],
[["FORECAST_LOOKUP_FAILED"],"We could not find a recent published forecast for this area. Try again later, or check weather data access with your administrator."],
[["FORECAST_HORIZON_UNAVAILABLE"],"This forecast does not cover the whole period you chose. Choose a shorter period or use the latest forecast option."],
[["DUPLICATE_FORECAST_CELL","FORECAST_WINDOW_MISMATCH","INVALID_FORECAST_CELL","INVALID_FORECAST_STATISTICS"],"The weather data returned was incomplete or inconsistent. Request a new forecast; do not use this result for planning."],
[["REQUEST_TOO_LARGE","PAYLOAD_TOO_LARGE"],"This submission is too large. Send fewer items or choose a smaller file."],
[["MALFORMED_JSON","malformed_json","UNSUPPORTED_COMMAND"],"We could not read this submission. Reload the page and try again. Contact support if it keeps happening."],
[["UNIQUE_CONSTRAINT","unique_constraint","conflict"],"A matching record already exists or has changed. Refresh and check the existing record before trying again."],
[["unauthorized"],"Your sign-in has expired. Sign in again to continue."],
[["forbidden"],"Your account does not have access to this action. Ask a workspace administrator for help."],
[["not_found"],"This item is no longer available, or you do not have access to it. Refresh the page."],
[["internal_error"],"We could not complete this request. Check whether the change was saved before trying again. Contact support if it continues."],
];

export const friendlyErrors: Record<string,string> = Object.fromEntries(errorGroups.flatMap(([codes,message])=>codes.map(code=>[code,message])));
/** Only locally authored, already reviewed text can bypass the provider-error translator. */
export class UserFacingError extends Error {
 constructor(message:string){super(translatedValidation(message)||message);}
}

export const validationRules: [RegExp,string][] = [
 [/The minutes must be 00|Forecast initialization must be on a UTC hour|INIT_MUST_BE_ON_HOUR|times on the hour|issued UTC cycle/i, friendlyErrors.FORECAST_HOUR_REQUIRED],
 [/end hour after the start hour, covering no more than 48|forecast window of at most 48|INVALID_FORECAST_WINDOW/i, 'Choose an end hour after the start hour, covering no more than 48 hours. For example, start at hour 1 and end at hour 24 for one day.'],
 [/already-issued forecast initialization|Select a newer forecast cycle|Choose a recent forecast/i, 'Choose the latest forecast option. The date you entered is in the future or is too old to use for current planning.'],
 [/forecast has not been issued yet/i, 'This forecast has not been published yet. Use the latest forecast option instead.'],
 [/analysis starts before this forecast was issued/i, 'The period you want to check starts before this forecast was published. Choose a later start date, or use Ask Sentinel to look at earlier days.'],
 [/Mission duration must be positive|Choose an end after the start, within 24 hours/i, 'The mission must end after it starts and last no more than 24 hours. For a longer response, plan separate shifts.'],
 [/Bounds must enclose a nonempty area|valid coordinates for the meeting point/i, 'Choose a location on the map. For an operating region, use the city and country search to fill in the area.'],
 [/Exact public DNS domain required/i, 'Enter your organisation\'s website address, such as resq-app.co.ke, without https:// or a page name.'],
 [/file_name must not contain path separators/i, 'Use just the file name, without folders or slashes.'],
 [/Provision and select the reviewed workspace/i, 'Create and select the workspace before approving it.'],
 [/Two distinct organizations and one participating lead/i, 'Choose two different organisations and select one of them to lead the partnership.'],
 [/report cannot link to itself/i, 'Choose a different report to link to this one.'],
 [/Choose a different destination/i, 'Choose a different destination for the transfer.'],
 [/Unsupported command|Unsupported training action/i, 'This action is no longer available in this version of the page. Reload and try again.'],
 [/future expiry within five years/i, 'Choose an expiry date after today and within the next five years.'],
 [/Heartbeat timestamp is outside/i, 'The device update is too old or its clock is incorrect. Check the device time and send a new update.'],
 [/Invitation scopes must belong to this organization/i, 'Choose regions that belong to the selected workspace before sending the invitation.'],
 [/Use directory preferences to opt in/i, 'Open your directory preferences and choose whether other coordinators may find you.'],
 [/Choose an active member with access to this region/i, 'Choose a current team member who has access to this region.'],
 [/edited allocation exceeds the eligible candidates, capacity or stock/i, 'This plan asks for more eligible volunteers, spaces or equipment than are available. Reduce the numbers and review the plan again.'],
 [/Choose the receiving coordinator/i, 'Choose the coordinator who will take over this case.'],
 [/Coordinator is only accepted for handover/i, 'Choose Hand over if you want to transfer this case to another coordinator.'],
 [/Capacity must be between 1 and 1,000/i, 'Enter the number of volunteers needed, from 1 to 1,000.'],
];
export function translatedValidation(message: string) {
 return validationRules.find(([pattern])=>pattern.test(message))?.[1];
}

const fieldNames: Record<string,string> = {
 init_time:'forecast publication time',from:'start date and time',to:'end date and time',from_hour:'start hour',to_hour:'end hour',
 starts_at:'mission start',ends_at:'mission end',capacity:'number of places',reason:'reason for this decision',
 title:'title',name:'name',full_name:'full name',brief:'mission description',description:'description',body:'message',message:'message',
 email:'email address',password:'password',phone:'phone number',domain:'website address',url:'web address',
 longitude:'map location',latitude:'map location',bounds:'area on the map',west:'western edge',east:'eastern edge',north:'northern edge',south:'southern edge',
 heat_c:'heat warning level',wind_mps:'wind warning level',hourly_rain_mm:'rain warning level',maximum_run_age_hours:'maximum forecast age',
 quantity:'quantity',delta:'stock change',unit:'stock unit',meeting_point:'meeting point',required_skills:'required skills',skill:'skill',
 expires_at:'expiry date',start:'start date',end:'end date',hours:'number of hours',file_name:'file name',content_type:'file type',size_bytes:'file size',
 role:'workspace role',status:'status',region_id:'operating region',region_ids:'operating regions',coordinator_id:'receiving coordinator',
 volunteer_id:'volunteer',user_id:'team member',mission_id:'mission',course_id:'course',score:'score',answer:'answer',choices:'answer choices',
 question:'question',lessons:'lessons',prerequisites:'required courses',notes:'notes',text:'message',display_name:'display name',
};
const detailFields = new Set(['id','expected_version','version','checksum_sha256','organization_id','action','token']);
export function validationMessage(details: unknown, message='') {
 const translated=translatedValidation(message);
 if(!Array.isArray(details)||!details.length)return translated || 'Some information is missing or is not valid. Check the fields in this form and try again.';
 const lines=details.slice(0,3).map((item:any)=>{
  if(!item||typeof item!=="object")return "Check the information in this form and try again.";
  const known=translatedValidation(String(item?.message||''));if(known)return known;
  const path=Array.isArray(item?.path)?item.path:String(item?.path||'').split('.');
  const key=String(path.filter((x:any)=>!/^[0-9]+$/.test(String(x))).at(-1)||'');
  if(detailFields.has(key))return 'This form is out of date or is missing information needed to save. Refresh and choose the item again.';
  const name=fieldNames[key];if(!name)return 'Some information is missing or is not valid. Check the fields in this form and try again.';
  // Only known field labels and numeric bounds are displayed; provider text is never echoed.
  const min=typeof item.minimum==='number'&&Number.isFinite(item.minimum)?item.minimum:undefined;
  const max=typeof item.maximum==='number'&&Number.isFinite(item.maximum)?item.maximum:undefined;
  if(item.code==='too_small')return min!==undefined?`Enter ${name} with at least ${min}${item.type==='string'?' characters':item.type==='array'?' items':''}.`:`Add a valid ${name}; the current value is missing or too small.`;
  if(item.code==='too_big')return max!==undefined?`Keep ${name} within ${max}${item.type==='string'?' characters':item.type==='array'?' items':''}.`:`Reduce the ${name}; the current value is too large or too long.`;
  if(item.code==='invalid_type'&&item.expected==='integer')return `Enter a whole number for ${name}, such as 1 or 2, without a decimal point.`;
  if(key==='email')return 'Enter a complete email address, such as name@example.org.';
  if(/time|date/.test(name))return `Choose a valid ${name} using the date and time picker.`;
  if(item.code==='invalid_enum_value'||item.code==='invalid_value')return `Choose one of the available options for ${name}.`;
  return `Check the ${name} and enter a valid value.`;
 });return [...new Set(lines)].join(' ');
}

export function friendlyError(code:string,message:string,status=0,details?:unknown):string {
 code=String(code||"");message=typeof message==="string"?message:"";
 if(code==='validation_failed')return validationMessage(details,message);
 if(friendlyErrors[code])return friendlyErrors[code];
 if(code.startsWith('auth/')){
  const auth:Record<string,string>={
   'invalid-credential':'Check your email and password.', 'wrong-password':'Check your email and password.', 'user-not-found':'Check your email and password.',
   'invalid-email':'Enter a complete email address, such as name@example.org.',
   'email-already-in-use':'This email address already has an account. Sign in or use Forgot password.',
   'weak-password':'Choose a longer password that meets the requirements shown in the form.',
   'password-does-not-meet-requirements':'Your password does not meet the requirements shown in the form. Choose a stronger password.',
   'requires-recent-login':'Sign in again before changing your account.',
   'too-many-requests':'Too many attempts. Wait a few minutes before trying again.',
   'network-request-failed':'We could not reach the sign-in service. Check your connection and try again.',
   'expired-action-code':'This email link has expired. Request a new link and use the newest email.',
   'invalid-action-code':'This email link is no longer valid. Request a new link and use the newest email.',
   'user-disabled':'This account has been disabled. Contact your workspace administrator.',
   'operation-not-allowed':'This sign-in method is not available. Contact your administrator to check sign-in settings.',
   'invalid-verification-code':'The verification code was not accepted. Check the code and try again.',
   'code-expired':'This verification code has expired. Request a new code and try again.',
   'popup-closed-by-user':'The sign-in window closed before you finished. Open it again to continue.',
   'popup-blocked':'Your browser blocked the sign-in window. Allow pop-ups for this site and try again.',
   'unauthorized-domain':'Sign-in is not connected to this website address. Contact your administrator.',
  };return auth[code.slice(5)]||'We could not complete this account action. Try again, or contact support if it continues.';
 }
 if(/Failed to fetch|NetworkError|fetch failed|Load failed/i.test(message))return 'We could not connect. Check your internet connection and try again.';
 if(code==='AbortError'||/operation was aborted|signal timed out/i.test(message))return 'The request stopped before it finished. Check the latest record before trying again.';
 const known=translatedValidation(message);if(known)return known;
 if(status===401)return friendlyErrors.unauthorized;
 if(status===403)return friendlyErrors.forbidden;
 if(status===404)return friendlyErrors.not_found;
 if(status===409)return friendlyErrors.conflict;
 if(status===413)return friendlyErrors.REQUEST_TOO_LARGE;
 if(status===429)return 'Too many requests at once. Wait a moment and try again.';
 if(status>=500)return friendlyErrors.internal_error;
 if(status===400||status===422)return validationMessage(details);
 // Unknown provider messages are never safe user copy, regardless of their wording.
 return 'We could not complete this action. Check the latest record before trying again. Contact support if it continues.';
}
