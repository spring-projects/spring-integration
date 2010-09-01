package org.springframework.integration.samples.multipart;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
/**
 * @author Oleg Zhurakousky
 *
 */
public class MultipartClient {

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception{

		RestTemplate template = new RestTemplate();
		String uri = "http://localhost:8080/multipart-http/inboundAdapter.htm";
		Resource s2logo = new ClassPathResource("org/springframework/integration/samples/multipart/spring09_logo.png");
		MultiValueMap map = new LinkedMultiValueMap();
		map.add("company", "SpringSource");
		map.add("company-logo", s2logo);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("multipart", "form-data"));
		HttpEntity request = new HttpEntity(map, headers);
		@SuppressWarnings("unused")
		ResponseEntity<?> httpResponse = template.exchange(uri, HttpMethod.POST, request, null);
	}
}
