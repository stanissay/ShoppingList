package stanissay.shopping.list

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import stanissay.shopping.list.utils.DeviceInfo

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DeviceInfo.init()
        OngoingManager.cancelIfExpired(applicationContext)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}