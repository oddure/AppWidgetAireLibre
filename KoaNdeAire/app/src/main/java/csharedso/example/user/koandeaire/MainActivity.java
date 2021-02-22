package csharedso.example.user.koandeaire;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;

import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.osmdroid.config.Configuration;

import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity{

    private TextView tvcity;
    private TextView tvSensor;
    private TextView tvAqi;
    private TextView tvStatus;
    private TextView tvUpdate;
    private CardView cvStatus;
    private ProgressBar pBUpdate;
    private Button btnUpdate;
    private CardView cvBtn;

    //PERMISOS
    final private int REQUEST_PERMISSION_LOCATION=111;

    //VERIFICAR GPS
    boolean gpsEnable;

    //variable para el widget
    public static String wgcity;
    public static String wgdescription;
    public static int wgaqi;
    public static String wgstatus;
    public static String wgupdate;

    //HANDLER
    private Handler handler;

    //////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Context ctx = getApplicationContext();
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        tvcity = (TextView) findViewById(R.id.tvCity);
        tvSensor = (TextView) findViewById(R.id.tvSensor);
        tvAqi = (TextView) findViewById(R.id.tvAqi);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvUpdate = (TextView) findViewById(R.id.tvUpdate);
        cvStatus = (CardView) findViewById(R.id.cardView);
        pBUpdate=(ProgressBar) findViewById(R.id.pB);
        btnUpdate= (Button) findViewById(R.id.btnActualizar);
        cvBtn= (CardView) findViewById(R.id.cardViewBtn);


        permisos();

        getValuePreference();

    }
    //////////////////////////////////////

    //Agregar menu
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_widget, menu);
        return true;
    }

    //Opciones de menu
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.item_sesion) {

            createWidget();

        }

        return super.onOptionsItemSelected(item);
    }

    //Cierra la aplicacion con el botón Back para evitar pestañas dobles
    /*
    @Override
    public void onBackPressed(){
        finish();
    }
    */

    //Solicita que se conceda permisos de Ubicación a la aplicación
    private void permisos(){
        int permisoUbicacion= ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);

        if(permisoUbicacion!=PackageManager.PERMISSION_GRANTED){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_PERMISSION_LOCATION);

            }
        }
    }

    public void callUbicacion(View view) {

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){

            Toast.makeText(getApplicationContext(),"Primero debe de otorgar los permisos de ubicación!!",Toast.LENGTH_LONG).show();

        } else{

            statusGPS();

            if (!gpsEnable) {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setCancelable(false);
                alertDialog.setMessage(getString(R.string.alertMessage));
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.btnPos),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(settingsIntent);
                            }
                        });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.btnNeg),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int in) {
                                //
                            }
                        });

                alertDialog.show();
            } else {

                tvStatus.setText("Actualizando...");
                pBUpdate.setVisibility(View.VISIBLE);
                btnUpdate.setEnabled(false);
                cvBtn.setCardBackgroundColor(Color.parseColor("#C3AAAAAA"));



                handler= new Handler(){
                    @Override
                    public void handleMessage(Message message){
                        Bundle bundle= message.getData();
                        pBUpdate.setVisibility(View.INVISIBLE);
                        btnUpdate.setEnabled(true);
                        cvBtn.setCardBackgroundColor(Color.parseColor("#03DAC5"));

                        //tvStatus.setText(bundle.getString(mensaje));
                        //Toast.makeText(getApplicationContext(), ""+bundle.getString(mensaje), Toast.LENGTH_LONG).show();
                        //Toast.makeText(getApplicationContext(), "Actualizado!!", Toast.LENGTH_LONG).show();

                        updateWidget();
                    }

                };

                //Thread thread= new Thread(new Mihilo());
                Thread thread= new Thread(new CallProcesos(getApplicationContext(), handler,
                        tvcity, tvSensor, tvAqi, tvStatus, tvUpdate, cvStatus));//
                thread.start();


            }


        }


    }

    //Obtiene el stado actual del GPS
    private void statusGPS() {
        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsEnable = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    //recupera el valor de las variables almacenadas
    public void getValuePreference() {
        String city, description, status, update;
        int aqi;

        SharedPreferences preferences = this.getSharedPreferences(String.valueOf(R.integer.save_values), Context.MODE_PRIVATE);
        city = preferences.getString(String.valueOf(R.string.city),"");
        description= preferences.getString(String.valueOf(R.string.description),"");
        aqi= preferences.getInt(String.valueOf(R.integer.aqi), 0);
        status= preferences.getString(String.valueOf(R.string.status), "");
        update= preferences.getString(String.valueOf(R.string.update), "");


        if (!city.equals("noactualizado")){

            tvcity.setText(""+city);
            tvSensor.setText(""+ description);
            tvAqi.setText(""+aqi);

            printStatus(status);

            tvUpdate.setText(""+update);

            wgcity=city;
            wgdescription=description;
            wgaqi= aqi;
            wgstatus=status;
            wgupdate= update;

            //Toast.makeText(getApplicationContext(),"OK" + update, Toast.LENGTH_SHORT).show();
        }

    }

    //realiza un intent al widget para que se actualicen los valores
    public void updateWidget(){
        AppWidgetManager appWidgetManager= null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            appWidgetManager = getApplication().getSystemService(AppWidgetManager.class);
        }
        Intent intent= new Intent(getApplication(), WidgetApp.class);
        intent.setAction(appWidgetManager.ACTION_APPWIDGET_UPDATE);

        int []ids= appWidgetManager.getAppWidgetIds(new ComponentName(getApplication(), WidgetApp.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
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
    }

    //metodo para generar el widget desde la app
    public void createWidget(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AppWidgetManager appWidgetManager= getApplication().getSystemService(AppWidgetManager.class);

            ComponentName myProvider= new ComponentName(getApplication(), WidgetApp.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if(appWidgetManager.isRequestPinAppWidgetSupported()){
                    appWidgetManager.requestPinAppWidget(myProvider,null,null);

                }
            }
        }


    }

    //redirecciona a la pagina de AireLibre
    public void urlSite(View view){
        Uri url= Uri.parse("http://airelib.re/");
        Intent intent= new Intent(Intent.ACTION_VIEW, url);
        startActivity(intent);
    }

}