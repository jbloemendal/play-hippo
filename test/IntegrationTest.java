import com.fasterxml.jackson.databind.JsonNode;
import org.junit.*;

import play.libs.Json;
import play.mvc.*;
import play.test.*;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

import static org.fluentlenium.core.filter.FilterConstructor.*;

public class IntegrationTest {

    @Test
    public void testBrowse() {
        running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, browser -> {
            browser.goTo("http://localhost:3333/browse/content");

            assertTrue(browser.pageSource().contains("properties"));
            assertTrue(browser.pageSource().contains("child"));
            assertTrue(browser.pageSource().contains("\"documents\":\"/content/documents\""));
            assertTrue(browser.pageSource().contains("cafebabe-cafe-babe-cafe-babecafebabe"));

            browser.goTo("http://localhost:3333/browse/content/documents");
            assertTrue(browser.pageSource().contains("cafebabe-cafe-babe-cafe-babecafebabe"));
        });
    }


    @Test
    public void testUuid() {
        running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, browser -> {
            browser.goTo("http://localhost:3333/browse/content");

            assertTrue(browser.pageSource().contains("properties"));
            assertTrue(browser.pageSource().contains("child"));
            assertTrue(browser.pageSource().contains("\"documents\":\"/content/documents\""));
            assertTrue(browser.pageSource().contains("cafebabe-cafe-babe-cafe-babecafebabe"));

            browser.goTo("http://localhost:3333/browse/content/documents");
            JsonNode root = Json.parse(browser.pageSource());
            JsonNode properties = root.get("properties");
            JsonNode jcrUuid = properties.get("jcr:uuid");
            String uuid = jcrUuid.asText();

            browser.goTo("http://localhost:3333/uuid/"+uuid);
            assertTrue(browser.pageSource().contains("\"path\":\"/content/documents\""));
        });
    }
}
