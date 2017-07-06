package com.example.android.reproductor;

import android.media.MediaMetadataRetriever;

/**
 * Created by jesus on 21/06/17.
 *
 * Aquí haremos una clase para trabajar con canciones de forma individual
 */

public class song {

    private long id;

    private String title;

    private String artist;

    private MediaMetadataRetriever fotoDisco;


    //Creamos un método para así poder extraer luego los metadatos de las canciones

        public song (long songID, String songTitle, String songArtist, MediaMetadataRetriever mr) {
        id=songID;
        title=songTitle;
        artist=songArtist;
        fotoDisco = mr;
    }

   //Métodos que devuelven metadatos de las canciones.

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public MediaMetadataRetriever getMediaMetaData(){
        return fotoDisco;
    }



}

