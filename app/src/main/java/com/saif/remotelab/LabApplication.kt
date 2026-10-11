package com.saif.remotelab

import android.app.Application
import com.saif.remotelab.data.LabDatabase
import com.saif.remotelab.data.LabRepository
import com.saif.remotelab.data.PlanPdfReader

class LabApplication : Application() {
    val repository: LabRepository by lazy { LabRepository(LabDatabase.create(this).dao()) }
    val pdfReader: PlanPdfReader by lazy { PlanPdfReader(this) }
}
