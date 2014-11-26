package com.javanto.muzei.ylearchives;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

public class YleArchivesArtSource extends RemoteMuzeiArtSource {
    private static final String TAG = YleArchivesArtSource.class.getCanonicalName();

    private static final String SOURCE_NAME = YleArchivesArtSource.class.getName();
    private static final String FLICKR_USERNAME = "Finnish Broadcasting Company Yle Archives";

    private YleArchivesService yleArchivesService;
    private Random random;

    public YleArchivesArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
        yleArchivesService = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint("https://www.flickr.com")
                .setRequestInterceptor((RequestInterceptor.RequestFacade request) ->
                                request.addQueryParam("api_key", BuildConfig.FLICKR_API_KEY)
                )
                .setErrorHandler((RetrofitError retrofitError) -> {
                    Log.d(TAG, "Retrofit error", retrofitError);
                    int statusCode = retrofitError.getResponse().getStatus();
                    if (retrofitError.getKind() == RetrofitError.Kind.NETWORK
                            || (500 <= statusCode && statusCode < 600)) {
                        return new RetryException();
                    }
                    scheduleUpdate();
                    return retrofitError;
                })
                .build()
                .create(YleArchivesService.class);
        random = new Random();
    }

    @Override
    protected void onTryUpdate(int i) throws RetryException {
        YleArchivesService.User user = yleArchivesService.getUser(FLICKR_USERNAME).user;
        YleArchivesService.Photo photo = getPhoto(user);

        if (photo == null) {
            Log.d(TAG, "Empty photo stream");
            scheduleUpdate();
            return;
        }

        publishArtwork(new Artwork.Builder()
                .title(photo.title)
                .imageUri(Uri.parse(photo.url_o))
                .token(photo.id)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(String.format(Locale.ENGLISH, "https://www.flickr.com/photos/%1$s/%2$s/", user.id, photo.id))))
                .build());
        scheduleUpdate();
    }

    private YleArchivesService.Photo getPhoto(YleArchivesService.User user) {
        Log.d(TAG, "user " + user);
        YleArchivesService.Photos photos = yleArchivesService.getPhotos(user.id, 1).photos;
        int total = photos.total;
        if (total == 0) {
            return null;
        }
        int photoIndex = random.nextInt(total - 1);

        Log.d(TAG, "photos " + photos);
        while (photoIndex >= photos.perpage * (photos.page)) {
            photos = yleArchivesService.getPhotos(user.id, photos.page + 1).photos;
        }

        photoIndex = photoIndex - ((photos.page - 1) * photos.perpage);
        return photos.photo.get(photoIndex);
    }

    private void scheduleUpdate() {
        // Schedule update tomorrow at midnight
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        scheduleUpdate(cal.getTimeInMillis());
    }
}