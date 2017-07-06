package com.example.android.reproductor;

import android.graphics.Bitmap;

/**
 * Created by jesus on 21/06/17.
 *
 * Aquí haremos una clase para trabajar con canciones de forma individual
 */

public class song {

    private long id;
    private String title;
    private String artist;

    private Bitmap fotoDisco = NO_IMAGE_PROVIDED;

    private static final Bitmap NO_IMAGE_PROVIDED = null;

    //Creamos un método para así poder extraer luego los metadatos de las canciones

    public song (long songID, String songTitle, String songArtist, Bitmap bitmap) {
        id=songID;
        title=songTitle;
        artist=songArtist;
        fotoDisco = bitmap;
    }

    public song(long songID, String songTitle, String songArtist) {
        id=songID;
        title=songTitle;
        artist=songArtist;
    }

    public song (Bitmap picture){
        fotoDisco = picture;
    }


    //Métodos que devuelven metadatos de las canciones.

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public Bitmap getBitmap(){
        return fotoDisco;
    }

    public boolean haspicture(){
         return fotoDisco != NO_IMAGE_PROVIDED;
    }

}

