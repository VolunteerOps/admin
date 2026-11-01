@file:OptIn(kotlin.js.ExperimentalJsExport::class)
package com.example.admin.business
@JsExport fun validEmail(value: String) = OperationsPolicy.validEmail(value)
@JsExport fun canCoordinate(role: String, active: Boolean, approved: Boolean, organization: String) = OperationsPolicy.canCoordinate(role,active,approved,organization)
@JsExport fun recordStatuses(kind: String): Array<String> = OperationsPolicy.statuses(kind).toTypedArray()
@JsExport fun missionTransitions(status: String): Array<String> = OperationsPolicy.missionTransitions(status).toTypedArray()
@JsExport fun availableStock(quantity: Int, reserved: Int) = OperationsPolicy.availableStock(quantity,reserved)
@JsExport fun coverage(assigned: Int, capacity: Int) = OperationsPolicy.coverage(assigned,capacity)
@JsExport fun missionError(title: String, brief: String, capacity: Int, start: Double, end: Double, latitude: Double, longitude: Double) = OperationsPolicy.missionError(title,brief,capacity,start,end,latitude,longitude)
