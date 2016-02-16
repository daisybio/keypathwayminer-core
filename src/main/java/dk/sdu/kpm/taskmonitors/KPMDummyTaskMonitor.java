package dk.sdu.kpm.taskmonitors;

/**
 * Created by: Martin
 * Date: 19-02-14
 */
public class KPMDummyTaskMonitor implements IKPMTaskMonitor{
    @Override
    public void setTitle(String title) {
        // Dummy
    }

    @Override
    public void setProgress(double progress) {
        // Dummy
    }

    @Override
    public void setStatusMessage(String statusMessage) {
        // Dummy
    }
}
