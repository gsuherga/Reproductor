package com.example.android.reproductor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
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

public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;
    private LayoutInflater songInf;

    public SongAdapter(Context c, ArrayList<Song> theSongs) {
        songs = theSongs;
        songInf = LayoutInflater.from(c);
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
        LinearLayout songLay = (LinearLayout) songInf.inflate
                (R.layout.song, parent, false);
        //obtenemos el título, el artista, y la foto del albúm
        TextView songView = (TextView) songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView) songLay.findViewById(R.id.song_artist);
        final ImageView coverArt = (ImageView) songLay.findViewById(R.id.album);
        //la canción según la posición del adapter
        Song currSong = songs.get(position);
        //título de la canción, el artista y el album
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());


        final MediaMetadataRetriever mdata = new MediaMetadataRetriever();

        mdata.setDataSource(currSong.getData());

        byte [] data = mdata.getEmbeddedPicture();


        if(data != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            coverArt.setImageBitmap(bitmap); //la portada se asocia al bitmap que hemos obtenido
            coverArt.setAdjustViewBounds(true);
        }else  {
            coverArt.setImageResource(R.drawable.vinilo); //si no hay portada ponemos esta foto por defecto
            coverArt.setAdjustViewBounds(true);
        }
        //establecemos como etiqueta según la posición en el adapter
        songLay.setTag(position);
        return songLay;
    }
}
