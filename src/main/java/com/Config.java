package com;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class Config extends Properties {

    private final static Config instance = new Config();
    private Logger logger = LogManager.getLogger(Main.class);

    private Config() {
        try(FileInputStream stream = new FileInputStream("src/main/resources/config.properties")) {
            load(stream);
        } catch (IOException e) {
            logger.warn(e);
        }
    }

    public static Config getInstance() {
        return instance;
    }

}
