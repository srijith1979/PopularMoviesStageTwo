package com.example.android.popularmovies.data;

import android.provider.BaseColumns;

/**
 * Created by sp051821 on 11/12/17.
 */

public class FavoriteMoviesContract {

    public static class FavoriteMovieEntry implements BaseColumns{
        public static final String TABLE_NAME = "movies";
        public static final String COLUMN_MOVIE_ID = "movie_id";
        public static final String COLUMN_MOVIE_TITLE = "movie_title";
        public static final String COLUMN_POSTER_URL = "movie_poster_url";
        public static final String COLUMN_OVERVIEW = "movie_overview";
        public static final String COLUMN_RELEASE_DATE ="movie_release_date";
        public static final String COLUMN_USER_RATING = "user_rating";
    }

}