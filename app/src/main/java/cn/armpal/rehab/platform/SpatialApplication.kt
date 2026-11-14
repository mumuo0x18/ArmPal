package cn.armpal.rehab.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import cn.armpal.rehab.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
