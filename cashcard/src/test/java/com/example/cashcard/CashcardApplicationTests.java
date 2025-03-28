
package com.example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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

	@LocalServerPort
	private int port;

	private String getBaseUrl() {
		return "http://localhost:" + port;
	}

	private ResponseEntity<String> makeGetRequest(String url, String username, String password, HttpStatus expectedStatus) {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth(username, password)
				.getForEntity(url, String.class);

		assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
		return response;
	}

	//Generic type <T>
	private <T> ResponseEntity<Void> makePostRequest(String url, T body, String username, String password, HttpStatus expectedStatus) {
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth(username, password)
				.postForEntity(url, body, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
		return response;
	}

	private <T> ResponseEntity<Void> makePutRequest(String url, T body, String username, String password, HttpStatus expectedStatus) {
		HttpEntity<T> request = new HttpEntity<>(body);

		ResponseEntity<Void> response = restTemplate
				.withBasicAuth(username, password)
				.exchange(url, HttpMethod.PUT, request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
		return response;
	}

	private ResponseEntity<Void> makeDeleteRequest(String url, String username, String password, HttpStatus expectedStatus) {

		ResponseEntity<Void> deleteResponse = restTemplate
				.withBasicAuth(username, password)
				.exchange(url, HttpMethod.DELETE, null, Void.class);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(expectedStatus);
		return deleteResponse;
	}


	@Test
	void shouldReturnACashCardWhenDataIsSaved() {
		ResponseEntity<String> response = makeGetRequest("/cashcards/99", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isNotNull();
		assertThat(id).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {
		ResponseEntity<String> response = makeGetRequest("/cashcards/1000", "sarah1", "abc123", HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	@DirtiesContext
	void shouldCreateANewCashCard() {
		CashCard newCashCard = new CashCard(null, 250.00,null);
		ResponseEntity<Void> createResponse = makePostRequest("/cashcards", newCashCard, "sarah1", "abc123", HttpStatus.CREATED);

		URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
        assert locationOfNewCashCard != null;
        ResponseEntity<String> getResponse = makeGetRequest(locationOfNewCashCard.toString(), "sarah1", "abc123", HttpStatus.OK);

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

		makePutRequest(getBaseUrl() + "/cashcards/99", cashCardUpdate, "sarah1", "abc123", HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = makeGetRequest("/cashcards/99", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");
		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(19.99);
	}

	@Test
	void shouldReturnAllCashCardsWhenListIsRequested() {
		ResponseEntity<String> response = makeGetRequest("/cashcards", "sarah1", "abc123", HttpStatus.OK);

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
		ResponseEntity<String> response = makeGetRequest("/cashcards?page=0&size=1", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	@Test
	void shouldReturnASortedPageOfCashCards() {
		ResponseEntity<String> response = makeGetRequest("/cashcards?page=0&size=1&sort=amount,desc", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray read = documentContext.read("$[*]");
		assertThat(read.size()).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
		ResponseEntity<String> response = makeGetRequest("/cashcards", "sarah1", "abc123", HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
	}

	@Test
	void shouldNotReturnACashCardWhenUsingBadCredentials() {
        makeGetRequest("/cashcards/99", "BAD-USER", "abc123", HttpStatus.UNAUTHORIZED);

        makeGetRequest("/cashcards/99", "sarah1", "BAD-PASSWORD", HttpStatus.UNAUTHORIZED);
    }

	@Test
	void shouldRejectUsersWhoAreNotCardOwners() {
        makeGetRequest("/cashcards/99", "hank-owns-no-cards", "qrs456", HttpStatus.FORBIDDEN);
    }

	@Test
	void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
        makeGetRequest("/cashcards/102", "sarah1", "abc123", HttpStatus.NOT_FOUND);
    }

	@Test
	void shouldNotUpdateACashCardThatDoesNotExist() {
		CashCard unknownCard = new CashCard(null, 19.99, null);
		makePutRequest(getBaseUrl()+"/cashcards/99999", unknownCard, "sarah1", "abc123", HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
		CashCard kumarsCard = new CashCard(null, 333.33, null);
		makePutRequest(getBaseUrl()+"/cascards/102", kumarsCard, "sarah1", "abc123", HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
		//We'll add this annotation to all tests which change the data.
		// If we don't, then these tests could affect the result of other tests in the file.
	void shouldDeleteAnExistingCashCard() {
		makeDeleteRequest("cashcards/99", "sarah1", "abc123", HttpStatus.NO_CONTENT);

		makeGetRequest("/cashcards/99", "sarah1", "abc123", HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteACashCardThatDoesNotExist() {
		makeDeleteRequest("cashcards/99999", "sarah1", "abc123", HttpStatus.NOT_FOUND);
	}
	@Test
	void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn() {
		makeDeleteRequest("/cashcards/99", "sarah1", "abc123", HttpStatus.NOT_FOUND);

		makeGetRequest("/cashcards/102", "kumar2", "xyz789", HttpStatus.OK);
	}
}