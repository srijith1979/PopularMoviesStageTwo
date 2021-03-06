package com.example.android.popularmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.android.popularmovies.adapter.CustomReviewAdapter;
import com.example.android.popularmovies.adapter.CustomTrailerAdapter;
import com.example.android.popularmovies.data.FavoriteMoviesContract.FavoriteMovieEntry;
import com.example.android.popularmovies.data.FavoriteMoviesDbHelper;
import com.example.android.popularmovies.model.Movie;
import com.example.android.popularmovies.model.Review;
import com.example.android.popularmovies.model.Trailer;
import com.example.android.popularmovies.util.HelperUtil;
import com.example.android.popularmovies.utilities.NetworkUtils;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/*
 * This Activity gets called when a movie is clicked from the
 * grid view.
 * This lists the movie title, synopsis, release date
 * vote count and movie poster
 */
public class MovieDetailsActivity extends AppCompatActivity {
    private Movie movie;
    @BindView(R.id.details_title_id)
    protected TextView titleTextView;
    @BindView(R.id.details_thumbnail_id)
    protected ImageView thumbnailImageView;
    @BindView(R.id.details_overview_id)
    protected TextView overviewTextView;
    @BindView(R.id.details_release_date_id)
    protected TextView releaseDateTextView;
    @BindView(R.id.details_user_rating_id)
    protected TextView userRatingTextView;
    @BindView(R.id.favourites_button_id)
    protected ToggleButton favoritesButton;
    @BindView(R.id.trailer_data_error_text_view)
    protected TextView trailerDataErrorMessage;
    @BindView(R.id.review_data_error_text_view)
    protected TextView reviewsDataErrorMessage;
    @BindView(R.id.trailer_recycler_view_id)
    protected RecyclerView trailerDataRecyclerView;
    @BindView(R.id.review_recycler_view_id)
    protected RecyclerView reviewsDataRecyclerView;
    @BindView(R.id.scroll_view_id)
    ScrollView scrollView;

    private TextView textView;

    private SQLiteDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        ButterKnife.bind(this);
        //for setting the custom view in action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_custom);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textView = getSupportActionBar().getCustomView().findViewById(R.id.action_bar_textview_id);
        final Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.action_bar_title_key))) {
            textView.setText(intent.getStringExtra(getString(R.string.action_bar_title_key)));
        }
        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            movie = (Movie) intent.getSerializableExtra(Intent.EXTRA_TEXT);
            startTrailerAndReviewersAsyncTasks();
            loadDataToUIElements(movie);
            final FavoriteMoviesDbHelper favoriteMoviesDbHelper = new FavoriteMoviesDbHelper(MovieDetailsActivity.this);
            mDb = favoriteMoviesDbHelper.getWritableDatabase();
            final Cursor cursor = mDb.query(FavoriteMovieEntry.TABLE_NAME, null, FavoriteMovieEntry.COLUMN_MOVIE_ID + "=?", new String[]{String.valueOf(movie.getMovieId())}, null, null, null);
            if (cursor.moveToFirst())
                favoritesButton.setChecked(true);
        }
        favoritesButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    final ContentValues cv = new ContentValues();
                    cv.put(FavoriteMovieEntry.COLUMN_MOVIE_ID, movie.getMovieId());
                    cv.put(FavoriteMovieEntry.COLUMN_MOVIE_TITLE, movie.getMovieTitle());
                    cv.put(FavoriteMovieEntry.COLUMN_POSTER_URL, movie.getMoviePosterUrl());
                    cv.put(FavoriteMovieEntry.COLUMN_OVERVIEW, movie.getMovieOverView());
                    cv.put(FavoriteMovieEntry.COLUMN_RELEASE_DATE, movie.getMovieReleaseDate());
                    cv.put(FavoriteMovieEntry.COLUMN_USER_RATING, movie.getMovieUserRating());
                    Uri uri = getContentResolver().insert(FavoriteMovieEntry.CONTENT_URI, cv);
                    if (uri != null)
                        Toast.makeText(MovieDetailsActivity.this, R.string.added_to_favorites_message, Toast.LENGTH_LONG).show();
                } else {
                    final String id = String.valueOf(movie.getMovieId());
                    final Uri uri = FavoriteMovieEntry.CONTENT_URI.buildUpon().appendPath(id).build();
                    final int result = getContentResolver().delete(uri, null, null);
                    if (result > 0)
                        Toast.makeText(MovieDetailsActivity.this, R.string.removed_from_favorites_message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void startTrailerAndReviewersAsyncTasks() {
        new FetchTrailerData().execute(HelperUtil.MOVIE_DB_URL + movie.getMovieId() + HelperUtil.TRAILER_URL_EXTENSION + HelperUtil.getApiKey(this));
        new FetchReviewDetails().execute(HelperUtil.MOVIE_DB_URL + movie.getMovieId() + HelperUtil.REVIEW_URL_EXTENSION + HelperUtil.getApiKey(this));
    }

    private void loadDataToUIElements(final Movie movie) {
        titleTextView.setText(movie.getMovieTitle());
        Picasso.with(getApplicationContext()).load(HelperUtil.PICASO_LOAD_URL + movie.getMoviePosterUrl()).into(thumbnailImageView);
        overviewTextView.setText(movie.getMovieOverView());
        releaseDateTextView.setText(movie.getMovieReleaseDate());
        userRatingTextView.setText(movie.getMovieUserRating() + "");
        scrollView.smoothScrollTo(0, 0);
    }

    /*
    * Async task to get the Trailer details
    * */
    class FetchTrailerData extends AsyncTask<String, Void, ArrayList<Trailer>> {
        private ArrayList<Trailer> trailers;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ArrayList<Trailer> doInBackground(String... strings) {
            final String url = strings[0];
            final URL trailerUrl = NetworkUtils.buildUrl(url);
            trailers = new ArrayList<>();
            try {
                final String trailerResponse = NetworkUtils.getResponseFromHttpUrl(trailerUrl);
                try {
                    final JSONObject jsonObjectResponse = new JSONObject(trailerResponse);
                    final JSONArray resultsJsonArray = jsonObjectResponse.getJSONArray("results");
                    for (int i = 0; i < resultsJsonArray.length(); i++) {
                        final JSONObject trailerJsonObject = resultsJsonArray.getJSONObject(i);
                        final String videoKey = trailerJsonObject.getString("key");
                        final String videoName = trailerJsonObject.getString("name");
                        final String site = trailerJsonObject.getString("site");
                        final String videoType = trailerJsonObject.getString("type");
                        final Trailer trailer = new Trailer(videoKey, videoName, site, videoType);
                        trailers.add(trailer);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return trailers;
        }

        @Override
        protected void onPostExecute(ArrayList<Trailer> trailers) {
            if (trailers != null) {
                final LinearLayoutManager layoutManager = new LinearLayoutManager(MovieDetailsActivity.this);
                trailerDataRecyclerView.setLayoutManager(layoutManager);
                trailerDataRecyclerView.setHasFixedSize(true);
                if (trailers.size() < 4)
                    trailerDataRecyclerView.getLayoutParams().height = 250;
                else
                    trailerDataRecyclerView.getLayoutParams().height = 500;
                final CustomTrailerAdapter customTrailerAdapter = new CustomTrailerAdapter(trailers, MovieDetailsActivity.this);
                trailerDataRecyclerView.setAdapter(customTrailerAdapter);
            } else {
                trailerDataErrorMessage.setVisibility(View.VISIBLE);
                trailerDataRecyclerView.setVisibility(View.GONE);
            }
        }
    }

    /*
    * Async task to get the Review details
    * */
    class FetchReviewDetails extends AsyncTask<String, Void, ArrayList<Review>> {

        private ArrayList<Review> reviews;

        @Override
        protected ArrayList<Review> doInBackground(String... strings) {
            final String url = strings[0];
            final URL getReviewsUrl = NetworkUtils.buildUrl(url);
            reviews = new ArrayList<>();
            try {
                final String trailerResponse = NetworkUtils.getResponseFromHttpUrl(getReviewsUrl);
                try {
                    final JSONObject jsonObjectResponse = new JSONObject(trailerResponse);
                    final JSONArray resultsJsonArray = jsonObjectResponse.getJSONArray("results");
                    for (int i = 0; i < resultsJsonArray.length(); i++) {
                        JSONObject trailerJsonObject = resultsJsonArray.getJSONObject(i);
                        final String author = trailerJsonObject.getString("author");
                        final String reviewUrl = trailerJsonObject.getString("url");
                        final Review review = new Review(author, reviewUrl);
                        reviews.add(review);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return reviews;
        }

        @Override
        protected void onPostExecute(ArrayList<Review> reviews) {
            if (reviews != null) {
                final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MovieDetailsActivity.this);
                reviewsDataRecyclerView.setLayoutManager(linearLayoutManager);
                final CustomReviewAdapter customReviewAdapter = new CustomReviewAdapter(reviews, MovieDetailsActivity.this);
                reviewsDataRecyclerView.setAdapter(customReviewAdapter);
            } else {
                reviewsDataErrorMessage.setVisibility(View.VISIBLE);
                reviewsDataRecyclerView.setVisibility(View.GONE);
            }
        }
    }
}