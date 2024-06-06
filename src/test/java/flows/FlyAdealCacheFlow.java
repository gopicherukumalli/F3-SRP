package flows;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.gson.Gson;

import FlyModules.BrowserContants;
import FlyModules.flyAdeal;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import pageObjects.BaseClass;
import pageObjects.Database;

public class FlyAdealCacheFlow {
    static WebDriver driver;
    private int iTestCaseRow;
    boolean status;
    private Database PnrDetails;
    public static String flyAdealApiUrl;

	@BeforeMethod
	public void setup() throws InterruptedException {
		WebDriverManager.chromedriver().setup();
		Map<String, Object> prefs = new HashMap<String, Object>();
		prefs.put("profile.default_content_setting_values.notifications", 2);
		ChromeOptions options = new ChromeOptions();
		options.setExperimentalOption("prefs", prefs);
		options.setPageLoadStrategy(PageLoadStrategy.NONE);
		options.addArguments("start-maximized");
		options.setExperimentalOption("useAutomationExtension", false);
		options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-infobars");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--disable-browser-side-navigation");
		options.addArguments("--disable-gpu");
		options.addArguments("--enable-javascript");
		prefs.put("profile.managed_default_content_settings.images", 2);
		options.setExperimentalOption("prefs", prefs);
		options.addArguments("force-device-scale-factor=0.5");
		options.addArguments("--clear-ssl-state");
		options.addArguments("--disable-cache");
		options.addArguments("--disk-cache-size=0");
		options.addArguments("--disable-network-throttling");
		driver = new ChromeDriver(options);
		driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
		// System.out.println(driver.manage().window().getSize());
		driver.manage().deleteAllCookies();

	}
    @Test 
    public void test() throws Exception {
        if (BrowserContants.ENV.equals("PRD")) {
            RestAssured.baseURI = BrowserContants.PRD_API_URL;
            System.out.println(BrowserContants.PRD_API_URL);
        } else if (BrowserContants.ENV.equals("STG")) {
            RestAssured.baseURI = BrowserContants.STG_API_URL;
            System.out.println(BrowserContants.STG_API_URL);
        }

        LocalTime currentTime = LocalTime.now();
        
        int days;
        int skipdays;
        
        LocalTime startTime = LocalTime.of(23, 0); // 11 PM
        LocalTime endTime = LocalTime.of(5, 0); // 5 AM
        
        // Check if the current time falls within the specified range
        if (currentTime.isAfter(startTime) || currentTime.equals(startTime) ||
            currentTime.isBefore(endTime) && currentTime.isAfter(LocalTime.MIDNIGHT)) {
            days = 5;
            skipdays = 4;
        } else {
        	days = 5;
            skipdays = 4;
        }
        RequestSpecification request = RestAssured.given();
        request.header("Content-Type", "application/json");
        Response response = request.get("/GetF3Routes?days="+days+"&group=6&skipdays="+skipdays+"");
        System.out.println("Response body: " + response.body().asString());
        String s = response.body().asString();
        System.out.println(s);
        int statusCode = response.getStatusCode();
        System.out.println("The status code received: " + statusCode);

        Gson gson = new Gson();
        Database[] databaseArray = gson.fromJson(s, Database[].class);
        List<Database> databaseList = Arrays.asList(databaseArray);

        // To track unique routes
        Set<String> uniqueRoutes = new HashSet<>();
        int uniqueRoutesProcessed = 0;

        for (Database data : databaseList) {
            // Create a unique route identifier
            String routeIdentifier = data.From + "->" + data.To;

            // Skip entries with the same "from" and "to" values, or if the route is already processed
            if (!uniqueRoutes.add(routeIdentifier)) {
                continue;
            }

            uniqueRoutesProcessed++;

            // Check if 5 unique routes have been processed, if yes, break out of the loop
            if (uniqueRoutesProcessed > 6) {
                break;
            }

            try {
            	Date depDate=new SimpleDateFormat("dd MMM yyyy").parse(data.DepartureDate);  
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				String strDate= formatter.format(depDate);
				System.out.println("strDate :"+strDate);
                flyAdealApiUrl = "https://www.flyadeal.com/en/booking/select/?origin1=" + data.From + "&destination1=" + data.To + "&departure1=" + strDate + "&adt1=1&chd1=0&inf1=0&currency=SAR";
                System.out.println("API URL: " + flyAdealApiUrl);
                PnrDetails = data;
                
                driver.get(flyAdealApiUrl);
                Thread.sleep(4000);
                new BaseClass(driver);
                flyAdeal.FlightDetails2(driver, PnrDetails);
                //driver.quit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @AfterMethod
    public void stop() throws Exception {
        if (driver != null) {
            driver.quit();
        }
    }
}
