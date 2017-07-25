package com.example.android.reproductor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jesus on 27/06/17.
 */

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    //Para modo aleatorio

    private boolean shuffle=false; //Para reproducir en modo aleatorio

    private Random rand; //Número aleatorio que nos dará la canción que debemos reproducir

    //Título canción
    private String songTitle=""; //Título canción para la barra de notificación del móvil

    private static final int NOTIFY_ID = 1; //Para la notificación en la barra superior del móvil

    //media player
    private MediaPlayer player; //Nuestro reproductor

    private int duration; //Duración de la canción

    //Lista de canciones
    private ArrayList<Song> songs; //Lista con las canciones

    //posición actual de la canción
    private int songPosn; //Nos dice la posición en el array de canciones anterior

    private Song currSong; //La canción de reproducción al pulsar directamente sobre una de la lista

    private final IBinder musicBind = new MusicBinder(); //Enlace entre el servicio de medioPlayer y la aplicación

    Notification.Builder builder = new Notification.Builder(this); //Para las notificaciones

    public void onCreate() {
        //creamos el servicio
        super.onCreate();
        //Iniciamos la posición de la canción en 0
        songPosn = 0;
        //Creamos el reproductor.
        player = new MediaPlayer();

        //Iniciamos el reproductor
        initMusicPlayer();

        //Variable aleatoria para el modo aleatorio
        rand = new Random();
    }

        public void notification(){

        //Devolver al usuario a la main activity
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.android_music_player_play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle(getResources().getString(R.string.playing))
                .setContentText(songTitle);
        Notification not = builder.build(); //Enviar la notificación.

        startForeground(NOTIFY_ID, not);}



    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    //Liberamos recursos cuando no se le da uso al servicio (MusicService).

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    public void initMusicPlayer(){
        //establece las propiedades del reproductor

        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK); //La reproducción continua aunque usemos otras aplicaciones
        player.setAudioStreamType(AudioManager.STREAM_MUSIC); //Establece la reproducción como streaming
        player.setOnPreparedListener(this); //establece la clase como listener cuando la instancia mediaplayer se pepara,
        player.setOnCompletionListener(this); // cuando la canción termina,
        player.setOnErrorListener(this); //y cuando se produce un error.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition()!=0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        //Comenzamos la reprodución.
        mp.start();
        duration = player.getDuration();


    }

    public void setSong(Song currentSong, int songPosition){

        songPosn = songPosition;
        currSong = currentSong;

    }

    //Pasamos la lista de canciones desde la actividad principal

    public void setList(ArrayList<Song> theSongs){
        songs=theSongs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong(Song song){

        //Reiniciar y Reproducir canción actual
        player.reset();

        //Obtener el título de la canción
        songTitle = song.getTitle();

        notification();

        //Obtener el ID
        long currSongID = song.getID();
        //Obtener el Uri de la canción (almacenada en la memoria externa).
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSongID);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
            Toast.makeText(this,songTitle ,
                    Toast.LENGTH_LONG).show();
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        player.prepareAsync(); //Reproducimos la canción a través de Async (segundo plano)
    }



    //Metodo para ir a la canción anterior
    public void playPrev(){

        player.reset();

        long newSong = 0;


       if(songPosn > 0){

           songPosn = songPosn - 1;

           //Obtener el ID
        newSong = songs.get(songPosn).getID();

           //Obtener el título de la canción
           songTitle = songs.get(songPosn).getTitle();

       }

        //Obtener el Uri de la canción (almacenada en la memoria externa).
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                newSong);
        Toast.makeText(this,songTitle,
                Toast.LENGTH_LONG).show();

        notification();

        try{
            player.setDataSource(getApplicationContext(), trackUri);
            player.prepare();
            player.start();
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

    }

    //Método para saltar a la canción siguiente
    public void playNext(){

        player.reset();

        long newSong = 0;

        if(shuffle /*Si el modo aleatorio está encendido*/){
            newSong = currSong.getID();
            while(newSong == currSong.getID()){
                int randomPosition = rand.nextInt(songs.size());

                newSong = songs.get(randomPosition).getID();
            }
        }
        else{
            songPosn = songPosn + 1 ;
           if(songPosn < songs.size()) {

             //   Obtener el ID
                 newSong = songs.get(songPosn).getID();
            }
        }

        //Obtener el título de la canción
        songTitle = songs.get(songPosn).getTitle();

        //Obtener el Uri de la canción (almacenada en la memoria externa).
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                newSong);
        Toast.makeText(this,songTitle,
                Toast.LENGTH_LONG).show();

        notification();

        try{
            player.setDataSource(getApplicationContext(), trackUri);
            player.prepare();
            player.start();
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

       // player.prepareAsync(); //Reproducimos la canción a través de Async (segundo plano)

    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return duration;
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    public void setShuffle(){
        if(shuffle) shuffle=false;
        else shuffle=true;
    }

    public void release(){
        player.release();
    }
}
