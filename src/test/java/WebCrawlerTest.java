import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WebCrawlerTest {
    private static final Logger logger = LogManager.getLogger(WebCrawlerTest.class);
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static Actions actions;
    private static final long TIMEOUT = 10;
    private static final String START_PAGE = "https://www.podpisnie.ru/";
    private static final String FILE_NAME = "books.csv";
    private static String originalTabHandle;

    @BeforeClass
    public static void generalSetup(){
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

    @AfterClass
    public static void teardown(){
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void bookScrapingTest(){
        openForeignLanguageBooksSection();
        scrapeAllBooks();
    }

    public static void confirmLocation(){
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.current-location button")));
        driver.findElement(By.xpath("//div[@class='current-location']/button[text()='Да']")).click();
    }

    public static void openForeignLanguageBooksSection(){
        WebElement booksMainSection = driver.findElement(By.xpath("//div[@class='nav-main-left']//a[text()='Книги']"));
        actions.moveToElement(booksMainSection).perform();
        driver.findElement(By.xpath("//div[@class='nav-main-left']//a[text()='Книги на иностранных языках']")).click();
        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("div.catalog-list-cards div.catalog-list-card")));
    }

    public static int getCurrentPageNumber(){
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.pagination li.is-active span")));
        String currentPage = driver.findElement(By.cssSelector("div.pagination li.is-active span")).getAttribute("textContent");
        return Integer.parseInt(currentPage);
    }

    public static int getLastPageNumber(){
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.pagination-left li:last-child")));
        String lastPage = driver.findElement(By.cssSelector("ul.pagination-left li:last-child")).getAttribute("textContent");
        return Integer.parseInt(lastPage);
    }

    public static void openNextPage(){
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("ul.pagination-right a")));
        driver.findElement(By.cssSelector("ul.pagination-right li:last-child a")).click();
        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("div.catalog-list-cards div.catalog-list-card")));
    }

    public static String getCurrentWindowHandle(){
        return driver.getWindowHandle();
    }

    public static void switchToSecondaryTab(){
        for(String handle : driver.getWindowHandles()){
            if(!handle.equals(originalTabHandle)){
                driver.switchTo().window(handle);
                return;
            }
        }
    }

    public static void scrapeAllBooks(){
        int totalNumberOfPages = getLastPageNumber();
        logger.info("Total number of pages: " + totalNumberOfPages);

        try (PrintWriter csvWriter = getPrintWriterInstance()){
            csvWriter.append("\"Title\";\"Author\";\"Description\";\"Publisher\";\"Published, year\";\"Language\";\"Number of Pages\";\"Price, rub\";\"URL\"\n");

            while (getCurrentPageNumber() <= totalNumberOfPages) {
                List<WebElement> booksOnCurrentPage = driver.findElements(By.cssSelector("div.catalog-list-cards div.catalog-list-card"));
                logger.info(String.format("Number of books on page %d: %d", getCurrentPageNumber(), booksOnCurrentPage.size()));

                for (WebElement book : booksOnCurrentPage) {
                    String bookLink = book.findElement(By.cssSelector("div.catalog-list-card-image a")).getAttribute("href");
                    logger.debug("Current book URL: " + bookLink);
                    switchToSecondaryTab();
                    driver.get(bookLink);
                    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("div.catalog-detail-item")));

                    String author, bookTitle, description, publisher, published, language, numberOfPages, price;

                    author = getWebElementTextContent(By.cssSelector("div.catalog-detail-header-author a"));
                    bookTitle = getWebElementTextContent(By.cssSelector("div.catalog-detail-header-title h1"));
                    description = getWebElementTextContent(By.cssSelector("div.catalog-detail-tabs-content-item"));
                    publisher = getWebElementTextContent(By.xpath("//div[@class='catalog-detail-item']/span[text()='Издательство']/following-sibling::span"));
                    published = getWebElementTextContent(By.xpath("//div[@class='catalog-detail-item']/span[text()='Год издания']/following-sibling::span"));
                    language = getWebElementTextContent(By.xpath("//div[@class='catalog-detail-item']/span[text()='Язык']/following-sibling::span"));
                    numberOfPages = getWebElementTextContent(By.xpath("//div[@class='catalog-detail-item']/span[text()='Количество страниц']/following-sibling::span"));
                    price = getWebElementTextContent(By.cssSelector("div.catalog-detail-buy-now"));

                    String bookInfo = String.format("\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\"\n", bookTitle, author, description, publisher, published, language, numberOfPages, price, bookLink);
                    csvWriter.append(bookInfo);

                    driver.switchTo().window(originalTabHandle);
                }
                openNextPage();
            }
        }
    }

    public static PrintWriter getPrintWriterInstance() {
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

    public static String getWebElementTextContent(final By locator){
        String textContent;
        try {
            WebElement we = driver.findElement((locator));
            textContent = we.getAttribute("textContent").trim();
        } catch (NoSuchElementException e){
            textContent = "Not Available";
            logger.debug("Element not found with locator: " + locator.toString());
        }
        return textContent;
    }
}