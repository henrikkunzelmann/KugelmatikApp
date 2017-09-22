package de.karlkuebelschule.kugelmatikapp;

import android.os.AsyncTask;

import java.net.InetAddress;
import java.net.UnknownHostException;

import de.karlkuebelschule.KugelmatikLibrary.ClusterInfo;
import de.karlkuebelschule.KugelmatikLibrary.Config;
import de.karlkuebelschule.KugelmatikLibrary.ErrorCode;
import de.karlkuebelschule.KugelmatikLibrary.IAddressProvider;
import de.karlkuebelschule.KugelmatikLibrary.Kugelmatik;
import de.karlkuebelschule.KugelmatikLibrary.StandardAddressProvider;
import de.karlkuebelschule.KugelmatikLibrary.StaticAddressProvider;

public class KugelmatikManager {
    private Kugelmatik kugelmatik;

    public synchronized void free() {
        if (kugelmatik != null) {
            kugelmatik.free();
            kugelmatik = null;
        }
    }

    public synchronized void loadKugelmatik() {
        load(5, 5, new StandardAddressProvider());
    }

    public synchronized void load(String host) throws UnknownHostException {
        load(1, 1, new StaticAddressProvider(InetAddress.getByName(host)));
    }

    private synchronized void load(int width, int height, final IAddressProvider provider) {
        free();

        Config.KugelmatikWidth = width;
        Config.KugelmatikHeight = height;
        Config.MaxHeight = 6000;


        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... data){
                try {
                    kugelmatik = new Kugelmatik(provider);
                    kugelmatik.sendPing();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute (Void result){

            }
        };
        task.execute();
    }

    public synchronized boolean isLoaded() {
        return kugelmatik != null;
    }

    public synchronized boolean isConnected() {
        if (kugelmatik == null)
            return false;

        return kugelmatik.isAnyClusterOnline();
    }

    public synchronized int getPing() {
        if (kugelmatik == null)
            return 0;
        return kugelmatik.getClusterByPosition(0, 0).getPing();
    }

    public synchronized int getVersion() {
        if (kugelmatik == null)
            return 0;
        ClusterInfo info = kugelmatik.getClusterByPosition(0, 0).getClusterInfo();
        if (info == null)
            return 0;
        return info.getBuildVersion();
    }

    public synchronized ErrorCode getError() {
        if (kugelmatik == null)
            return ErrorCode.UnknownError;
        ClusterInfo info = kugelmatik.getClusterByPosition(0, 0).getClusterInfo();
        if (info == null)
            return ErrorCode.UnknownError;
        return info.getLastErrorCode();
    }

    public synchronized void sendPing() {
        if (isLoaded())
            kugelmatik.sendPing();
    }

    public synchronized void sendStop() {
        if (isLoaded())
            kugelmatik.sendStop();
    }

    public synchronized void sendHome() {
        if (isLoaded())
            kugelmatik.getAllCluster()[0].sendHome();
    }

    public synchronized void setHeight(int height) {
        if (isLoaded()) {
            height = Math.min(Config.MaxHeight, Math.max(0, height));

            kugelmatik.setAllSteppers(height);
            kugelmatik.sendMovementData(false, true);
        }
    }

    public synchronized void setHeightPercentage(float height) {
        setHeight((int)Math.round(height * Config.MaxHeight));
    }

    public synchronized int getHeight() {
        if (!isLoaded())
            return 0;
        return kugelmatik.getStepperByPosition(0, 0).getHeight();
    }
}
