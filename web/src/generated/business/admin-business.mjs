import {
  THROW_CCE2g6jy02ryeudk as THROW_CCE,
  isCharSequence1ju9jr1w86plq as isCharSequence,
  trim11nh7r46at6sx as trim,
  toString1pkumu07cwy4m as toString,
  Regex_init_$Create$314aplwie2316 as Regex_init_$Create$,
  protoOf180f3jzyo7rfj as protoOf,
  isBlank1dvkhjjvox3p0 as isBlank,
  setOf45ia9pnfhe90 as setOf,
  listOf1jh22dvmctj1r as listOf,
  emptyList1g2z5xcrvp2zy as emptyList,
  coerceAtLeast2bkz8m9ik7hep as coerceAtLeast,
  numberToInt1ygmcfwhs2fkq as numberToInt,
  coerceIn10f36k81le1mm as coerceIn,
  isFinite2t9l5a275mxm6 as isFinite,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  copyToArray2j022khrow2yi as copyToArray,
} from './kotlin-kotlin-stdlib.mjs';
//region block: imports
//endregion
//region block: pre-declaration
initMetadataForObject(OperationsPolicy, 'OperationsPolicy');
//endregion
function OperationsPolicy() {
}
protoOf(OperationsPolicy).n4 = function (value) {
  // Inline function 'kotlin.text.trim' call
  // Inline function 'kotlin.let' call
  var it = toString(trim(isCharSequence(value) ? value : THROW_CCE()));
  return it.length <= 254 && Regex_init_$Create$('^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$').c4(it);
};
protoOf(OperationsPolicy).o4 = function (role, active, approved, organization) {
  var tmp;
  var tmp_0;
  if (active && approved) {
    // Inline function 'kotlin.text.isNotBlank' call
    tmp_0 = !isBlank(organization);
  } else {
    tmp_0 = false;
  }
  if (tmp_0) {
    var tmp_1 = setOf(['ORG_ADMIN', 'COORDINATOR', 'TEAM_LEAD']);
    // Inline function 'kotlin.text.uppercase' call
    // Inline function 'kotlin.js.asDynamic' call
    var tmp$ret$1 = role.toUpperCase();
    tmp = tmp_1.l(tmp$ret$1);
  } else {
    tmp = false;
  }
  return tmp;
};
protoOf(OperationsPolicy).p4 = function (kind) {
  switch (kind) {
    case 'MISSION':
      return listOf(['DRAFT', 'STAGED', 'ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED']);
    case 'APPLICATION':
      return listOf(['APPLIED', 'WAITLISTED', 'ACCEPTED', 'WITHDRAWN']);
    case 'ASSIGNMENT':
      return listOf(['ASSIGNED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED']);
    case 'REPORT':
      return listOf(['SUBMITTED', 'REVIEWING', 'ACTION_REQUIRED', 'RESOLVED']);
    case 'HOURS':
      return listOf(['PENDING', 'APPROVED']);
    case 'COURSE':
      return listOf(['DRAFT', 'PUBLISHED']);
    case 'ENROLLMENT':
      return listOf(['IN_PROGRESS', 'AWAITING_REVIEW', 'COMPLETED']);
    case 'CREDENTIAL':
      return listOf(['VERIFIED', 'REVOKED']);
    case 'SUPPORT':
      return listOf(['OPEN', 'IN_PROGRESS', 'RESOLVED']);
    case 'BULLETIN':
      return listOf(['PUBLISHED', 'WITHDRAWN']);
    default:
      return emptyList();
  }
};
protoOf(OperationsPolicy).q4 = function (status) {
  switch (status) {
    case 'DRAFT':
      return listOf(['STAGED', 'CANCELLED']);
    case 'STAGED':
      return listOf(['ACTIVE', 'CANCELLED']);
    case 'ACTIVE':
      return listOf(['PAUSED', 'COMPLETED', 'CANCELLED']);
    case 'PAUSED':
      return listOf(['ACTIVE', 'CANCELLED']);
    default:
      return emptyList();
  }
};
protoOf(OperationsPolicy).r4 = function (quantity, reserved) {
  return coerceAtLeast(quantity - reserved | 0, 0);
};
protoOf(OperationsPolicy).s4 = function (assigned, capacity) {
  return capacity <= 0 ? 0 : coerceIn(numberToInt(100.0 * assigned / capacity), 0, 100);
};
protoOf(OperationsPolicy).t4 = function (title, brief, capacity, start, end, latitude, longitude) {
  var tmp;
  // Inline function 'kotlin.text.trim' call
  var containsArg = toString(trim(isCharSequence(title) ? title : THROW_CCE())).length;
  if (!(3 <= containsArg ? containsArg <= 160 : false)) {
    tmp = 'Use a mission title of 3\u2013160 characters.';
  } else {
    // Inline function 'kotlin.text.trim' call
    var containsArg_0 = toString(trim(isCharSequence(brief) ? brief : THROW_CCE())).length;
    if (!(10 <= containsArg_0 ? containsArg_0 <= 5000 : false)) {
      tmp = 'Add a brief of at least 10 characters.';
    } else {
      if (!(1 <= capacity ? capacity <= 1000 : false)) {
        tmp = 'Capacity must be between 1 and 1,000.';
      } else {
        if (!isFinite(start) || !isFinite(end) || end <= start || end - start > 86400000) {
          tmp = 'Choose an end after the start, within 24 hours.';
        } else {
          if (!isFinite(latitude) || !(-90.0 <= latitude ? latitude <= 90.0 : false) || !isFinite(longitude) || !(-180.0 <= longitude ? longitude <= 180.0 : false)) {
            tmp = 'Choose valid coordinates for the meeting point.';
          } else {
            tmp = '';
          }
        }
      }
    }
  }
  return tmp;
};
var OperationsPolicy_instance;
function OperationsPolicy_getInstance() {
  return OperationsPolicy_instance;
}
function validEmail(value) {
  return OperationsPolicy_instance.n4(value);
}
function canCoordinate(role, active, approved, organization) {
  return OperationsPolicy_instance.o4(role, active, approved, organization);
}
function recordStatuses(kind) {
  // Inline function 'kotlin.collections.toTypedArray' call
  var this_0 = OperationsPolicy_instance.p4(kind);
  return copyToArray(this_0);
}
function missionTransitions(status) {
  // Inline function 'kotlin.collections.toTypedArray' call
  var this_0 = OperationsPolicy_instance.q4(status);
  return copyToArray(this_0);
}
function availableStock(quantity, reserved) {
  return OperationsPolicy_instance.r4(quantity, reserved);
}
function coverage(assigned, capacity) {
  return OperationsPolicy_instance.s4(assigned, capacity);
}
function missionError(title, brief, capacity, start, end, latitude, longitude) {
  return OperationsPolicy_instance.t4(title, brief, capacity, start, end, latitude, longitude);
}
//region block: init
OperationsPolicy_instance = new OperationsPolicy();
//endregion
//region block: exports
export {
  validEmail as validEmail,
  canCoordinate as canCoordinate,
  recordStatuses as recordStatuses,
  missionTransitions as missionTransitions,
  availableStock as availableStock,
  coverage as coverage,
  missionError as missionError,
};
//endregion

//# sourceMappingURL=admin-business.mjs.map
