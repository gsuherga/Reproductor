package com.example.android.reproductor;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by jesus on 21/06/17.
 */

public class SongAdapter extends BaseAdapter{

    private ArrayList<song> songs;
    private LayoutInflater songInf;

    public SongAdapter(Context c, ArrayList<song> theSongs){
        songs=theSongs;
        songInf=LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //mapa para inflar el layout de las canciones
        LinearLayout songLay = (LinearLayout)songInf.inflate
                (R.layout.song, parent, false);
        //obtenemos el título, el artista, y la foto del albúm
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
        ImageView coverArt = (ImageView) songLay.findViewById(R.id.album);
        //la canción según la posición del adapter
        song currSong = songs.get(position);
        //título de la canción, el artista y el album
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());

        byte [] picture = currSong.getMediaMetaData().getEmbeddedPicture();

        try {
            coverArt.setImageBitmap((BitmapFactory.decodeByteArray(picture, 0,picture.length)));
        } catch (Exception e){

            coverArt.setImageResource(R.drawable.vinilo);
        }
        //establecemos como equiteta según la posición en el adapter
        songLay.setTag(position);
        return songLay;
    }
}
