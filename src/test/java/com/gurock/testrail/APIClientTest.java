package com.gurock.testrail;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.data.MapEntry;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

class APIClientTest {

    static WireMockServer wireMockServer;

    @BeforeAll
    static void startWiremock() {
	wireMockServer = new WireMockServer();
	wireMockServer.start();
    }

    @AfterAll
    static void stopWiremock() {
	wireMockServer.stop();
    }

    @Test
    void sendGetContainsAuthorization() throws Exception {
	WireMock.stubFor(get("/index.php?/api/v2/test") //
		.withHeader("Content-Type", containing("json")) //
		.withBasicAuth("user", "pwd") //
		.willReturn(okJson("{ \"test\": \"42\"}")));//

	APIClient apiClient = new APIClient("http://localhost:8080");
	apiClient.setUser("user");
	apiClient.setPassword("pwd");

	apiClient.sendGet("test");

	verify(getRequestedFor(urlEqualTo("/index.php?/api/v2/test")));
    }

    @Test
    void sendGetReturnsNoData() throws Exception {
	WireMock.stubFor(get("/index.php?/api/v2/test") //
		.withHeader("Content-Type", containing("json")) //
		.willReturn(ok()));

	APIClient apiClient = new APIClient("http://localhost:8080");
	JSONObject jsonResult = (JSONObject) apiClient.sendGet("test");

	assertThat(jsonResult).isEmpty();
    }

    @Test
    void sendGetReturnsValidJsonValue() throws Exception {
	WireMock.stubFor(get("/index.php?/api/v2/test") //
		.withHeader("Content-Type", containing("json")) //
		.willReturn(okJson("{ \"test\": \"42\"}")));

	APIClient apiClient = new APIClient("http://localhost:8080");
	JSONObject jsonResult = (JSONObject) apiClient.sendGet("test");

	assertThat(jsonResult).contains(MapEntry.entry("test", "42"));
    }

    @Test
    void sendGetReturnsAttachment(@TempDir Path tempDir) throws Exception {
	WireMock.stubFor(get("/index.php?/api/v2/get_attachment/numbers") //
		.withHeader("Content-Type", containing("json")) //
		.willReturn(ok().withBody("12345")));

	APIClient apiClient = new APIClient("http://localhost:8080");
	Path tempFileName = Paths.get(tempDir.toString() + "data.txt");

	String jsonResult = (String) apiClient.sendGet("get_attachment/numbers", tempFileName.toString());

	assertThat(jsonResult).isEqualTo(tempFileName.toString());
	assertThat(Files.readAllLines(tempFileName).get(0)).isEqualTo("12345");
    }

    @Test
    void sendGetReturnsErrorCode() throws Exception {
	WireMock.stubFor(get("/index.php?/api/v2/test") //
		.withHeader("Content-Type", containing("json")) //
		.willReturn(WireMock.forbidden()));

	APIClient apiClient = new APIClient("http://localhost:8080");

	assertThatExceptionOfType(APIException.class).isThrownBy(() -> apiClient.sendGet("test"));
    }

    @Test
    void sendGetReturnsErrorMessage() throws Exception {
	WireMock.stubFor(get("/index.php?/api/v2/test") //
		.withHeader("Content-Type", containing("json")) //
		.willReturn(WireMock.forbidden().withHeader("Content-Type", "application/json")
			.withBody("{ \"error\": \"any http error\"}")));

	APIClient apiClient = new APIClient("http://localhost:8080");

	assertThatExceptionOfType(APIException.class).isThrownBy(() -> apiClient.sendGet("test"))
		.withMessageContaining("any http error");
    }

    @Test
    void sendPostWithData() throws Exception {
	WireMock.stubFor(post("/index.php?/api/v2/post") //
		.willReturn(WireMock.ok().withHeader("Content-Type", "application/json")));

	APIClient apiClient = new APIClient("http://localhost:8080");

	apiClient.sendPost("post", "{ \"data\": \"something going on\"}");

	verify(postRequestedFor(urlEqualTo("/index.php?/api/v2/post"))
		.withRequestBody(matching(".*something going on.*")).withHeader("Content-Type", containing("json")));
    }

    @Test
    void sendPostWithAttachment(@TempDir Path tempDir) throws Exception {
	WireMock.stubFor(post("/index.php?/api/v2/add_attachment/data") //
		.willReturn(WireMock.ok().withHeader("Content-Type", "application/json")));

	Path tempFileName = Paths.get(tempDir.toString() + "data.txt");
	Path path = Paths.get(tempFileName.toString());
	Files.write(path, "something going on".getBytes());

	APIClient apiClient = new APIClient("http://localhost:8080");

	apiClient.sendPost("add_attachment/data", tempFileName.toString());

	verify(postRequestedFor(urlEqualTo("/index.php?/api/v2/add_attachment/data")).withHeader("Content-Type",
		equalTo("multipart/form-data; boundary=TestRailAPIAttachmentBoundary")));
    }

}
