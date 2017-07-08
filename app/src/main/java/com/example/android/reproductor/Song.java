package com.example.android.reproductor;

/**
 * Created by jesus on 21/06/17.
 *
 * Aquí haremos una clase para trabajar con canciones de forma individual
 */

public class Song {

    private long id;

    private String title;

    private String artist;

    private String fotoDisco;


    //Creamos un método para así poder extraer luego los metadatos de las canciones

        public Song(long songID, String songTitle, String songArtist, String data) {
        id=songID;
        title=songTitle;
        artist=songArtist;
        fotoDisco = data;
    }

   //Métodos que devuelven metadatos de las canciones.

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public String getData(){
        return fotoDisco;
    }



}

