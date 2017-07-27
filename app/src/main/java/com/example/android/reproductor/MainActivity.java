package com.example.android.reproductor;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/*

This app was made with help specially of this guide found in internet:

https://code.tutsplus.com/es/tutorials/create-a-music-player-on-android-project-setup--mobile-22764

 */

//Implementamos la clase MediaController para poder así reproducir los archivos de música

public class MainActivity extends AppCompatActivity implements MediaPlayerControl {

    ArrayList<Song> songList = new ArrayList<>(); //La lista de canciones desde el adapter

    ListView songView; //Vista donde irán las canciones

    private MusicService musicSrv; //Variable de musicService para reproducir y manejar las canciones en el reproductor

    private Intent playIntent; //Enlace entre la main activity de nuestra aplicación el servicio de música (MusicService)

    private boolean musicBound = false; //Controla si se ha establecido conexión entre esta activity y la del MusicService

    private MusicController controller; //Controlador de mediaplayer

    private boolean paused = false, playbackPaused = false; //Variables para controlar la pausa en la reproducción

    Song song; //Variable para canción (actual)

    private AudioManager mAudioManager; //Variable de enlace con el audiofocus

    Context context; //Contexto para pasar al audiofocus

    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {


        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Si perdemos audifocus por un corto tiempo (AUDIOFOCUS_LOSS_TRANSIENT) o si
                // podemos reproducir pero a menor volumen (UDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
                // preferimos pausar la reproducción
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                //Obtenemos audifocus nuevamente: retomamos la reproducción
                start();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            //Si perdemos totalmente el audiofoucus paramos y liberamos el mediaplayer.
                releaseMediaPlayer();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Se comprueba que tenemos permiso para leer la tajeta externa de memoria

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                return;
            }
        }

        //La lista de las canciones.

        songView = (ListView) findViewById(R.id.song_list);

        //Llamamos al método para cargar las canciones.

        getSongList();

        //Ordenamos las canciones por Album
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getAlbum().compareTo(b.getAlbum());
            }
        });

        // Disponemos la lista de canciones en la listView
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        //Contexto para luego pasarlo al audiofocus
        context=this;

        songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Creamos un enlace para requerir el audiofocus

                mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

                song = songList.get(position); //La posición nos dice la canción escogida

                musicSrv.setSong(position); //Establecemos la canción y el music service sabe ir a la anterior y la posterior.

                // Requerimos el audiofocus para poder reproducir la canción si nos lo permite

                int result = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

                    //Obtenos audiofocus, por tanto reproducimos la canción

                    songPicked(song);
                }
            }
        });

        //establecemos el controlador (método setController)

        setController();
    }

    public void getSongList() {

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        //Usando el cursor vamos añadiendo las canciones que haya en el teléfono

        //Obtenemos los datos desde los archivos
        int titleColumn = musicCursor.getColumnIndex
                (android.provider.MediaStore.Audio.Media.TITLE); //Título de la canción
        int idColumn = musicCursor.getColumnIndex
                (android.provider.MediaStore.Audio.Media._ID); //ID de la canción
        int artistColumn = musicCursor.getColumnIndex
                (android.provider.MediaStore.Audio.Media.ARTIST); //Artista
        int album = musicCursor.getColumnIndex
                (MediaStore.Audio.Media.ALBUM); //Artista
        int tracknumber = musicCursor.getColumnIndex
                (MediaStore.Audio.Media.TRACK); //Artista

        int album_data = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA); //Datos del album

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever(); //Obtener metadatos

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //Añadimos canciones a la lista
            do { // pasamos a String los datos que hemos obtenido justo arriba
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(album); //Esto es para obtener el título del album, no la foto
                String thisData = musicCursor.getString(album_data);
                String thistrack = musicCursor.getString(tracknumber);
                metadataRetriever.setDataSource(thisData);
                songList.add(new Song(thisId, thisTitle, thisAlbum, thistrack, thisArtist, thisData));

                //añadimos la canción a la lista
            } while (musicCursor.moveToNext()); //Mientras que haya canciones volveremos a ejecutar el bucle

        }

        musicCursor.close();
    }

    public void songPicked(Song song) {
        musicSrv.playSong(song);

        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    //Conectamos con el servicio para conseguir la música (musicService).
    private ServiceConnection musicConnection = new ServiceConnection() {

        //
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            //Obtener el servicio.
            musicSrv = binder.getService();
            //pasar lista de canciones
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    private void setController() {

        //establecer el controlador
        controller = new MusicController(this);

        //Reproducir la canción previa o siguiente al presionar los correspondientes botones

        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }


    @Override
    public void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    //Para buscar una posición concreta en la canción que se está reproducción.
    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);

    }

    @Override
    public void start() {
        musicSrv.go();
            }


    @Override
    public int getDuration() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {

        if (musicSrv != null && musicBound && musicSrv.isPng()) {
            return musicSrv.getPosn();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isPlaying() {
        if (musicSrv != null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Menú de selección

        switch (item.getItemId()) {
            case R.id.action_shuffle: //Modo aleatorio
                musicSrv.setShuffle();
                break;
            case R.id.action_end: //Parar la reproducción
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    //Salir de la aplicación detendrá la música
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(playIntent);
        musicSrv = null;
    }

    //Reproducir la siguiente
    private void playNext() {

        musicSrv.playNext();

        if (playbackPaused) {

            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    //Reproducir la anterior
    private void playPrev() {
        musicSrv.playPrev();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    //OnPause si la aplicación sale de la aplicación sin cerrarla
    @Override
    protected void onPause() {
        super.onPause();
        musicSrv.onUnbind(playIntent);
        musicSrv = null;
        paused = true;
    }

    //Si el uusario vuelve a la aplicación, que aún estaba ejecutándose
    @Override
    protected void onResume() {//Para reiniciar la aplicación
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    //Al salir de la aplicación:
    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
        releaseMediaPlayer();
    }

    /**
     * Limpiar el mediaPlayer.
     */
    public void releaseMediaPlayer() {
        // If the media player is not null, then it may be currently playing a sound.
        //Si el mediaPlayer no está vacio, puede estar reproduciendo sonido.
        if (musicSrv != null) {
            // Regardless of the current state of the media player, release its resources
            // because we no longer need it.

            //A pesar de su estado actual, liberar sus recursos porque no los necesitamos más.
            musicSrv.release();
            //Establecer el reproductor mediaplayer en vació (null) y  abandonar el audiofocus.
            musicSrv = null;
            mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        }

    }
}
