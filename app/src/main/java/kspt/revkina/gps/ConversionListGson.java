package kspt.revkina.gps;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ConversionListGson {

    public void saveSpeedList(String KEY, SharedPreferences pref, ArrayList<HashMap<String, String>> listSpeed) {
        JSONArray result= new JSONArray(listSpeed);
        SharedPreferences.Editor prefEditor = pref.edit();
        prefEditor.putString(KEY, result.toString());
        prefEditor.apply();
    }

    public ArrayList<HashMap<String, String>> getSpeedList(String KEY, SharedPreferences pref, ArrayList<HashMap<String, String>> listSpeed) {
        String storedCollection = pref.getString(KEY, null);
        try {
            JSONArray array = new JSONArray(storedCollection);
            HashMap<String, String> item;
            for(int i =0; i<array.length(); i++){
                JSONObject ary = array.optJSONObject(i);
                Iterator<String> it = ary.keys();
                item = new HashMap<>();
                while(it.hasNext()){
                    String key = it.next();
                    item.put(key, (String)ary.get(key));
                }
                listSpeed.add(item);
            }
        } catch (JSONException ignored) {
        }
        return listSpeed;
    }
}
