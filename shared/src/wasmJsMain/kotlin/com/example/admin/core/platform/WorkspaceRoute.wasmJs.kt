@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package com.example.admin.core.platform

import kotlin.JsFun

@JsFun("() => globalThis.location.hash.replace(/^#\\//, '')")
private external fun readRoute(): String
@JsFun("(route, replace) => { if(replace) globalThis.history.replaceState(null, '', '#/' + route); else globalThis.location.hash = '/' + route; }")
private external fun writeRoute(route:String,replace:Boolean)
actual fun readWorkspaceRoute(): String? = readRoute()
actual fun writeWorkspaceRoute(route:String,replace:Boolean) = writeRoute(route,replace)
