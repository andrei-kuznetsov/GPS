package kspt.revkina.gps;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * The class responsible for obtaining data by clusters
 */

class MyItem implements ClusterItem {
    private final LatLng mPosition;

    public MyItem(double lat, double lon) {
        mPosition = new LatLng(lat, lon);
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }
}
