package com.urlshorteningservice.minimizurl.service;

import org.springframework.stereotype.Service;

@Service
public class ShorteningService {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public String encode(long id){
        StringBuilder shortUrl = new StringBuilder();
        if(id == 0){
            return String.valueOf(BASE62.charAt(0));
        }
        while(id > 0){
            int rem = (int)(id % 62);
            shortUrl.insert(0, BASE62.charAt(rem));
            id /= 62;
        }
        return shortUrl.toString();
    }

    public long decode(String shortUrl){
        long id = 0;
        for(int i = 0; i < shortUrl.length(); i++){
            id = id * 62 + BASE62.indexOf(shortUrl.charAt(i));
        }
        return id;
    }
}
