package com.joshlong.mogul.api.old;

//@Controller
class PodcastsController {

	/*
	 *
	 * private final Logger log = LoggerFactory.getLogger(getClass());
	 *
	 * private final MogulService mogulService;
	 *
	 * private final Serializer<PodcastArchive> podcastArchiveSerializer;
	 *
	 * private final File podcastDraftsDirectory;
	 *
	 * private final File podcastArchiveDirectory;
	 *
	 * private final MarkdownService markdownService;
	 *
	 * PodcastsController(File podcastDraftsDirectory, File podcastArchiveDirectory,
	 * MogulService mogulService, Serializer<PodcastArchive> podcastArchiveSerializer,
	 * MarkdownService markdownService) { this.mogulService = mogulService;
	 * this.podcastDraftsDirectory = podcastDraftsDirectory; this.podcastArchiveDirectory
	 * = podcastArchiveDirectory; this.podcastArchiveSerializer =
	 * podcastArchiveSerializer; this.markdownService = markdownService;
	 * Assert.notNull(this.markdownService, "the markDownService is null");
	 * Assert.notNull(this.mogulService, "the mogulService is null");
	 * Assert.notNull(this.podcastDraftsDirectory, "the podcastsDraftDirectory is null");
	 * Assert.notNull(this.podcastArchiveDirectory,
	 * "the podcastArchiveDirectory is null");
	 * Assert.notNull(this.podcastArchiveSerializer,
	 * "the podcastArchiveSerializer is null");
	 * FileUtils.ensureDirectoryExists(this.podcastDraftsDirectory); }
	 *
	 */
	/**
	 * the pipeline starts <EM>here</EM>!
	 *//*
		 *
		 * @ResponseBody
		 *
		 * @PostMapping("/podcasts/drafts/{uid}") PodcastDraft
		 * startPodcastPipeline(@PathVariable("uid") String uid, @RequestParam("title")
		 * String title,
		 *
		 * @RequestParam("description") String description, @RequestParam("intro")
		 * MultipartFile intro,
		 *
		 * @RequestParam("interview") MultipartFile interview, @RequestParam("picture")
		 * MultipartFile picture,
		 *
		 * @AuthenticationPrincipal Jwt jwt) throws Exception {
		 *
		 * // todo write out the JWT log.debug("got the following JWT for the user [" +
		 * jwt.getTokenValue() + "]");
		 *
		 * var draft = this.mogulService.getPodcastDraftByUid(uid); Assert.notNull(draft,
		 * "the PodcastDraft object must be non-null"); var mount = new
		 * File(this.podcastDraftsDirectory, uid); log.info("moving [" + uid + "] to [" +
		 * mount.getAbsolutePath() + "]"); Assert.state(mount.exists() || mount.mkdirs(),
		 * "the directory [" + mount.getAbsolutePath() + "] does not exist"); var
		 * pictureFN = this.handle(mount, "picture", picture); var interviewFN =
		 * this.handle(mount, "interview", interview); var introFN = this.handle(mount,
		 * "intro", intro); Assert.state(pictureFN.exists() && interviewFN.exists() &&
		 * introFN.exists(), "the files were not uploaded correctly"); var podcastArchive
		 * = new PodcastArchive(uid, title, description, introFN, interviewFN, pictureFN);
		 * var zipTmp = new File(mount, uid + ".tmp"); var zip = new
		 * File(this.podcastArchiveDirectory, uid + ".zip"); try (var output = new
		 * BufferedOutputStream(new FileOutputStream(zipTmp))) {
		 * this.podcastArchiveSerializer.serialize(podcastArchive, output); }
		 * log.info("wrote the archive to [" + zipTmp.getAbsolutePath() + "]");
		 * Assert.state(zipTmp.exists(), "the temporary zip archive file [" +
		 * zipTmp.getAbsolutePath() + "] does not exist"); Files.move(zipTmp.toPath(),
		 * zip.toPath()); log.info("moved the archive from [" + zipTmp.getAbsolutePath() +
		 * "] to [" + zip.getAbsolutePath() + "]"); Assert.state(!zipTmp.exists(),
		 * "the temporary zip archive file [" + zipTmp.getAbsolutePath() +
		 * "] still exists. Why?"); Assert.state(zip.exists(),
		 * "the final .zip archive file [" + zip.getAbsolutePath() + "] does not exist");
		 * return this.mogulService.updatePodcastDraft(getCurrentMogulId(), uid, title,
		 * description, pictureFN, introFN, interviewFN, false); }
		 *
		 * @MutationMapping PodcastDraft createPodcastDraft(@Argument String uid) { return
		 * this.mogulService.createPodcastDraft(getCurrentMogulId(), uid); }
		 *
		 * @MutationMapping Boolean deletePodcast(@Argument Long id) {
		 * System.out.println("deleting podcast [" + id + "]");
		 * this.mogulService.schedulePodcastForDeletion(id); return true; }
		 *
		 * @SchemaMapping(typeName = "Podcast") Long created(Podcast podcast) { return
		 * podcast.date().getTime(); }
		 *
		 * @SchemaMapping(typeName = "PodcastDraft") Long created(PodcastDraft
		 * podcastDraft) { return podcastDraft.date().getTime(); }
		 *
		 * @SchemaMapping(typeName = "Podcast") String permalinkUri(Podcast podcast) {
		 * return podcast.podbean().permalink().toString(); }
		 *
		 * @SchemaMapping(typeName = "Podcast") String playerUri(Podcast podcast) { return
		 * podcast.podbean().player().toString(); }
		 *
		 * @SchemaMapping(typeName = "Podcast") String html(Podcast podcast) { return
		 * this.markdownService.convertMarkdownTemplateToHtml(podcast.description()); }
		 *
		 */
	/* convenience method! don't export. *//*
											 *
											 * private Long getCurrentMogulId() { return
											 * this.mogulService.getCurrentMogul().id(); }
											 *
											 * @QueryMapping Collection<PodcastDraft>
											 * podcastDrafts() { return
											 * this.mogulService.getPodcastDraftsByMogul(
											 * getCurrentMogulId()); }
											 *
											 * @QueryMapping Collection<Podcast>
											 * podcasts() { return
											 * this.mogulService.getPodcastsByMogul(
											 * getCurrentMogulId()); }
											 *
											 * private Resource handle(File root, String
											 * type, MultipartFile file) throws Exception
											 * { var resource = file.getResource(); var
											 * ogFileName = resource.getFilename(); var
											 * child = type + "." +
											 * Objects.requireNonNull(ogFileName).
											 * substring(ogFileName.lastIndexOf('.') + 1);
											 * var output = new File(root, child); try
											 * (var in = new
											 * BufferedInputStream(resource.getInputStream
											 * ()); var out = new BufferedOutputStream(new
											 * FileOutputStream(output))) {
											 * FileCopyUtils.copy(in, out); } return new
											 * FileSystemResource(output); }
											 *
											 * @SchemaMapping(typeName = "PodcastDraft")
											 * String uploadPath(PodcastDraft
											 * podcastDraft) { return "/podcasts/drafts/"
											 * + podcastDraft.uid(); }
											 */

}
