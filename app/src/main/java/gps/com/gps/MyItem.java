package gps.com.gps;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Класс, отвечающий за получение данных кластерами
 */

public class MyItem implements ClusterItem {
    private final LatLng mPosition;

    public MyItem(double lat, double lon) {
        mPosition = new LatLng(lat, lon);
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }
}
