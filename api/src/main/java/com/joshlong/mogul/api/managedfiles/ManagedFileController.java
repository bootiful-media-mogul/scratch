package com.joshlong.mogul.api.managedfiles;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.MogulService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
class ManagedFileController {

	private static final String MF_RW_URL = "/managedfiles/{id}";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ManagedFileService managedFileService;

	private final MogulService mogulService;

	ManagedFileController(ManagedFileService managedFileService, MogulService mogulService) {
		this.managedFileService = managedFileService;
		this.mogulService = mogulService;
	}

	@QueryMapping
	ManagedFile managedFileById(@Argument Long id ) {
		return this.managedFileService.getManagedFile( id );
	}

	@GetMapping(MF_RW_URL)
	@ResponseBody
	ResponseEntity<Resource> read(@PathVariable Long id) throws Exception {
		Assert.notNull(id, "the managed file id is null");
		var mf = managedFileService.getManagedFile(id);
		Assert.notNull(mf, "the managed file does not exist [" + id + "]");
		var read = managedFileService.read(id);
		var contentType = mf.contentType();
		return ResponseEntity.ok()
				.contentLength(read.contentLength())
				.contentType(MediaType.parseMediaType(contentType))
				.body(read);

	}

	@ResponseBody
	@PostMapping(MF_RW_URL)
	Map<String, Object> write(@PathVariable Long id, @RequestParam MultipartFile file) {
		Assert.notNull(id, "the id should not be null");
		var mogul = this.mogulService.getCurrentMogul();
		var managedFile = this.managedFileService.getManagedFile(id);
		Assert.notNull(managedFile, "the managed file is null for managed file id [" + id + "]");
		Assert.state(managedFile.mogulId().equals(mogul.id()), "you're trying to write to an invalid file to which you are not authorized!");
		var originalFilename = file.getOriginalFilename();
		var mediaType = CommonMediaTypes.guess(file.getResource());
		log.debug("guessing the media type for [" + file.getOriginalFilename() + "] is  " + mediaType);
		this.managedFileService.write(managedFile.id(), originalFilename, mediaType, file.getResource());
		var updated = managedFileService.getManagedFile( managedFile.id()) ;
		log.debug("finished writing managed file [" + id + "] to s3: " + originalFilename + ":" +
				updated.toString());
		return Map.of("managedFileId", id);
	}

}
