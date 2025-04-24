package think.auto.dev.common.ui.statusbar

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Pair
import io.opentelemetry.api.internal.GuardedBy

@Service
class ThinkAutoDevStatusService : ThinkAutoDevStatusListener, Disposable {
    private val lock = Any()

    @GuardedBy("lock")
    private var status = ThinkAutoDevStatus.Ready

    @GuardedBy("lock")
    private var message: String? = null

    init {
        ApplicationManager.getApplication().messageBus
            .connect(this)
            .subscribe(ThinkAutoDevStatusListener.TOPIC, this)
    }

    override fun dispose() {

    }

    private fun getStatus(): Pair<ThinkAutoDevStatus, String?> {
        synchronized(lock) { return Pair.create(status, message) }
    }

    override fun onCopilotStatus(status: ThinkAutoDevStatus, customMessage: String?) {
        synchronized(lock) {
            this.status = status
            message = customMessage
        }

        updateAllStatusBarIcons()
    }

    private fun updateAllStatusBarIcons() {
        val action = Runnable {
            ProjectManager.getInstance().openProjects
                .filterNot { it.isDisposed }
                .forEach { ThinkAutoDevStatusBarWidget.update(it) }
        }

        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            action.run()
        } else {
            application.invokeLater(action)
        }
    }

    companion object {

        val currentStatus: Pair<ThinkAutoDevStatus, String?>
            get() = ApplicationManager.getApplication().getService(ThinkAutoDevStatusService::class.java).getStatus()

        @JvmOverloads
        fun notifyApplication(status: ThinkAutoDevStatus, customMessage: String? = null) {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(ThinkAutoDevStatusListener.TOPIC)
                .onCopilotStatus(status, customMessage)
        }

    }
}