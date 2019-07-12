package com.otus.javaqa;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Constants {
    protected static final Logger LOGGER = LogManager.getLogger(WebCrawlerTest.class);
    protected static final long TIMEOUT = 10;
    protected static final String START_PAGE = "https://www.podpisnie.ru/categories/knigi/knigi-na-inostrannykh-yazykakh/";
    protected static final String FILE_NAME = "books.csv";
}
