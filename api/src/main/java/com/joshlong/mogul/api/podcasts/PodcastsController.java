package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.ApiProperties;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.Podcast;
import com.joshlong.mogul.api.PodcastDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

@Controller
class PodcastsController {

	private final MogulService mogulService;

	private final File podcastDraftsDirectory;

	private final Logger log = LoggerFactory.getLogger(getClass());

	PodcastsController(File podcastDraftsDirectory, MogulService mogulService) {
		this.mogulService = mogulService;
		this.podcastDraftsDirectory = podcastDraftsDirectory;
		Assert.notNull(this.mogulService, "the mogulService must be provided");
		Assert.notNull(this.podcastDraftsDirectory, "the podcastsDraftDirectory must be provided");
		Assert.state(this.podcastDraftsDirectory.exists() || this.podcastDraftsDirectory.mkdirs(),
				"couldn't create the drafts directory [" + this.podcastDraftsDirectory.getAbsolutePath() + "]");

	}

	@MutationMapping
	PodcastDraft createPodcastDraft(@Argument String uid) {
		var pd = this.mogulService.createPodcastDraft(this.mogulService.getCurrentMogul().id(), uid);
		log.debug(pd.toString());
		return pd;
	}

	@QueryMapping
	Collection<Podcast> podcasts() {
		return this.mogulService.getPodcastsByMogul(this.mogulService.getCurrentMogul().id());
	}

	private File handle(File root, String type, MultipartFile file) throws Exception {
		var contentType = file.getContentType();
		var ogFileName = file.getOriginalFilename();
		var fileName = file.getName();
		var resource = file.getResource();
		var child = type + "." + Objects.requireNonNull(ogFileName).substring(ogFileName.lastIndexOf('.') + 1);
		var output = new File(root, child);
		Map.of("fileName", fileName, "contentType", contentType, "originalFileName", ogFileName)
			.forEach((k, v) -> log.info(k + '=' + v));
		try (var in = resource.getInputStream(); var out = new FileOutputStream(output)) {
			FileCopyUtils.copy(in, out);
		}
		return output;
	}

	@ResponseBody
	@PostMapping("/podcasts/drafts/{uid}")
	PodcastDraft uploadDataToPodcastDraft(@PathVariable("uid") String uid, @RequestParam("title") String title,
			@RequestParam("description") String description, @RequestParam("intro") MultipartFile intro,
			@RequestParam("interview") MultipartFile interview, @RequestParam("picture") MultipartFile picture)
			throws Exception {

		var draft = this.mogulService.getPodcastDraftByUid(uid);
		Assert.notNull(draft, "the PodcastDraft object must be non-null");
		var mount = new File(this.podcastDraftsDirectory, uid);
		log.info("uploading [" + uid + "] to [" + mount.getAbsolutePath() + "]");
		Assert.state(mount.exists() || mount.mkdirs(),
				"the directory [" + mount.getAbsolutePath() + "] does not exist");
		var pictureFN = handle(mount, "picture", picture);
		var interviewFN = handle(mount, "interview", interview);
		var introFN = handle(mount, "intro", intro);
		return this.mogulService.completePodcastDraft(this.mogulService.getCurrentMogul().id(), uid, title, description,
				pictureFN, introFN, interviewFN);
	}

	@SchemaMapping(typeName = "PodcastDraft")
	String uploadPath(PodcastDraft podcastDraft) {
		return "/podcasts/drafts/" + podcastDraft.uid();
	}

}

@Configuration
class PodcastsControllerConfiguration {

	@Bean
	PodcastsController podcastsController(MogulService mogulService, ApiProperties properties) {
		return new PodcastsController(properties.podcasts().pipeline().drafts(), mogulService);
	}

}
