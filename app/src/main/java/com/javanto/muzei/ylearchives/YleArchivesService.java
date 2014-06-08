package com.javanto.muzei.ylearchives;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Query;

public interface YleArchivesService {
    @GET("/services/rest/?method=flickr.people.findByUsername&nojsoncallback=1&format=json")
    UserResponse getUser(@Query("username") String username);

    @GET("/services/rest/?method=flickr.people.getPublicPhotos&extras=url_o&nojsoncallback=1&format=json")
    PhotosResponse getPhotos(@Query("user_id") String userId, @Query("page") int page);

    public static class UserResponse {
        User user;

        @Override
        public String toString() {
            return "UserResponse{" +
                    "user='" + user + '\'' +
                    '}';
        }
    }

    public static class User {
        String id;

        @Override
        public String toString() {
            return "User{" +
                    "id='" + id + '\'' +
                    '}';
        }
    }

    public static class PhotosResponse {
        Photos photos;

        @Override
        public String toString() {
            return "PhotosResponse{" +
                    "photos=" + photos +
                    '}';
        }
    }

    public static class Photos {
        int page;
        int pages;
        int perpage;
        int total;
        List<Photo> photo;

        @Override
        public String toString() {
            return "Photos{" +
                    "page=" + page +
                    ", pages=" + pages +
                    ", perpage=" + perpage +
                    ", total='" + total + '\'' +
                    ", photo=" + photo +
                    '}';
        }
    }

    public static class Photo {
        String id;
        String title;
        String url_o;

        @Override
        public String toString() {
            return "Photo{" +
                    "id='" + id + '\'' +
                    ", title='" + title + '\'' +
                    ", url_o='" + url_o + '\'' +
                    '}';
        }
    }
}
