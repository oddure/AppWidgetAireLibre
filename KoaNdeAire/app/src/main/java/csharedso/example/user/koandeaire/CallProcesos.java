package csharedso.example.user.koandeaire;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;

public class CallProcesos implements Runnable {

    private Context context;

    private static final String mensaje= "mensaje";
    private Handler handler;

    double lat;
    double lon;

    private TextView tvcity;
    private TextView tvSensor;
    private TextView tvAqi;
    private TextView tvStatus;
    private TextView tvUpdate;
    private CardView cvStatus;

    String URL_Scraper= "http://url_del_scraper/scrapear_airebot.py";

    //////////////////////////////////////
    public CallProcesos(Context context, Handler handler,
                        TextView tvcity, TextView tvSensor, TextView tvAqi, TextView tvStatus, TextView tvUpdate,CardView cvStatus){
        this.context= context;
        this.handler= handler;
        this.tvcity= tvcity;
        this.tvSensor= tvSensor;
        this.tvAqi= tvAqi;
        this.tvStatus= tvStatus;
        this.tvUpdate= tvUpdate;
        this.cvStatus= cvStatus;
    }
    //////////////////////////////////////

    @Override
    public void run() {
        Message message= new Message();
        final Bundle bundle= new Bundle();

        message.setData(bundle);

        Handler mLocationHandler;

        Looper.prepare();

        try {

            Location l=null;
            LocationManager mLocationManager = (LocationManager)context.getSystemService(LOCATION_SERVICE);
            List<String> providers = mLocationManager.getProviders(true);
            Location bestLocation = null;
            for (String provider : providers) {
                if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
                    l = mLocationManager.getLastKnownLocation(provider);
                }
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = l;
                }
            }

            if(bestLocation==null){

                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {

                        Toast.makeText(context, "Ejecutando Looper", Toast.LENGTH_LONG).show();

                        lat= location.getLatitude();
                        lon= location.getLongitude();


                        nameCity(lat,lon);

                        bundle.putString(mensaje, String.valueOf(location));

                        Looper.myLooper().quit();

                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                });

            } else{

                Toast.makeText(context, "Ubicacion determinada con Bestlocation"+ bestLocation, Toast.LENGTH_LONG).show();
                lat= bestLocation.getLatitude();
                lon= bestLocation.getLongitude();

                nameCity(lat,lon);

                bundle.putString(mensaje, String.valueOf(bestLocation));
                Looper.myLooper().quit();

            }


        }catch (Exception e){
            bundle.putString(mensaje, "Error:"+ e);
        }

        Looper.loop();

        handler.sendMessage(message);
    }

    /*
    Con el valor de  las coordenadas realiza una llamada a OpenStreetMap
    para determinar el nombre de la ciudad en la que se encuentra.
    */
    public void nameCity(Double latitud, Double longitud){


        final String QUERY_URL ="https://nominatim.openstreetmap.org/reverse?format=json" +
                "&lat=" + latitud+
                "&lon=" + longitud+
                "&zoom=18&addressdetails=1";

        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                QUERY_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {

                            String city = "";
                            //ObjectPrincipal
                            JSONObject jsonObject = new JSONObject(response);
                            //Object"Address"

                            JSONObject jsonObject1= jsonObject.getJSONObject("address");

                            city= jsonObject1.getString("city");

                            //tvcity.setText(""+city);

                            //progressDialogCity.dismiss();


                            SharedPreferences sharedPreferences = context.getSharedPreferences(String.valueOf(R.integer.save_values), Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor= sharedPreferences.edit();

                            editor.putString(String.valueOf(R.string.city), city);

                            editor.apply();


                            //Realizar consulta al scraper

                            callApi(city);

                            //Toast.makeText(context, "Ciudad:" +city, Toast.LENGTH_LONG).show();


                        } catch (JSONException e) {

                            //progressDialogCity.dismiss();

                            //Toast.makeText(context, "Error en Json consulta City "+ e, Toast.LENGTH_LONG).show();
                            Toast.makeText(context, "Por favor intente de nuevo...city", Toast.LENGTH_LONG).show();
                            if(tvStatus!=null){
                                tvStatus.setText("Ocurrió un error!!");
                            }

                            e.printStackTrace();
                        }
                    }

                },

                /** Si ha habido un error en la adquisición de los datos avisamos por pantalla con un toast*/
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        //Toast.makeText(context, "Error en consulta City, Verifique su conexion de Internet!!"+ error, Toast.LENGTH_LONG).show();
                        Toast.makeText(context, "Por favor intente de nuevo...city2", Toast.LENGTH_LONG).show();
                        if(tvStatus!=null){
                            tvStatus.setText("Ocurrió un error!!");
                        }

                    }
                });
        Volley.newRequestQueue(context).add(stringRequest);

        return;

    }

    //obtiene los datos correspondientes realizando una llamada al scraper
    private void callApi(final String city){

        final String QUERY_URL = URL_Scraper;

        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                QUERY_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {

                            String description= "";
                            double latitude=0.0;
                            double longitude=0.0;
                            int aqi=0;
                            String status="";

                            //print
                            double distance=0.0;
                            double Vwdistance=0.0;
                            String Vwdescription="";
                            double Vwlatitude=0.0;
                            double Vwlongitude=0.0;
                            int Vwaqi=0;
                            String Vwstatus="";



                            JSONArray jsonArray= new JSONArray(response);

                            for(int i=0; i<jsonArray.length();i++){

                                JSONObject jsonObject= jsonArray.getJSONObject(i);

                                JSONObject jsonObjectSen= jsonObject.getJSONObject("quality");

                                description= jsonObject.getString("description");
                                longitude=jsonObject.getDouble("longitude");
                                latitude= jsonObject.getDouble("latitude");
                                status= jsonObjectSen.getString("category");
                                aqi= jsonObjectSen.getInt("index");


                                distance= distFrom(lat,lon, latitude,longitude);

                                if(i==0){
                                    Vwdistance= distance;
                                    Vwdescription=description;
                                    Vwlatitude=latitude;
                                    Vwlongitude=longitude;
                                    Vwaqi=aqi;
                                    Vwstatus=status;


                                } else if(i>0 && distance< Vwdistance){
                                    Vwdistance= distance;
                                    Vwdescription=description;
                                    Vwlatitude=latitude;
                                    Vwlongitude=longitude;
                                    Vwaqi=aqi;
                                    Vwstatus=status;


                                }

                            }

                            //Toast.makeText(ctx,""+lat+";"+lon+";"+city+";"+Vwdescription+";"+Vwaqi+";"+Vwstatus,Toast.LENGTH_LONG).show();
                            //Toast.makeText(ctx,""+lat+";"+lon,Toast.LENGTH_LONG).show();

                            SharedPreferences sharedPreferences = context.getSharedPreferences(String.valueOf(R.integer.save_values), Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor= sharedPreferences.edit();

                            editor.putString(String.valueOf(R.string.description), Vwdescription);
                            editor.putInt(String.valueOf(R.integer.aqi), Vwaqi);
                            editor.putString(String.valueOf(R.string.status), Vwstatus);

                            editor.apply();

                            //Toast.makeText(ctx,"Actualizado ",Toast.LENGTH_LONG).show();

                            imprimir(lat, lon, city, Vwdescription,Vwaqi, Vwstatus);


                            //Toast.makeText(ctx,"Distancia: "+ Vwdistance,Toast.LENGTH_LONG).show();

                        } catch (JSONException e) {

                            //Toast.makeText(context, "Error en Json consulta scrapper "+ e, Toast.LENGTH_LONG).show();
                            //e.printStackTrace();

                            Toast.makeText(context, "Por favor intente de nuevo...scraper", Toast.LENGTH_LONG).show();
                            if(tvStatus!=null){
                                tvStatus.setText("Ocurrió un error!!");
                            }

                            return;
                        }
                    }

                },

                /** Si ha habido un error en la adquisición de los datos avisamos por pantalla con un toast*/
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        //Toast.makeText(context, "Error en consulta scrapper, Verifique su conexion de Internet!!"+ error+QUERY_URL, Toast.LENGTH_LONG).show();
                        Toast.makeText(context, "Por favor intente de nuevo...scraper2", Toast.LENGTH_LONG).show();
                        if(tvStatus!=null){
                            tvStatus.setText("Ocurrió un error!!");
                        }

                        return;
                    }
                });
        Volley.newRequestQueue(context).add(stringRequest);

        return;

    }

    /*
    Determina la distancia de los sensores en relacion de la ubicacion actual (del usuario),
    para que posteriormente se muestre la informacion del sensor más cercano
    */
    public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat1-lat2);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = (double) (earthRadius * c);

        return dist;
    }

    //Determina y almacena la hora en que se realizan las actualizaciones (en el caso de que no haya ocurrido ningún error)
    //Y dependiendo de que activity se realiza la llamada actualiza los textview del MainActivity
    public void imprimir(double lat, double lon, String city, String description, int aqi, String status){

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm dd-MM");
        String dateUpdate = simpleDateFormat.format(new Date());

        //almacena de  forma local el valor de la variable
        SharedPreferences sharedPreferences = context.getSharedPreferences(String.valueOf(R.integer.save_values), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor= sharedPreferences.edit();

        editor.putString(String.valueOf(R.string.update), dateUpdate);

        editor.apply();

        //Toast.makeText(ctx,"Actualizado "+ dateUpdate,Toast.LENGTH_LONG).show();


        /*
        - Verifica si el constructor recibio parametros null
        - En el caso de que sea null la llamada se realiza desde el widget
        - Caso contrario desde el MainActivity
        */

        if(cvStatus==null){

            //Toast.makeText(context,"Textview no encontrado",Toast.LENGTH_LONG).show();
        }else{
            tvcity.setText(city);
            tvSensor.setText(description);
            tvAqi.setText(""+aqi);
            //tvStatus.setText(status);
            tvUpdate.setText(dateUpdate);


            printStatus(status);
        }


        return;
    }

    //metodo para imprimir el status y cambiar el color del fondo del cardview
    private void printStatus(String status){

        /*
        Libre= {'Good',#A2DC61}
        Maso= {'Moderate',#FBD651}
        No tan bien= {'Unhealthy for Sensitive Groups',#FD9A57}
        Insalubre= {'Unhealthy',#FF6A6E}
        Muy insalubre= {'Very Unhealthy',#A97BBC}
        Peligroso= {'Hazardous',#9B5974}
        */

        //Toast.makeText(getApplicationContext(), "status: "+ status,Toast.LENGTH_LONG).show();

        if(status.equals("Good")){
            tvStatus.setText("LIBRE");
            cvStatus.setCardBackgroundColor(Color.parseColor("#A2DC61"));
        } else if(status.equals("Moderate")){
            tvStatus.setText("MASO");
            cvStatus.setCardBackgroundColor(Color.parseColor("#FBD651"));
        } else if(status.equals("Unhealthy for Sensitive Groups")){
            tvStatus.setText("NO TAN BIEN");
            cvStatus.setCardBackgroundColor(Color.parseColor("#FD9A57"));
        } else if(status.equals("Unhealthy")){
            tvStatus.setText("INSALUBRE");
            cvStatus.setCardBackgroundColor(Color.parseColor("#FF6A6E"));
        } else if(status.equals("Very Unhealthy")){
            tvStatus.setText("MUY INSALUBRE");
            cvStatus.setCardBackgroundColor(Color.parseColor("#A97BBC"));
        } else if(status.equals("Hazardous")){
            tvStatus.setText("PELIGROSO");
            cvStatus.setCardBackgroundColor(Color.parseColor("#9B5974"));
        }else{
            tvStatus.setText("ACTUALIZAR");
        }

        Toast.makeText(context, "Actualizado!!", Toast.LENGTH_LONG).show();

        return;
    }

}
