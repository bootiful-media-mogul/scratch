package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.Podcast;
import com.joshlong.mogul.api.PodcastDraft;
import com.joshlong.mogul.api.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.serializer.Serializer;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Objects;

@Controller
class PodcastsController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final MogulService mogulService;

	private final Serializer<PodcastArchive> podcastArchiveSerializer;

	private final File podcastDraftsDirectory;

	private final File podcastArchiveDirectory;

	PodcastsController(File podcastDraftsDirectory, File podcastArchiveDirectory, MogulService mogulService,
			Serializer<PodcastArchive> podcastArchiveSerializer) {
		this.mogulService = mogulService;
		this.podcastDraftsDirectory = podcastDraftsDirectory;
		this.podcastArchiveDirectory = podcastArchiveDirectory;
		this.podcastArchiveSerializer = podcastArchiveSerializer;
		Assert.notNull(this.mogulService, "the mogulService is null");
		Assert.notNull(this.podcastDraftsDirectory, "the podcastsDraftDirectory is null");
		Assert.notNull(this.podcastArchiveDirectory, "the podcastArchiveDirectory is null");
		Assert.notNull(this.podcastArchiveSerializer, "the podcastArchiveSerializer is null");
		FileUtils.ensureDirectoryExists(this.podcastDraftsDirectory);
	}

	/**
	 * the pipeline starts <EM>here</EM>!
	 */
	@ResponseBody
	@PostMapping("/podcasts/drafts/{uid}")
	PodcastDraft startPodcastPipeline(@PathVariable("uid") String uid, @RequestParam("title") String title,
			@RequestParam("description") String description, @RequestParam("intro") MultipartFile intro,
			@RequestParam("interview") MultipartFile interview, @RequestParam("picture") MultipartFile picture,
			@AuthenticationPrincipal Jwt jwt) throws Exception {

		// todo write out the JWT
		log.debug("got the following JWT for the user [" + jwt.getTokenValue() + "]");

		var draft = this.mogulService.getPodcastDraftByUid(uid);
		Assert.notNull(draft, "the PodcastDraft object must be non-null");
		var mount = new File(this.podcastDraftsDirectory, uid);
		log.info("moving [" + uid + "] to [" + mount.getAbsolutePath() + "]");
		Assert.state(mount.exists() || mount.mkdirs(),
				"the directory [" + mount.getAbsolutePath() + "] does not exist");
		var pictureFN = this.handle(mount, "picture", picture);
		var interviewFN = this.handle(mount, "interview", interview);
		var introFN = this.handle(mount, "intro", intro);
		Assert.state(pictureFN.exists() && interviewFN.exists() && introFN.exists(),
				"the files were not uploaded correctly");
		var podcastArchive = new PodcastArchive(uid, title, description, introFN, interviewFN, pictureFN);
		var zipTmp = new File(mount, uid + ".tmp");
		var zip = new File(this.podcastArchiveDirectory, uid + ".zip");
		try (var output = new BufferedOutputStream(new FileOutputStream(zipTmp))) {
			this.podcastArchiveSerializer.serialize(podcastArchive, output);
		}
		log.info("wrote the archive to [" + zipTmp.getAbsolutePath() + "]");
		Assert.state(zipTmp.exists(),
				"the temporary zip archive file [" + zipTmp.getAbsolutePath() + "] does not exist");
		Files.move(zipTmp.toPath(), zip.toPath());
		log.info("moved the archive from [" + zipTmp.getAbsolutePath() + "] to [" + zip.getAbsolutePath() + "]");
		Assert.state(!zipTmp.exists(),
				"the temporary zip archive file [" + zipTmp.getAbsolutePath() + "] still exists. Why?");
		Assert.state(zip.exists(), "the final .zip archive file [" + zip.getAbsolutePath() + "] does not exist");
		return this.mogulService.completePodcastDraft(this.mogulService.getCurrentMogul().id(), uid, title, description,
				pictureFN, introFN, interviewFN);
	}

	@MutationMapping
	PodcastDraft createPodcastDraft(@Argument String uid) {
		return this.mogulService.createPodcastDraft(this.mogulService.getCurrentMogul().id(), uid);
	}

	@QueryMapping
	Collection<Podcast> podcasts() {
		return this.mogulService.getPodcastsByMogul(this.mogulService.getCurrentMogul().id());
	}

	private Resource handle(File root, String type, MultipartFile file) throws Exception {
		var resource = file.getResource();
		var ogFileName = resource.getFilename();
		var child = type + "." + Objects.requireNonNull(ogFileName).substring(ogFileName.lastIndexOf('.') + 1);
		var output = new File(root, child);
		try (var in = resource.getInputStream(); var out = new FileOutputStream(output)) {
			FileCopyUtils.copy(in, out);
		}
		return new FileSystemResource(output);
	}

	@SchemaMapping(typeName = "PodcastDraft")
	String uploadPath(PodcastDraft podcastDraft) {
		return "/podcasts/drafts/" + podcastDraft.uid();
	}

}
