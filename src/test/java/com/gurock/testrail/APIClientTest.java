package com.gurock.testrail;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.github.tomakehurst.wiremock.WireMockServer;

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

}
