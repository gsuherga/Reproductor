package com.example.android.reproductor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
        //obtenemos el título y el artista
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
        //la posición de la canción
        song currSong = songs.get(position);
        //título de la canción y el artista
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        //establecemos como equiteta la posición en la canción
        songLay.setTag(position);
        return songLay;
    }
}
