package com.otus.javaqa;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static com.otus.javaqa.Constants.*;

public class WebCrawlerTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;
    private String originalTabHandle;

    @Before
    public void setup(){
        driver = WebDriverFactory.createDriver(WebDriverType.valueOf("CHROME"));
        wait = new WebDriverWait(driver, TIMEOUT);
        actions = new Actions(driver);
        driver.manage().window().maximize();
        originalTabHandle = getCurrentWindowHandle();
        ((JavascriptExecutor)driver).executeScript("window.open()");
        driver.switchTo().window(originalTabHandle);

        driver.get(START_PAGE);
        confirmLocation();
    }

    @After
    public void teardown(){
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void bookScrapingTest(){
        scrapeAllBooks();
    }

    public void confirmLocation(){
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.current-location button")));
        driver.findElement(By.xpath("//div[@class='current-location']/button[text()='Да']")).click();
    }

    public int getLastPageNumber(){
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.pagination-left li:last-child")));
        String lastPage = driver.findElement(By.cssSelector("ul.pagination-left li:last-child")).getAttribute("textContent");
        return Integer.parseInt(lastPage);
    }

    public String getCurrentWindowHandle(){
        return driver.getWindowHandle();
    }

    public void switchToSecondaryTab(){
        for(String handle : driver.getWindowHandles()){
            if(!handle.equals(originalTabHandle)){
                driver.switchTo().window(handle);
                return;
            }
        }
    }

    public void scrapeAllBooks(){
        int totalNumberOfPages = getLastPageNumber();
        LOGGER.info("Total number of pages: " + totalNumberOfPages);

        try (PrintWriter csvWriter = getPrintWriterInstance()){
            csvWriter.append("\"Title\";\"Author\";\"Description\";\"Publisher\";\"Published, year\";\"Language\";\"Number of Pages\";\"Price, rub\";\"URL\"\n");

            for(int i = 1; i < totalNumberOfPages; i++){
                List<WebElement> booksOnCurrentPage = driver.findElements(By.cssSelector("div.catalog-list-cards div.catalog-list-card"));
                LOGGER.info("Number of books on page {}: {}", i, booksOnCurrentPage.size());

                for (WebElement book : booksOnCurrentPage) {
                    String bookLink = book.findElement(By.cssSelector("div.catalog-list-card-image a")).getAttribute("href");
                    LOGGER.debug("Current book URL: " + bookLink);
                    switchToSecondaryTab();
                    driver.get(bookLink);

                    String author, bookTitle, description, publisher, published, language, numberOfPages, price;

                    author = getWebElementTextContent(By.cssSelector("div.catalog-detail-header-author a"));
                    bookTitle = getWebElementTextContent(By.cssSelector("div.catalog-detail-header-title h1"));
                    description = getWebElementTextContent(By.cssSelector("div.catalog-detail-tabs-content-item"));
                    publisher = getWebElementTextContent(By.xpath("//div[@class='catalog-detail-item']/span[text()='Издательство']/following-sibling::a"));
                    published = getWebElementTextContent(By.xpath("//div[@class='catalog-detail-item']/span[text()='Год издания']/following-sibling::span"));
                    language = getWebElementTextContent(By.xpath("//div[@class='catalog-detail-item']/span[text()='Язык']/following-sibling::span"));
                    numberOfPages = getWebElementTextContent(By.xpath("//div[@class='catalog-detail-item']/span[text()='Количество страниц']/following-sibling::span"));
                    price = getWebElementTextContent(By.cssSelector("div.catalog-detail-buy-now"));

                    String bookInfo = String.format("\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\"\n", bookTitle, author, description, publisher, published, language, numberOfPages, price, bookLink);
                    csvWriter.append(bookInfo);

                    driver.switchTo().window(originalTabHandle);
                }
                driver.get(START_PAGE + "?page=" + (i+1));
            }
        }
    }

    public PrintWriter getPrintWriterInstance() {
        OutputStream os = null;
        // код ниже необходим для корректного отображения кирилических символов в .csv файле
        try {
            os = new FileOutputStream(FILE_NAME);
            os.write(239);
            os.write(187);
            os.write(191);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
    }

    public String getWebElementTextContent(final By locator){
        String textContent;
        try {
            WebElement we = driver.findElement((locator));
            textContent = we.getAttribute("textContent").trim();
        } catch (NoSuchElementException e){
            textContent = "Not Available";
            LOGGER.debug("Element not found : " + locator.toString());
        }
        return textContent;
    }
}