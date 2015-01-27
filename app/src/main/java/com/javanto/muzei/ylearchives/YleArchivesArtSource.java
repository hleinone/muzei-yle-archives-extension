package com.javanto.muzei.ylearchives;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import io.fabric.sdk.android.Fabric;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class YleArchivesArtSource extends RemoteMuzeiArtSource {
    private static final String TAG = YleArchivesArtSource.class.getCanonicalName();

    private static final String SOURCE_NAME = YleArchivesArtSource.class.getName();
    private static final String FLICKR_USERNAME = "Archives of the Finnish Broadcasting Company Yle";
    private static final RestAdapter.LogLevel RETROFIT_LOG_LEVEL = BuildConfig.DEBUG ?
            RestAdapter.LogLevel.FULL : RestAdapter.LogLevel.NONE;

    private YleArchivesService yleArchivesService;
    private Random random;

    public YleArchivesArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
        yleArchivesService = new RestAdapter.Builder()
                .setLogLevel(RETROFIT_LOG_LEVEL)
                .setEndpoint("https://www.flickr.com")
                .setRequestInterceptor((RequestInterceptor.RequestFacade request) ->
                                request.addQueryParam("api_key", BuildConfig.FLICKR_API_KEY)
                )
                .setErrorHandler((RetrofitError retrofitError) -> {
                    Log.d(TAG, "Retrofit error", retrofitError);
                    Response response;
                    int statusCode;
                    if (retrofitError.getKind() == RetrofitError.Kind.NETWORK ||
                            (response = retrofitError.getResponse()) == null ||
                            ((statusCode = response.getStatus()) >= 500 && statusCode < 600)) {
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

    private YleArchivesService.Photo getPhoto(YleArchivesService.User user) throws RetryException {
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