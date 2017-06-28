package com.example.android.reproductor;

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

/**
 * Created by jesus on 27/06/17.
 */

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

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


    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
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
        songPosn++;
        //No saltar si es la última posición
        if(songPosn == songs.size()) songPosn=0;
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
}
