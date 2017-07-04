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

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jesus on 27/06/17.
 */

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    //Para modo aleatorio
    private boolean shuffle=false;
    private Random rand;

    //Título canción
    private String songTitle="";
    private static final int NOTIFY_ID=1;

    //media player
    private MediaPlayer player;
    //Lista de canciones
    private ArrayList<song> songs;
    //posición actual de la canción
    private int songPosn;

    private final IBinder musicBind = new MusicBinder();

    public void onCreate(){
        //creamos el servicio
        super.onCreate();
    //Iniciamos la posición de la canción en 0
        songPosn=0;
    //Creamos el reproductor.
        player = new MediaPlayer();

        //Iniciamos el reproductor
        initMusicPlayer();

        //Variable aleatoria para el modo aleatorio
        rand=new Random();


        //Devolver al usuario a la main activity
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.android_music_player_play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle(getResources().getString(R.string.playing)).setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }


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

    //Pasamos la lista de canciones desde la actividad principal

    public void setList(ArrayList<song> theSongs){
        songs=theSongs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong(){
        //Reiniciar y Reproducir canción actual
        player.reset();

        //Obtener la canción
        song playSong = songs.get(songPosn);
        //Obtener el título de la canción
        songTitle=playSong.getTitle();
        //Obtener el ID
        long currSong = playSong.getID();
        //Obtener el Uri de la canción (almacenada en la memoria externa).
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        player.prepareAsync(); //Reproducimos la canción a través de Async (segundo plano)
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

    }

    public void setSong(int songIndex){
        songPosn=songIndex;
    }

    //Metodo para ir a la canción anterior
    public void playPrev(){
        songPosn--;

        //No saltar si es la primera canción
        if(songPosn==0) songPosn=songs.size()-1;
        playSong();
    }

    //Método para saltar a la canción siguiente
    public void playNext(){
        if(shuffle /*Si el modo aleatorio está encendido*/){
            int newSong = songPosn;
            while(newSong==songPosn){
                newSong=rand.nextInt(songs.size());
            }
            songPosn=newSong;
        }
        else{
            songPosn++;
            if(songPosn == songs.size()) songPosn=0;
        }
        playSong();
    }


    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
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
}
