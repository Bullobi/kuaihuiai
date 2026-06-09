package io.github.xororz.localdream.data

import android.content.Context

class Preferences(context: Context) {
    // Stub implementation
    var backendPort: Int = 8081
    var captureLogs: Boolean = false
    var listenOnAllAddresses: Boolean = false
    var tagAutocomplete: Boolean = true
}
