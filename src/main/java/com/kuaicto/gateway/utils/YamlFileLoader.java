package com.kuaicto.gateway.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.kuaicto.gateway.cache.GatewayCacheYaml;

public abstract class YamlFileLoader {
    protected static final Logger logger = LoggerFactory.getLogger(YamlFileLoader.class);
    
    public static <T> T loadAs(String name, Class<T> clazz) throws FileNotFoundException {
        
        InputStream in = getFromEnv(name);
        try {
            if (in == null) {
                logger.info("loading {} from classpath", name);
                in = GatewayCacheYaml.class.getClassLoader().getResourceAsStream(name);
            }
            
            T loaded = new Yaml().loadAs(in, clazz);
            logger.info("loaded: {} = {}", name, loaded);
            return loaded;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static InputStream getFromEnv(String name) throws FileNotFoundException {
        String path = System.getProperty(name);
        logger.info("{}: {}", name, path);
        if (StringUtils.isNotBlank(path)) {
            File file = new File(path);
            if (file.exists()) {
                return new FileInputStream(file);
            } else {
                logger.warn("File not exists: {}", path);
            }
        }
        
        return null;
    }
}
