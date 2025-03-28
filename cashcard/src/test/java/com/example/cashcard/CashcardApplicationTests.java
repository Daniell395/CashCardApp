
package com.example.cashcard;

import com.example.cashcard.utils.TestRestClient;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CashCardApplicationTests {
	@Autowired
	TestRestTemplate restTemplate;

	private TestRestClient testClient;

	@BeforeEach
	void setUp() {
		String baseUrl = "http://localhost:" + port; // manual URL
		testClient = new TestRestClient(restTemplate, baseUrl());
	}

	private String baseUrl() {
		return "http://localhost:" + port;
	}

	@LocalServerPort
	private int port;


	@Test
	void shouldReturnACashCardWhenDataIsSaved() {
		ResponseEntity<String> response = testClient.makeGetRequest("/cashcards/99", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isNotNull();
		assertThat(id).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {
		ResponseEntity<String> response = testClient.makeGetRequest("/cashcards/1000", "sarah1", "abc123", HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	@DirtiesContext
	void shouldCreateANewCashCard() {
		CashCard newCashCard = new CashCard(null, 250.00,null);
		ResponseEntity<Void> createResponse = testClient.makePostRequest("/cashcards", newCashCard, "sarah1", "abc123", HttpStatus.CREATED);

		URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
        assert locationOfNewCashCard != null;
        ResponseEntity<String> getResponse = testClient.makeGetRequest(locationOfNewCashCard.toString(), "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(250.00);
	}

	@Test
	@DirtiesContext
	void shouldUpdateAnExistingCashCard() {
		CashCard cashCardUpdate = new CashCard(null, 19.99, null);

		testClient.makePutRequest("/cashcards/99", cashCardUpdate, "sarah1", "abc123", HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = testClient.makeGetRequest("/cashcards/99", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");
		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(19.99);
	}

	@Test
	void shouldReturnAllCashCardsWhenListIsRequested() {
		ResponseEntity<String> response = testClient.makeGetRequest("/cashcards", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.0, 150.00);
	}

	@Test
	void shouldReturnAPageOfCashCards() {
		ResponseEntity<String> response = testClient.makeGetRequest("/cashcards?page=0&size=1", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	@Test
	void shouldReturnASortedPageOfCashCards() {
		ResponseEntity<String> response = testClient.makeGetRequest("/cashcards?page=0&size=1&sort=amount,desc", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray read = documentContext.read("$[*]");
		assertThat(read.size()).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
		ResponseEntity<String> response = testClient.makeGetRequest("/cashcards", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
	}

	@Test
	void shouldNotReturnACashCardWhenUsingBadCredentials() {
		testClient.makeGetRequest("/cashcards/99", "BAD-USER", "abc123", HttpStatus.UNAUTHORIZED);

		testClient.makeGetRequest("/cashcards/99", "sarah1", "BAD-PASSWORD", HttpStatus.UNAUTHORIZED);
    }

	@Test
	void shouldRejectUsersWhoAreNotCardOwners() {
		testClient.makeGetRequest("/cashcards/99", "hank-owns-no-cards", "qrs456", HttpStatus.FORBIDDEN);
    }

	@Test
	void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
		testClient.makeGetRequest("/cashcards/102", "sarah1", "abc123", HttpStatus.NOT_FOUND);
    }

	@Test
	void shouldNotUpdateACashCardThatDoesNotExist() {
		CashCard unknownCard = new CashCard(null, 19.99, null);
		testClient.makePutRequest("/cashcards/99999", unknownCard, "sarah1", "abc123", HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
		CashCard kumarsCard = new CashCard(null, 333.33, null);
		testClient.makePutRequest("/cascards/102", kumarsCard, "sarah1", "abc123", HttpStatus.FORBIDDEN);
	}

	@Test
	@DirtiesContext
		//We'll add this annotation to all tests which change the data.
		// If we don't, then these tests could affect the result of other tests in the file.
	void shouldDeleteAnExistingCashCard() {

		testClient.makeDeleteRequest("/cashcards/99", "sarah1", "abc123", HttpStatus.NO_CONTENT);

		testClient.makeGetRequest("/cashcards/99", "sarah1", "abc123", HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteACashCardThatDoesNotExist() {
		testClient.makeDeleteRequest("/cashcards/99999", "sarah1", "abc123", HttpStatus.NOT_FOUND);
	}
	@Test
	void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn() {
		testClient.makeDeleteRequest("/cashcards/102", "sarah1", "abc123", HttpStatus.NOT_FOUND);

		testClient.makeGetRequest("/cashcards/102", "kumar2", "xyz789", HttpStatus.OK);
	}
}