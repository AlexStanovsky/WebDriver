import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class DriverWorkerBase implements Runnable {

	private static final int REDIRECT_TIMEOUT_SECONDS = 20;
	protected Queue<String> _sites;
	protected Queue<WebDriver> _drivers;
	protected Writer _writer;
	protected EnvironmentPrefix _environment;

	public DriverWorkerBase(Queue<String> sites, Queue<WebDriver> drivers,
			EnvironmentPrefix environment, Writer writer) {
		_sites = sites;
		_drivers = drivers;
		_writer = writer;
		_environment = environment;
	}

	@Override
	public void run() {

		String site = null;
		synchronized (_sites) {
			if (!_sites.isEmpty()) {
				System.out.println("Sites left: " + _sites.size());
				site = _sites.poll();
			}
		}

		if (site != null) {
			WebDriver driver = _drivers.poll();

			try {
				executeTest(driver, site);

			} catch (IOException e) {
				e.printStackTrace();
			}

			_drivers.offer(driver);
		}
	}

	public abstract void executeTest(WebDriver driver, String site) throws IOException ;
	
	protected String redirectToLink(WebDriver driver, String redirectLink) {
		String currentUrl = "Error in get Url";

		try {
			// Redirect to link
			driver.get(redirectLink);

			WebDriverWait wait = new WebDriverWait(driver, REDIRECT_TIMEOUT_SECONDS);
			wait.until(ExpectedConditions.not(ExpectedConditions
					.titleIs("")));

		} catch (Throwable err) {
			System.out.println("Error in navigation to " + redirectLink);
		}

		try {
			currentUrl = driver.getCurrentUrl();
		} catch (Throwable tr) {
			System.out.println("Couldn't get current url for redirect :"
					+ redirectLink);
		}
		return currentUrl;
	}
	
	protected Collection<String> gatherLinksForSite(WebDriver driver, String site) {
		Collection<String> links = new HashSet<String>();
		try {
			driver.get(site);

			// Get all the links in the page
			List<WebElement> webElemetsLinks = driver.findElements(By
					.partialLinkText(""));

			// Filter visit php links
			for (WebElement link : webElemetsLinks) {
				if (link.getAttribute("href").contains("visit.php")) {
					links.add(link.getAttribute("href"));
				}
			}

		} catch (Throwable er) {
			System.out.println("Error loading in " + site);
		}

		if (links.size() > 0) {
			System.out.println("Loaded successfully in " + site
					+ " with links " + links.size());
		}

		return links;

	}

	public String buildLink(String site, EnvironmentPrefix environment) {
		// Check if mobile
		if (site.startsWith("m.")){
			return "http://" + site.replace("m.", "m." +environment.toString() + ".");
		} else{
			return "http://" + site.replace("www", environment.toString());
		}
	}
}
