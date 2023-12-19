package com.joshlong.mogul.api.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.List;
import java.util.UUID;

class AiClient {

	private static final String OPENAI_API_IMAGES_URL = "https://api.openai.com/v1/images/generations";

	private final RestTemplate restTemplate;

	private final org.springframework.ai.client.AiClient aiClient;

	private final String openaiApiKey;

	private final TranscriptionClient transcriptionClient;

	AiClient(RestTemplate restTemplate, org.springframework.ai.client.AiClient aiClient, String openaiApiKey,
			TranscriptionClient transcriptionClient) {
		this.restTemplate = restTemplate;
		this.aiClient = aiClient;
		this.openaiApiKey = openaiApiKey;
		this.transcriptionClient = transcriptionClient;
	}

	public String transcribe(Resource audio) {
		return this.transcriptionClient.transcribe(UUID.randomUUID().toString(), audio);
	}

	public String chat(String prompt) {
		return this.aiClient.generate(prompt);
	}

	public Resource render(String prompt, ImageSize imageSize) {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + openaiApiKey);

		var request = new ImageGenerationRequest("dall-e-3", prompt, 1, imageSize.value());
		var entity = new HttpEntity<>(request, headers);
		var response = restTemplate.postForEntity(OPENAI_API_IMAGES_URL, entity, ImageGenerationResponse.class);
		var igr = response.getBody();
		if (igr != null && igr.data() != null && !igr.data().isEmpty()) {
			var img = igr.data().iterator().next();
			var url = img.url();
			return new UrlResource(url);
		}
		throw new IllegalStateException("couldn't generate an image given prompt [" + prompt + "]");
	}

}

// 1024x1024, 1024x1792 or 1792x1024
record ImageGenerationRequest(String model, String prompt, int n, String size) {

}

record ImageGenerationResponse(long created, List<Image> data) {
}

record Image(@JsonProperty("revised_prompt") String revisedPrompt, URL url) {
}

enum ImageSize {

	SIZE_1024x1024("1024x1024"), SIZE_1024x1792("1024x1792"), SIZE_1792x1024("1792x1024");

	private final String value;

	ImageSize(String s) {
		this.value = s;
	}

	String value() {
		return this.value;
	}

}