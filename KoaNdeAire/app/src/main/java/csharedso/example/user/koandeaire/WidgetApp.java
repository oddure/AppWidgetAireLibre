package csharedso.example.user.koandeaire;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Implementation of App Widget functionality.
 */
public class WidgetApp extends AppWidgetProvider{

    private static Handler handler;

    private static final String SYNC_CLICKED    = "automaticWidgetSyncButtonClick";

    static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                                final int appWidgetId) {

        /*
        se retrasa el proceso por 4 segundos ya que los valores almacenados con el sharedPreferences
        tienen un cierto retardo para actualizarlas
        */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                // Constructor de los objetos de RemoteViews
                final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_app);

                //Oculta las barras de progreso una vez creado o actualizado el widget
                views.setViewVisibility(R.id.progressBar, View.INVISIBLE);
                views.setViewVisibility(R.id.progressBar2, View.INVISIBLE);

                //asignacion de onclick a elementos para pasar al Activity main
                views.setOnClickPendingIntent(R.id.wgImage, IntentMain(context));
                views.setOnClickPendingIntent(R.id.button, IntentMain(context));

                //asignacion de onclick al textview de wgUpdate para actualizar el widget desde la vista de inicio
                views.setOnClickPendingIntent(R.id.wgUpdate, getPendingSelfIntent(context, SYNC_CLICKED,appWidgetId));


                //asignación de valores almacenados con en el SharedPreferences
                SharedPreferences preferences = context.getSharedPreferences(String.valueOf(R.integer.save_values), Context.MODE_PRIVATE);
                String city = preferences.getString(String.valueOf(R.string.city),"");
                int aqi= preferences.getInt(String.valueOf(R.integer.aqi), 0);
                String status= preferences.getString(String.valueOf(R.string.status), "");
                String update= preferences.getString(String.valueOf(R.string.update), "");


                //////////////////////////
                PrintWidget(views, status);

                views.setTextViewText(R.id.wgCity,""+city);
                views.setTextViewText(R.id.wgAqi, ""+aqi);
                views.setTextViewText(R.id.wgUpdate, update);

                //Toast.makeText(context,"Widget actualizado", Toast.LENGTH_LONG).show();
                //////////////////////////

                // Indica al administrador de widgets que actualice el widget
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }

        }, 4000);


    }

    //Intent para actualizar el widget una vez pulsado el textview tvUpdate
    public static PendingIntent getPendingSelfIntent(Context context, String action, int appWidgetId) {
        Intent intent = new Intent(context, WidgetApp.class);
        intent.setAction(action);

        int[] idArray = new int[]{appWidgetId};
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray);

        PendingIntent pendingIntent= PendingIntent.getBroadcast(context, appWidgetId, intent, 0);

        return pendingIntent;
    }

    //Intent para pasar del widget al Activity principal
    public static PendingIntent IntentMain(Context context){
        Intent intent= new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent= PendingIntent.getActivity(context,0, intent,0);

        return pendingIntent;
    }

    /*
    Imprime el estado y la imagen en el widget
    el color de la imagen y el tamaño del texto varía
    */
    public static void PrintWidget(RemoteViews views, String status){
        if(status.equals("Good")){//MainActivity.wgstatus
            views.setTextViewText(R.id.wgStatus, "LIBRE");
            views.setTextViewTextSize(R.id.wgStatus,1,15);

            views.setImageViewResource(R.id.wgImage, R.drawable.airlibre);

        } else if(status.equals("Moderate")){
            views.setTextViewText(R.id.wgStatus, "MASO");
            views.setTextViewTextSize(R.id.wgStatus,1,15);

            views.setImageViewResource(R.id.wgImage, R.drawable.airmaso);

        } else if(status.equals("Unhealthy for Sensitive Groups")){
            views.setTextViewText(R.id.wgStatus, "NO TAN BIEN");
            views.setTextViewTextSize(R.id.wgStatus,1,12);

            views.setImageViewResource(R.id.wgImage, R.drawable.airnotan);

        } else if(status.equals("Unhealthy")) {
            views.setTextViewText(R.id.wgStatus, "INSALUBRE");
            views.setTextViewTextSize(R.id.wgStatus,1,13);

            views.setImageViewResource(R.id.wgImage, R.drawable.airinsa);

        } else if(status.equals("Very Unhealthy")) {
            views.setTextViewText(R.id.wgStatus, "MUY INSALUBRE");
            views.setTextViewTextSize(R.id.wgStatus,1,11);

            views.setImageViewResource(R.id.wgImage, R.drawable.airminsa);

        } else if(status.equals("Hazardous")) {
            views.setTextViewText(R.id.wgStatus, "PELIGROSO");
            views.setTextViewTextSize(R.id.wgStatus,1,13);

            views.setImageViewResource(R.id.wgImage, R.drawable.airpelig);

        } else{
            views.setTextViewText(R.id.wgStatus, "ACTUALIZAR");
            views.setTextViewTextSize(R.id.wgStatus,1,12);

            views.setImageViewResource(R.id.wgImage, R.drawable.airinstrumentation);
        }
    }

    @Override
    public void onUpdate(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        //VERIFICAR GPS
        Boolean gpsEnable= statusGPS(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_app);


        for (int appWidgetId : appWidgetIds) {

            if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){

                updateAppWidget(context, appWidgetManager, appWidgetId);
            }else{

                if(!gpsEnable){

                    updateAppWidget(context, appWidgetManager, appWidgetId);
                    //Toast.makeText(context, "No actualizado!!", Toast.LENGTH_LONG).show();
                }else {

                    //Toast.makeText(context, "Actualizando...", Toast.LENGTH_LONG).show();
                    views.setViewVisibility(R.id.progressBar2, View.VISIBLE);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                    handler= new Handler(){
                        @Override
                        public void handleMessage(Message message){
                            Bundle bundle= message.getData();

                            //Toast.makeText(context, "Actualizando...", Toast.LENGTH_LONG).show();
                        }

                    };

                    Thread thread= new Thread(new CallProcesos(context, handler,
                            null, null, null, null, null, null));//
                    thread.start();

                    updateAppWidget(context, appWidgetManager, appWidgetId);
                }

            }

            //updateAppWidget(context, appWidgetManager, appWidgetId);
        }


    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created

    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    //Obtiene el estado actual del GPS
    private static boolean statusGPS(Context context) {
        //VERIFICAR GPS
        boolean gpsEnable;
        //LocationManager mLocationManager = (LocationManager)ctx.getSystemService(LOCATION_SERVICE);
        LocationManager mlocManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        gpsEnable = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        return gpsEnable;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        super.onReceive(context, intent);

        Boolean gpsEnable= statusGPS(context);

        //Verifica si se ha pulsado el textview para llamar al onUpdate y actualizar el widget de forma manual
        if (SYNC_CLICKED.equals(intent.getAction())) {


            if(!gpsEnable){
                Toast.makeText(context,"Active el GPS para actualizar", Toast.LENGTH_LONG).show();
            }else{

                //Realiza una llamada al onUpdate para actualizar
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), getClass().getName());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
                onUpdate(context, appWidgetManager, appWidgetIds);

            }


        }

    }

}

