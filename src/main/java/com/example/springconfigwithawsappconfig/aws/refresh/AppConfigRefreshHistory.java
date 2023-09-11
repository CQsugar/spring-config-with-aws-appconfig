package com.example.springconfigwithawsappconfig.aws.refresh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class AppConfigRefreshHistory {

    private static final Logger log = LoggerFactory.getLogger(AppConfigRefreshHistory.class);

    private static final int MAX_SIZE = 20;

    private final LinkedList<Record> records = new LinkedList<>();

    private MessageDigest md;

    public AppConfigRefreshHistory() {
        try {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            log.error("failed to initialize MessageDigest : ", e);
        }
    }



    public void addRefreshRecord(String application, String environment,String profile, String data) {
        records.addFirst(new Record(LocalDateTime.now().toString(),
                application, environment, profile, md5(data)));
        if (records.size() > MAX_SIZE) {
            records.removeLast();
        }
    }

    public List<Record> getRecords() {
        return records;
    }

    private String md5(String data) {
        if (!StringUtils.hasLength(data)) {
            return null;
        }
        if (null == md) {
            try {
                md = MessageDigest.getInstance("MD5");
            }
            catch (NoSuchAlgorithmException ignored) {
                return "unable to get md5";
            }
        }
        return new BigInteger(1, md.digest(data.getBytes(StandardCharsets.UTF_8)))
                .toString(16);
    }

    static class Record {

        private final String timestamp;

        private final String application;

        private final String environment;

        private final String profile;

        private final String md5;

        Record(String timestamp, String application, String environment,
               String profile, String md5) {
            this.timestamp = timestamp;
            this.application = application;
            this.environment = environment;
            this.profile = profile;
            this.md5 = md5;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getApplication() {
            return application;
        }

        public String getEnvironment() {
            return environment;
        }

        public String getProfile() {
            return profile;
        }

        public String getMd5() {
            return md5;
        }

    }

}
