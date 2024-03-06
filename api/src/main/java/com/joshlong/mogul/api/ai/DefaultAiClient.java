package com.joshlong.mogul.api.ai;

import org.springframework.ai.chat.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

class DefaultAiClient implements AiClient {

	private static final String OPENAI_API_IMAGES_URL = "https://api.openai.com/v1/images/generations";

	private final RestTemplate restTemplate;

	private final ChatClient aiClient;

	private final String openaiApiKey;

	private final TranscriptionClient transcriptionClient;

	DefaultAiClient(RestTemplate restTemplate, ChatClient aiClient, String openaiApiKey,
			TranscriptionClient transcriptionClient) {
		this.restTemplate = restTemplate;
		this.aiClient = aiClient;
		this.openaiApiKey = openaiApiKey;
		this.transcriptionClient = transcriptionClient;
	}

	@Override
	public String transcribe(Resource audio) {
		return this.transcriptionClient.transcribe(UUID.randomUUID().toString(), audio);
	}

	@Override
	public String chat(String prompt) {
		return this.aiClient.call(prompt);
	}

	@Override
	public Resource render(String prompt, ImageSize imageSize) {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + openaiApiKey);

		var request = new ImageGenerationRequest("dall-e-3", prompt, 1, imageSize.value());
		var entity = new HttpEntity<>(request, headers);
		var response = restTemplate.postForEntity(OPENAI_API_IMAGES_URL, entity, ImageGenerationResponse.class);
		var igr = response.getBody();
		if (igr != null && igr.data() != null && !igr.data().isEmpty()) {
			var img = igr.data().getFirst();
			var url = img.url();
			return new UrlResource(url);
		}
		throw new IllegalStateException("couldn't generate an image given prompt [" + prompt + "]");
	}

}
