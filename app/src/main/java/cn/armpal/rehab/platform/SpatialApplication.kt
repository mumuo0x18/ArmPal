package cn.armpal.rehab.platform

import android.app.Application
import cn.armpal.rehab.data.TrainingRepository
import cn.armpal.rehab.mainApp
import com.pico.spatial.ui.foundation.dsl.launch

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TrainingRepository.init(this)
        launch(::mainApp)
    }
}
