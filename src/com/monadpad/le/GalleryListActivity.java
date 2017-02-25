package com.monadpad.le;

import android.app.ListActivity;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GalleryListActivity extends ListActivity
{
    private MatrixCursor galleryCursor  = new MatrixCursor(galleryProjection);
    private String galleryUrl;
    private int page;
    private boolean noMoreToDownload = false;

    public static final String[] galleryProjection =
            {"_ID", "name", "artist", "date", "json", "ratingCount", "ratingAverage"};

    private TextView foot;
    private TextView head;

    private ArrayList<String> jsonArray = new ArrayList<String>();

    private int headerOffset = 0;


    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        galleryUrl = getString(R.string.url_main) + getString(R.string.url_gallery);

        setTitle(getString(R.string.gallery_list_title));
        page = 1;

        head = new TextView(this);
        final int savedGrooves = SdListManager.getSavedGrooveCount();
        if (savedGrooves > 0) {
            head.setText(getString(R.string.saved_count_on_sd));
            head.setTextAppearance(this, android.R.style.TextAppearance_Medium);
            head.setPadding(20, 20, 20, 20);
            head.setGravity(0x11);
            getListView().addHeaderView(head, -7, true);

            headerOffset = 1;
        }

        foot = new TextView(this);
        foot.setText(R.string.loading_please_wait);
        foot.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        foot.setPadding(20, 20, 20, 20);
        foot.setGravity(0x11);
        getListView().addFooterView(foot, -7, true);

        setupAdapter();

        if (savedInstanceState != null && savedInstanceState.get("json_array") != null){
            jsonArray = savedInstanceState.getStringArrayList("json_array");
            page = savedInstanceState.getInt("page");
            for (String json : jsonArray){
                try{
                    addToGalleryCursorFromJson(json);
                } catch (JSONException ej){}
            }
//            setupAdapter();
        }
        else {
            new DownloadGallery().execute();
        }
    }


    public void onListItemClick(ListView l, View v, int position, long id){

        if (v == head) {
            startActivity(new Intent(this, SdListActivity.class));
            return;
        }

        if (v == foot){
            if (((TextView)v).getText().equals(getString(R.string.get_more)) && !noMoreToDownload){
                page++;
                new DownloadGallery().execute();
            }
            return;
        }

        galleryCursor.moveToPosition(position - headerOffset);
        Intent intent = new Intent(this, MainActivity.class);
        String json = galleryCursor.getString(galleryCursor.getColumnIndex("json"));
        if (!json.startsWith("{")){
            json = "good;" + json;
        }
        intent.putExtra("grooveInfo", json);
        intent.putExtra("galleryId", (int)id);
        intent.putExtra("galleryTitle", galleryCursor.getString(galleryCursor.getColumnIndex("name")));
        intent.putExtra("galleryArtist", galleryCursor.getString(galleryCursor.getColumnIndex("artist")));

        startActivity(intent);

    }

    private class DownloadGallery extends AsyncTask<Void, Void, Void>{

        String responseString = "";

        private boolean downloaded = false;
        private boolean parsed = false;


        @Override
        public void onPreExecute(){

            foot.setText(R.string.loading_please_wait);

        }

        @Override
        public void onPostExecute(Void v){

            if (!parsed || galleryCursor.getCount() == 0){
                noMoreToDownload = true;
            }

            if (getListAdapter() == null){
                setupAdapter();

            }
            else {
                ((SimpleCursorAdapter)getListAdapter()).notifyDataSetChanged();

            }

            if (noMoreToDownload){
                foot.setText(getString(R.string.no_more_to_download));
            }
            else {
                foot.setText(R.string.get_more);
            }

        }


        @Override
        protected Void doInBackground(Void... voids) {

            // get a list of grooves in the gallery
            String cUrl= galleryUrl + Integer.toString(page);

            HttpClient httpclientup = new DefaultHttpClient();
            try {
                HttpResponse response = httpclientup.execute(new HttpGet(cUrl));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();

                    downloaded = true;

                }else{
                    responseString = statusLine.getReasonPhrase();
                }

            } catch (ClientProtocolException e) {
                responseString =
                        e.getMessage();
                e.printStackTrace();
            } catch (IOException e) {
                responseString =
                        e.getMessage();
                e.printStackTrace();
            }

            if (downloaded){
                if (responseString.startsWith("[")){
                    jsonArray.add(responseString);

                    try {
                        if (addToGalleryCursorFromJson(responseString) == 0)
                            noMoreToDownload = true;
                        parsed = true;
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                        responseString = "JSON but could not be parsed";
                    }

                }

            }
            return null;
        }


    }


    int addToGalleryCursorFromJson(String json) throws JSONException{

        JSONArray jsA = new JSONArray(json);
        for (int iii = 0; iii < jsA.length(); iii++){
            MatrixCursor.RowBuilder rb = galleryCursor.newRow();
            rb.add(jsA.getJSONObject(iii).getString("id"));
            rb.add(jsA.getJSONObject(iii).getString("name"));
            rb.add(jsA.getJSONObject(iii).getString("artist"));
            //                rb.add(jsA.getJSONObject(iii).getString("date"));
            rb.add("date");
            rb.add(jsA.getJSONObject(iii).getString("json"));
            rb.add(jsA.getJSONObject(iii).optDouble("ratingCount", 0));
            rb.add(jsA.getJSONObject(iii).optDouble("ratingAverage", 0));
        }
        return jsA.length();

    }


    @Override
    public void onSaveInstanceState(Bundle saveInstanceState){
        super.onSaveInstanceState(saveInstanceState);
        saveInstanceState.putStringArrayList("json_array", jsonArray);
        saveInstanceState.putInt("page", page);
    }

    private void setupAdapter(){
        SimpleCursorAdapter curA = new GalleryAdapter(GalleryListActivity.this,
                R.layout.gallery_row,
                galleryCursor, new String[]{"name", "artist"},
                new int[]{R.id.gallery_name,R.id.gallery_artist});
        setListAdapter(curA);

    }

}
