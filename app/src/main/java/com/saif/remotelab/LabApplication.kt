package com.saif.remotelab

import android.app.Application
import com.saif.remotelab.data.LabDatabase
import com.saif.remotelab.data.LabRepository

class LabApplication : Application() {
    val repository: LabRepository by lazy { LabRepository(LabDatabase.create(this).dao()) }
}
