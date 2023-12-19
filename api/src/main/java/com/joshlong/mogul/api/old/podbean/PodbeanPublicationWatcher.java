package com.joshlong.mogul.api.old.podbean;

/**
 * registers watches in the DB and ensures there's an integration flow running somewhere
 * to actually watch for that instance
 **/
// @Component
class PodbeanPublicationWatcher {

	/*
	 * private final Logger log = LoggerFactory.getLogger(getClass());
	 *
	 * private final Map<Long, IntegrationFlowContext.IntegrationFlowRegistration>
	 * watchers = new ConcurrentHashMap<>();
	 *
	 * private final ApplicationEventPublishingMessageHandler
	 * episodeSyncApplicationEventPublishingMessageHandler;
	 *
	 * private final PodbeanClient podbeanClient;
	 *
	 * private final IntegrationFlowContext context;
	 *
	 * private final MogulSecurityContexts mogulSecurityContexts;
	 *
	 * private final MogulService mogulService;
	 *
	 * PodbeanPublicationWatcher( ApplicationEventPublishingMessageHandler
	 * episodeSyncApplicationEventPublishingMessageHandler, PodbeanClient podbeanClient,
	 * IntegrationFlowContext context, MogulSecurityContexts mogulSecurityContexts,
	 * MogulService mogulService) {
	 * this.episodeSyncApplicationEventPublishingMessageHandler =
	 * episodeSyncApplicationEventPublishingMessageHandler; this.podbeanClient =
	 * podbeanClient; this.context = context; this.mogulSecurityContexts =
	 * mogulSecurityContexts; this.mogulService = mogulService; }
	 *
	 * public boolean isWatching(Long podcastId) { return
	 * this.watchers.containsKey(podcastId); }
	 *
	 * public IntegrationFlowContext.IntegrationFlowRegistration launch(Long mogulId, Long
	 * podcastId) {
	 *
	 * var messageSource = new MessageSource<Boolean>() {
	 *
	 * @Override public Message<Boolean> receive() {
	 */
	/*
	 * for (var podbeanPublication :
	 * mogulService.getPodbeanPublicationsByNode(NodeUtils.nodeId())) if
	 * (!podbeanPublication.continueTracking()) stop(podbeanPublication.podcastId());
	 *//*
		 * var shouldContinue =
		 * mogulService.getPodbeanPublicationByPodcast(mogulService.getPodcastById(
		 * podcastId)) .continueTracking(); return MessageBuilder//
		 * .withPayload(shouldContinue)// .setHeader(PodcastIntegrations.PODCAST_HEADER,
		 * podcastId) // .build(); } }; var integrationFlow = IntegrationFlow //
		 * .from(messageSource, pm -> pm.poller(p ->
		 * PollerFactory.fixedRate(Duration.ofMinutes(1), Duration.ofSeconds(0))))//
		 * .transformWith(spec -> spec.requiresReply(false).transformer(source -> { var
		 * authentication = mogulSecurityContexts.install(mogulId);
		 * Assert.notNull(authentication,
		 * "the authentication for the current mogul is null!"); var episodes =
		 * podbeanClient.getAllEpisodes(); var podcast =
		 * mogulService.getPodcastById(podcastId); for (var e : episodes) { var matches =
		 * e.getId().equalsIgnoreCase(podcast.podbean().id()); var published =
		 * StringUtils.hasText(e.getStatus()) &&
		 * e.getStatus().equalsIgnoreCase(EpisodeStatus.PUBLISH.name()); if (matches &&
		 * published) { return new PodbeanEpisodeCompletedEvent(podcast, e); } } return
		 * null; })) // .handle(episodeSyncApplicationEventPublishingMessageHandler)//
		 * .get();
		 *
		 * var registration = this.context.registration(integrationFlow).register();
		 * this.watchers.put(podcastId, registration); return registration; }
		 *
		 * @EventListener void complete(PodbeanEpisodeCompletedEvent evt) { var podcastId
		 * = evt.podcast().id(); stop(podcastId); }
		 *
		 * private void stop(Long podcastId) { log.info("stopping " + getClass().getName()
		 * + " for outstanding podcast publication for podcast [" + podcastId + "]"); if
		 * (watchers.containsKey(podcastId)) { var flow = watchers.remove(podcastId); if
		 * (null != flow) { flow.stop(); } } }
		 */

}
