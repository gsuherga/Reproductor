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

//Implementamos la clase MediaController para poder así reproducir los archivos de música

public class MainActivity extends AppCompatActivity implements MediaPlayerControl {

    ArrayList<Song> songList = new ArrayList<>();

    ListView songView;

    private MusicService musicSrv;

    private Intent playIntent;

    private boolean musicBound = false;

    private MusicController controller;

    private boolean paused = false, playbackPaused = false;

    Song song;

    /**
     * Handles audio focus when playing a sound file
     */

    private AudioManager mAudioManager; //Variable de enlace con el audiofocus

    Context context; //Contexto para pasar al audiofocus

    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {


        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // The AUDIOFOCUS_LOSS_TRANSIENT case means that we've lost audio focus for a
                // short amount of time. The AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK case means that
                // our app is allowed to continue playing sound but at a lower volume.
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // The AUDIOFOCUS_GAIN case means we have regained focus and can resume playback.
              onResume();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // The AUDIOFOCUS_LOSS case means we've lost audio focus and
                // Stop playback and clean up resources
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

                // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                // app-defined int constant

                return;
            }
        }

        //La lista de las canciones.

        songView = (ListView) findViewById(R.id.song_list);

        //Llamamos al método para cargar las canciones.

        getSongList();

        //Ordenamos las canciones por Artista
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getArtist().compareTo(b.getArtist());
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

                musicSrv.setSong(song, song.getID()); //Establecemos la canción y el music service sabe ir a la anterior y la posterior.

                // Requerimos el audiofocus para poder reproducir la canción si podemos

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

    public void songPicked(Song song) {
      //  Log.e(view.getTag().toString(), "view.getTag().toString()")
        // musicSrv.setSong(Integer.parseInt(song.getID()));
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

    public void getSongList() {
        //retrieve Song info

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

        int album_data = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA); //Datos del album

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever(); //Obtener metadatos

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //Añadimos canciones a la lista
            do { // pasamos a String los datos que hemos obtenido justo arriba
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                // String thisAlbum = musicCursor.getString(album); //Esto es para obtener el título del album, no la foto
                String thisData = musicCursor.getString(album_data);
                metadataRetriever.setDataSource(thisData);
                songList.add(new Song(thisId, thisTitle, thisArtist, thisData));

                //añadimos la canción a la lista
            } while (musicCursor.moveToNext()); //Mientras que haya canciones volveremos a ejecutar el bucle

        }

        musicCursor.close();
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

    //Salir de la aplicación no detendrá la música
    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
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

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {//Para reiniciar la reproducción
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    /**
     * Clean up the media player by releasing its resources.
     */
    public void releaseMediaPlayer() {
        // If the media player is not null, then it may be currently playing a sound.
        if (musicSrv != null) {
            // Regardless of the current state of the media player, release its resources
            // because we no longer need it.
            musicSrv.release();
            // Set the media player back to null. For our code, we've decided that
            // setting the media player to null is an easy way to tell that the media player
            // is not configured to play an audio file at the moment.
            musicSrv = null;
            // Regardless of whether or not we were granted audio focus, abandon it. This also
            // unregisters the AudioFocusChangeListener so we don't get anymore callbacks.
            mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        }

    }
}
